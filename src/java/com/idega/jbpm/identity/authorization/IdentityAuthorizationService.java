package com.idega.jbpm.identity.authorization;

import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.jbpm.security.AuthenticationService;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.data.IDORuntimeException;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.TaskInstanceAccess;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BpmBindsDAO;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.BPMTaskAccessPermission;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.util.CoreUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2008/03/10 19:32:48 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class IdentityAuthorizationService implements AuthorizationService {

	private static final long serialVersionUID = -7496842155073961922L;
	
	private AuthenticationService authenticationService;
	private BpmBindsDAO bpmBindsDAO;

	public void checkPermission(Permission perm) throws AccessControlException {

		if(!(perm instanceof BPMTaskAccessPermission))
			throw new IllegalArgumentException("Only permissions implementing "+BPMTaskAccessPermission.class.getName()+" supported");
		
		BPMTaskAccessPermission permission = (BPMTaskAccessPermission)perm;
		
		String loggedInActorId = getAuthenticationService().getActorId();

		TaskInstance taskInstance = permission.getTaskInstance();
		
		if(taskInstance.getActorId() != null) {
			
			if(!loggedInActorId.equals(taskInstance))
				throw new AccessControlException("You shall not pass. Logged in actor id doesn't match the assigned actor id. Assigned: "+taskInstance.getActorId()+", taskInstanceId: "+taskInstance.getId());
			
		} else {
			
//			super admin always gets an access
			if(IWContext.getIWContext(FacesContext.getCurrentInstance()).isSuperAdmin())
				return;

			@SuppressWarnings("unchecked")
			Set<PooledActor> pooledActors = taskInstance.getPooledActors();
			
			if(pooledActors.isEmpty()) {
				throw new AccessControlException("You shall not pass. Pooled actors set was empty, for taskInstanceId: "+taskInstance.getId());
				
			} else {

				Collection<Long> pooledActorsIds = new ArrayList<Long>(pooledActors.size());
				
				for (PooledActor pooledActor : pooledActors) {
					
					Long actorId = new Long(pooledActor.getActorId());
					pooledActorsIds.add(actorId);
				}
				checkPermissionInPooledActors(new Integer(loggedInActorId), pooledActorsIds, taskInstance);
			}
		}
	}
	
	protected void checkPermissionInPooledActors(int userId, Collection<Long> pooledActors, TaskInstance taskInstance) throws AccessControlException {
	
		List<ProcessRole> assignedRolesIdentities = getBpmBindsDAO().getProcessRoles(pooledActors, taskInstance.getId());
		ArrayList<Long> filteredRolesIdentities = new ArrayList<Long>(assignedRolesIdentities.size());
		
		boolean writeAccessNeeded = !taskInstance.hasEnded();
		
		for (ProcessRole assignedRoleIdentity : assignedRolesIdentities) {

			List<TaskInstanceAccess> accesses = assignedRoleIdentity.getTaskInstanceAccesses();
			
			for (TaskInstanceAccess tiAccess : accesses) {
				
				if(tiAccess.getTaskInstanceId() == taskInstance.getId()) {
			
					if(tiAccess.hasAccess(Access.read) && (!writeAccessNeeded || tiAccess.hasAccess(Access.write))) {
				
						filteredRolesIdentities.add(assignedRoleIdentity.getActorId());
					}
					
					break;
				}
			}
		}
		
		if(!filteredRolesIdentities.isEmpty()) {

//			check for groups:
			List<NativeIdentityBind> nativeIdentities = getBpmBindsDAO().getNativeIdentities(filteredRolesIdentities, IdentityType.GROUP);
			
			if(!nativeIdentities.isEmpty()) {
				
				try {
					UserBusiness ub = getUserBusiness();
					@SuppressWarnings("unchecked")
					Collection<Group> userGroups = ub.getUserGroups(userId);
					
					for (Group group : userGroups) {
					
						String groupId = group.getPrimaryKey().toString();
						
						for (NativeIdentityBind nativeIdentity : nativeIdentities) {
							
							if(nativeIdentity.getIdentityId().equals(groupId))
								return;
						}
					}
					
				} catch (RemoteException e) {
					throw new IDORuntimeException(e);
				}
			}

//			check for roles
//			check for users
		}
		
		throw new AccessControlException("User ("+userId+") doesn't fall into any of pooled actors ("+pooledActors+"). Write access needed: "+writeAccessNeeded);
	}
	
	public void close() { }

	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}

	@Autowired
	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public BpmBindsDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	@Autowired
	public void setBpmBindsDAO(BpmBindsDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}
	
	protected UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil.getIWContext(), UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
}