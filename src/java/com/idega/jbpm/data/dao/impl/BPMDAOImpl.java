package com.idega.jbpm.data.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.jbpm.data.ActorTaskBind;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.TaskInstanceAccess;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.permission.Access;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/03/12 12:41:57 $ by $Author: civilis $
 */
@Scope("singleton")
@Repository("bpmBindsDAO")
@Transactional(readOnly=true)
public class BPMDAOImpl extends GenericDaoImpl implements BPMDAO {

	public ActorTaskBind getActorTaskBind(long taskId, String actorType) {
		
		return (ActorTaskBind) getEntityManager().createNamedQuery(ActorTaskBind.GET_UNIQUE_BY_TASK_ID_AND_ACTOR_TYPE_QUERY_NAME)
		.setParameter(ActorTaskBind.taskIdParam, taskId)
		.setParameter(ActorTaskBind.actorTypeParam, actorType)
		.getSingleResult();
	}
	
	public ActorTaskBind getActorTaskBind(long taskId) {
		
		return (ActorTaskBind) getEntityManager().createNamedQuery(ActorTaskBind.GET_UNIQUE_BY_TASK_ID_QUERY_NAME)
		.setParameter(ActorTaskBind.taskIdParam, taskId)
		.getSingleResult();
	}
	
	public ViewTaskBind getViewTaskBind(long taskId, String viewType) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(ViewTaskBind.GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME)
		.setParameter(ViewTaskBind.taskIdParam, taskId)
		.setParameter(ViewTaskBind.viewTypeParam, viewType)
		.getResultList();

		return binds.isEmpty() ? null : binds.iterator().next();
	}
	
	public List<ViewTaskBind> getViewTaskBindsByTaskId(long taskId) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(ViewTaskBind.getViewTaskBindsByTaskId)
		.setParameter(ViewTaskBind.taskIdParam, taskId)
		.getResultList();

		return binds;
	}
	
	public ViewTaskBind getViewTaskBindByView(String viewId, String viewType) {
		
		return (ViewTaskBind)getEntityManager().createNamedQuery(ViewTaskBind.GET_VIEW_TASK_BIND_BY_VIEW_QUERY_NAME)
		.setParameter(ViewTaskBind.viewIdParam, viewId)
		.setParameter(ViewTaskBind.viewTypeParam, viewType)
		.getSingleResult();
	}
	
	public List<ViewTaskBind> getViewTaskBindsByTasksIds(Collection<Long> taskIds) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> viewTaskBinds = getEntityManager().createNamedQuery(ViewTaskBind.GET_VIEW_TASK_BINDS_BY_TASKS_IDS)
		.setParameter(ViewTaskBind.tasksIdsParam, taskIds)
		.getResultList();

		return viewTaskBinds;
	}

	public Task getTaskFromViewTaskBind(ViewTaskBind viewTaskBind) {
		
		return (Task)getEntityManager().createNamedQuery(ViewTaskBind.GET_VIEW_TASK)
		.setParameter(ViewTaskBind.viewTypeParam, viewTaskBind.getViewType())
		.setParameter(ViewTaskBind.taskIdParam, viewTaskBind.getTaskId())
		.getSingleResult();
	}
	
	public ManagersTypeProcessDefinitionBind getManagersTypeProcDefBind(long processDefinitionId) {
		
		return 
		(ManagersTypeProcessDefinitionBind)
		getEntityManager().createNamedQuery(ManagersTypeProcessDefinitionBind.managersTypeProcessDefinitionBind_getByProcessDefinitionId)
		.setParameter(ManagersTypeProcessDefinitionBind.processDefinitionIdParam, processDefinitionId)
		.getSingleResult();
	}
	
	public List<ProcessDefinition> getAllManagersTypeProcDefs() {
	
		@SuppressWarnings("unchecked")
		List<ProcessDefinition> all = getEntityManager().createNamedQuery(ManagersTypeProcessDefinitionBind.managersTypeProcessDefinitionBind_getAllProcDefs)
		.getResultList();
		
		return all;
	}
	
	public List<ProcessRole> getAllGeneralProcessRoles() {
		
		@SuppressWarnings("unchecked")
		List<ProcessRole> all = getEntityManager().createNamedQuery(ProcessRole.getAllGeneral)
		.getResultList();
		
		return all;
	}
	
	@SuppressWarnings("unchecked")
	public List<ProcessRole> getProcessRolesByRolesNames(Collection<String> rolesNames, Long processInstanceId) {
		
		if(rolesNames == null || rolesNames.isEmpty())
			throw new IllegalArgumentException("Roles names should contain values");
		
		List<ProcessRole> all;
		
		if(processInstanceId == null) {
			
			all = getEntityManager().createNamedQuery(ProcessRole.getAllByRoleNamesAndPIIdIsNull)
			.setParameter(ProcessRole.processRoleNameProperty, rolesNames)
			.getResultList();
			
		} else {
		
			all = getEntityManager().createNamedQuery(ProcessRole.getAllByRoleNamesAndPIId)
			.setParameter(ProcessRole.processRoleNameProperty, rolesNames)
			.setParameter(ProcessRole.processInstanceIdProperty, processInstanceId)
			.getResultList();
		}
		
		return all;
	}
	
	public List<ProcessRole> getProcessRoles(Collection<Long> actorIds) {
		
		if(actorIds == null || actorIds.isEmpty())
			throw new IllegalArgumentException("ActorIds should contain values");
		
		@SuppressWarnings("unchecked")
		List<ProcessRole> all = getEntityManager().createNamedQuery(ProcessRole.getAllByActorIds)
		.setParameter(ProcessRole.actorIdProperty, actorIds)
		.getResultList();
		
		return all;
	}
	
	public List<ProcessRole> getProcessRoles(Collection<Long> actorIds, long taskInstanceId) {
		
		if(actorIds == null || actorIds.isEmpty())
			throw new IllegalArgumentException("ActorIds should contain values");
		
		@SuppressWarnings("unchecked")
		List<ProcessRole> all = getEntityManager().createNamedQuery(ProcessRole.getAssignedToTaskInstances)
		.setParameter(ProcessRole.actorIdProperty, actorIds)
		.setParameter(TaskInstanceAccess.taskInstanceIdProperty, taskInstanceId)
		.getResultList();
		
		return all;
	}
	
	@Transactional(readOnly = false)
	public void updateAddGrpsToRole(Long roleActorId, Collection<String> selectedGroupsIds) {
		
		ProcessRole roleIdentity = find(ProcessRole.class, roleActorId);
		
		List<NativeIdentityBind> nativeIdentities = new ArrayList<NativeIdentityBind>(selectedGroupsIds.size());
		
		for (String groupId : selectedGroupsIds) {
		
			NativeIdentityBind nativeIdentity = new NativeIdentityBind();
			nativeIdentity.setIdentityId(groupId);
			nativeIdentity.setIdentityType(IdentityType.GROUP);
			nativeIdentity.setProcessRole(roleIdentity);
			nativeIdentities.add(nativeIdentity);
		}
		
		List<NativeIdentityBind> existingNativeIdentities = roleIdentity.getNativeIdentities();
		List<Long> nativeIdentitiesToRemove = new ArrayList<Long>();
		
		if(existingNativeIdentities != null) {
		
			for (NativeIdentityBind existing : existingNativeIdentities) {
				
				if(nativeIdentities.contains(existing)) {
					
					nativeIdentities.remove(existing);
					nativeIdentities.add(existing);
				} else {
					
					nativeIdentitiesToRemove.add(existing.getId());
				}
			}
		} else {
			existingNativeIdentities = new ArrayList<NativeIdentityBind>();
		}
		
		roleIdentity.setNativeIdentities(nativeIdentities);
		merge(roleIdentity);
		
		if(!nativeIdentitiesToRemove.isEmpty())
			getEntityManager().createNamedQuery(NativeIdentityBind.deleteByIds)
				.setParameter(NativeIdentityBind.idsParam, nativeIdentitiesToRemove)
				.executeUpdate();
	}
	
	public List<NativeIdentityBind> getNativeIdentities(long processRoleIdentityId) {
		
		@SuppressWarnings("unchecked")
		List<NativeIdentityBind> binds = getEntityManager().createNamedQuery(NativeIdentityBind.getByProcIdentity)
		.setParameter(NativeIdentityBind.procIdentityParam, processRoleIdentityId)
		.getResultList();
		
		return binds;
	}
	
	public List<NativeIdentityBind> getNativeIdentities(List<Long> actorsIds, IdentityType identityType) {

		@SuppressWarnings("unchecked")
		List<NativeIdentityBind> binds = getEntityManager().createNamedQuery(NativeIdentityBind.getByTypesAndProceIdentities)
		.setParameter(NativeIdentityBind.identityTypeProperty, identityType)
		.setParameter(ProcessRole.actorIdProperty, actorsIds)
		.getResultList();
		
		return binds;
	}
	
	@Transactional(readOnly = false)
	public Collection<String> updateAssignTaskAccesses(long taskInstanceId, Map<Role, ProcessRole> proles, Integer userIdentityId) {
		
		HashSet<String> actorIds = new HashSet<String>(proles.size());
		
		for (Entry<Role, ProcessRole> entry : proles.entrySet()) {
			
			ProcessRole role = (ProcessRole)merge(entry.getValue());
			
			TaskInstanceAccess tiAccess = new TaskInstanceAccess();
			tiAccess.setProcessRole(role);
			tiAccess.setTaskInstanceId(taskInstanceId);
			
			for (Access access : entry.getKey().getAccesses())
				tiAccess.addAccess(access);
			
			persist(tiAccess);
			
			if(!entry.getKey().isGeneral() && userIdentityId != null) {
				
				NativeIdentityBind userIdentity = new NativeIdentityBind();
				userIdentity.setIdentityId(userIdentityId.toString());
				userIdentity.setIdentityType(IdentityType.USER);
				userIdentity.setProcessRole(role);
				
				persist(userIdentity);
			}
			
			actorIds.add(entry.getValue().getActorId().toString());
		}
		
		return actorIds;
	}
	
	@Transactional(readOnly = false)
	public void updateCreateProcessRoles(Collection<String> rolesNames, Long processInstanceId) {
		
		for (String roleName : rolesNames) {
			
			ProcessRole prole = new ProcessRole();
			prole.setProcessRoleName(roleName);
			prole.setProcessInstanceId(processInstanceId);
			
			persist(prole);
		}
	}
}