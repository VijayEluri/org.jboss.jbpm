package com.idega.jbpm.data.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.data.DatastoreInterface;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.bean.VariableStringInstance;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.AutoloadedProcessDefinition;
import com.idega.jbpm.data.BPMVariableData;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessDefinitionVariablesBind;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.events.VariableCreatedEvent;
import com.idega.jbpm.identity.Role;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.28 $ Last modified: $Date: 2009/02/13 17:06:30 $ by $Author: donatas $
 */

@Repository("bpmBindsDAO")
@Transactional(readOnly = true)
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BPMDAOImpl extends GenericDaoImpl implements BPMDAO, ApplicationListener {
	
	private static final Logger LOGGER = Logger.getLogger(BPMDAOImpl.class.getName());
	
	@Autowired
	private BPMContext bpmContext;
	
	@Autowired
	private VariableInstanceQuerier variablesQuerier;
	
	public ViewTaskBind getViewTaskBind(long taskId, String viewType) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(
		    ViewTaskBind.GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME)
		        .setParameter(ViewTaskBind.taskIdParam, taskId).setParameter(
		            ViewTaskBind.viewTypeParam, viewType).getResultList();
		
		return binds.isEmpty() ? null : binds.iterator().next();
	}
	
	public ViewTaskBind getViewTaskBindByTaskInstance(long taskInstanceId,
	        String viewType) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager()
		        .createNamedQuery(
		            ViewTaskBind.GET_UNIQUE_BY_TASK_INSTANCE_ID_AND_VIEW_TYPE_QUERY_NAME)
		        .setParameter(ViewTaskBind.taskInstanceIdProp, taskInstanceId)
		        .setParameter(ViewTaskBind.viewTypeParam, viewType)
		        .getResultList();
		
		return binds.isEmpty() ? null : binds.iterator().next();
	}
	
	public List<ViewTaskBind> getViewTaskBindsByTaskId(long taskId) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(
		    ViewTaskBind.getViewTaskBindsByTaskId).setParameter(
		    ViewTaskBind.taskIdParam, taskId).getResultList();
		
		return binds;
	}
	
	public List<ViewTaskBind> getViewTaskBindsByTaskInstanceId(
	        long taskInstanceId) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(
		    ViewTaskBind.getViewTaskBindsByTaskInstanceId).setParameter(
		    ViewTaskBind.taskInstanceIdProp, taskInstanceId).getResultList();
		
		return binds;
	}
	
	public ViewTaskBind getViewTaskBindByView(String viewId, String viewType) {
		
		return getSingleResult(
		    ViewTaskBind.GET_VIEW_TASK_BIND_BY_VIEW_QUERY_NAME,
		    ViewTaskBind.class, new Param(ViewTaskBind.viewIdParam, viewId),
		    new Param(ViewTaskBind.viewTypeParam, viewType));
	}
	
	public List<ViewTaskBind> getViewTaskBindsByTasksIds(
	        Collection<Long> taskIds) {
		
		@SuppressWarnings("unchecked")
		List<ViewTaskBind> viewTaskBinds = getEntityManager().createNamedQuery(
		    ViewTaskBind.GET_VIEW_TASK_BINDS_BY_TASKS_IDS).setParameter(
		    ViewTaskBind.tasksIdsParam, taskIds).getResultList();
		
		return viewTaskBinds;
	}
	
	public Task getTaskFromViewTaskBind(ViewTaskBind viewTaskBind) {
		
		return (Task) getEntityManager().createNamedQuery(
		    ViewTaskBind.GET_VIEW_TASK).setParameter(
		    ViewTaskBind.viewTypeParam, viewTaskBind.getViewType())
		        .setParameter(ViewTaskBind.taskIdParam,
		            viewTaskBind.getTaskId()).getSingleResult();
	}
	
	public ProcessManagerBind getProcessManagerBind(String processName) {
		
		ProcessManagerBind pmb = getSingleResult(
		    ProcessManagerBind.getByProcessName, ProcessManagerBind.class,
		    new Param(ProcessManagerBind.processNameProp, processName));
		
		return pmb;
	}
	
	public List<Actor> getAllGeneralProcessRoles() {
		
		@SuppressWarnings("unchecked")
		List<Actor> all = getEntityManager().createNamedQuery(
		    Actor.getAllGeneral).getResultList();
		
		return all;
	}
	
	public List<Actor> getProcessRoles(Collection<Long> actorIds) {
		
		if (actorIds == null || actorIds.isEmpty())
			throw new IllegalArgumentException("ActorIds should contain values");
		
		@SuppressWarnings("unchecked")
		List<Actor> all = getEntityManager().createNamedQuery(
		    Actor.getAllByActorIds).setParameter(Actor.actorIdProperty,
		    actorIds).getResultList();
		
		return all;
	}
	
	@Transactional(readOnly = false)
	public void updateAddGrpsToRole(Long roleActorId,
	        Collection<String> selectedGroupsIds) {
		
		Actor roleIdentity = find(Actor.class, roleActorId);
		
		List<NativeIdentityBind> nativeIdentities = new ArrayList<NativeIdentityBind>(
		        selectedGroupsIds.size());
		
		for (String groupId : selectedGroupsIds) {
			
			NativeIdentityBind nativeIdentity = new NativeIdentityBind();
			nativeIdentity.setIdentityId(groupId);
			nativeIdentity.setIdentityType(IdentityType.GROUP);
			nativeIdentity.setActor(roleIdentity);
			nativeIdentities.add(nativeIdentity);
		}
		
		List<NativeIdentityBind> existingNativeIdentities = roleIdentity
		        .getNativeIdentities();
		List<Long> nativeIdentitiesToRemove = new ArrayList<Long>();
		
		if (existingNativeIdentities != null) {
			
			for (NativeIdentityBind existing : existingNativeIdentities) {
				
				if (nativeIdentities.contains(existing)) {
					
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
		getEntityManager().merge(roleIdentity);
		
		if (!nativeIdentitiesToRemove.isEmpty())
			getEntityManager().createNamedQuery(NativeIdentityBind.deleteByIds)
			        .setParameter(NativeIdentityBind.idsParam,
			            nativeIdentitiesToRemove).executeUpdate();
	}
	
	public List<NativeIdentityBind> getNativeIdentities(
	        long processRoleIdentityId) {
		
		@SuppressWarnings("unchecked")
		List<NativeIdentityBind> binds = getEntityManager().createNamedQuery(
		    NativeIdentityBind.getByProcIdentity).setParameter(
		    NativeIdentityBind.procIdentityParam, processRoleIdentityId)
		        .getResultList();
		
		return binds;
	}
	
	public List<NativeIdentityBind> getNativeIdentities(
	        Collection<Long> actorsIds, IdentityType identityType) {
		
		@SuppressWarnings("unchecked")
		List<NativeIdentityBind> binds = getEntityManager().createNamedQuery(
		    NativeIdentityBind.getByTypesAndProceIdentities).setParameter(
		    NativeIdentityBind.identityTypeProperty, identityType)
		        .setParameter(Actor.actorIdProperty, actorsIds).getResultList();
		
		return binds;
	}
	
	@Transactional(readOnly = false)
	public void updateCreateProcessRoles(Collection<Role> rolesNames,
	        Long processInstanceId) {
		
		for (Role role : rolesNames) {
			
			Actor prole = new Actor();
			prole.setProcessRoleName(role.getRoleName());
			prole.setProcessInstanceId(processInstanceId);
			
			persist(prole);
		}
	}
	
	public List<Object[]> getProcessTasksViewsInfos(
	        Collection<Long> processDefinitionsIds, String viewType) {
		
		if (processDefinitionsIds == null || processDefinitionsIds.isEmpty()
		        || viewType == null)
			return new ArrayList<Object[]>(0);
		
		@SuppressWarnings("unchecked")
		List<Object[]> viewsInfos = getEntityManager().createNamedQuery(
		    ViewTaskBind.GET_PROCESS_TASK_VIEW_INFO).setParameter(
		    ViewTaskBind.processDefIdsParam, processDefinitionsIds)
		        .setParameter(ViewTaskBind.viewTypeProp, viewType)
		        .getResultList();
		
		return viewsInfos;
	}
	
	public List<Actor> getProcessRoles(Collection<String> rolesNames,
	        Long processInstanceId) {
		
		List<Actor> proles = getResultList(Actor.getSetByRoleNamesAndPIId,
		    Actor.class, new Param(Actor.processRoleNameProperty, rolesNames),
		    new Param(Actor.processInstanceIdProperty, processInstanceId));
		
		return proles;
	}
	
	public List<ProcessInstance> getSubprocessInstancesOneLevel(
	        long parentProcessInstanceId) {
		
		List<ProcessInstance> subprocesses = getResultList(
		    ProcessManagerBind.getSubprocessesOneLevel, ProcessInstance.class,
		    new Param(ProcessManagerBind.processInstanceIdParam,
		            parentProcessInstanceId));
		
		return subprocesses;
	}
	
	public ProcessDefinition findLatestProcessDefinition(
	        final String processName) {
		
		return getBpmContext().execute(new JbpmCallback() {
			
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				
				return context.getGraphSession().findLatestProcessDefinition(
				    processName);
			}
		});
	}
	
	BPMContext getBpmContext() {
		return bpmContext;
	}
		
	public List<ActorPermissions> getPermissionsForUser(Integer userId,
	        String processName, Long processInstanceId,
	        Set<String> userNativeRoles, Set<String> userGroupsIds) {
		
		if (userGroupsIds != null) {
			throw new UnsupportedOperationException(
			        "Searching by user groups not supported yet");
		}
		
		if(processName != null) {
			throw new UnsupportedOperationException("Searching by process name not supported yet");
		}
		
		if(ListUtil.isEmpty(userNativeRoles)) {

//			this is perhaps silly, but just because we don't want to maintain two queries just for the case, when user doesn't have any roles
//			this is the case only for bpmUser usually
			userNativeRoles = new HashSet<String>(1);
			userNativeRoles.add("mock2345324659324");
		}
		
		String identityTypeRoleParam = "identityTypeRole";
		String identityIdsRolesParam = "identityIdsRoles";
		
		List<ActorPermissions> perms = getResultListByInlineQuery(
			
		    "select perms from com.idega.jbpm.data.Actor a inner join a."+ Actor.nativeIdentitiesProperty+ " ni inner join a."+Actor.actorPermissionsProperty+" perms "+
		    "where a."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty+" and " +
		    		"((ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty+" and ni."+NativeIdentityBind.identityIdProperty+" = :"+NativeIdentityBind.identityIdProperty+") " +
		    		"or (ni."+NativeIdentityBind.identityTypeProperty+" = :"+identityTypeRoleParam+" and ni."+NativeIdentityBind.identityIdProperty+" in (:"+identityIdsRolesParam+")))"
		    		
		            , ActorPermissions.class, 
		            new Param(Actor.processInstanceIdProperty, processInstanceId),
		            new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER),
		            new Param(NativeIdentityBind.identityIdProperty, userId.toString()),
		            new Param(identityTypeRoleParam, NativeIdentityBind.IdentityType.ROLE),
		            new Param(identityIdsRolesParam, userNativeRoles));
		
//		TODO: merge with roles (backward)
		List<Actor> globalRolesActors = getResultListByInlineQuery(
			
//			a."+ Actor.nativeIdentitiesProperty+ "
			
		    "select a from com.idega.jbpm.data.Actor a "+
		    "where a."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty+
		    " and a."+Actor.processRoleNameProperty+" in(:"+Actor.processRoleNameProperty+")"
		            , Actor.class, 
		            new Param(Actor.processInstanceIdProperty, processInstanceId),
		            new Param(Actor.processRoleNameProperty, userNativeRoles)
		);
		
		if(globalRolesActors != null) {
			
			for (Actor actor : globalRolesActors) {
		        
				if(ListUtil.isEmpty(actor.getNativeIdentities())) {
					
//					this is in old way the pd scope actor

					if(actor.getActorPermissions() != null) {
						perms.addAll(actor.getActorPermissions());
					}
				}
	        }	
		}
		
		return perms;
	}

	public int getTaskViewBindCount(String viewId, String viewType) {
		return getResultList(ViewTaskBind.GET_VIEW_TASK_BIND_BY_VIEW_QUERY_NAME,
			    ViewTaskBind.class, new Param(ViewTaskBind.viewIdParam, viewId),
			    new Param(ViewTaskBind.viewTypeParam, viewType)).size();
	}

	private List<AutoloadedProcessDefinition> getAllLoadedProcessDefinitions() {
		try {
			return getResultList(AutoloadedProcessDefinition.QUERY_SELECT_ALL, AutoloadedProcessDefinition.class);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting auto loaded process definitions");
		}
		return null;
	}

	private List<ProcessDefinitionVariablesBind> getAllProcDefVariableBinds() {
		try {
			return getResultList(ProcessDefinitionVariablesBind.QUERY_SELECT_ALL, ProcessDefinitionVariablesBind.class);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variables for process definitions", e);
		}
		return null;
	}
	
	public void bindProcessVariables() {
		List<AutoloadedProcessDefinition> procDefs = getAllLoadedProcessDefinitions();
		if (ListUtil.isEmpty(procDefs)) {
			return;
		}
		
		List<ProcessDefinitionVariablesBind> currentBinds = getAllProcDefVariableBinds();
		
		for (AutoloadedProcessDefinition apd: procDefs) {
			bindProcessVariables(apd.getProcessDefinitionName(), currentBinds);
		}
	}
	
	private void bindProcessVariables(String processDefinitionName) {
		bindProcessVariables(processDefinitionName, getAllProcDefVariableBinds());
	}
	
	@Transactional(readOnly = false)
	private void bindProcessVariables(String processDefinitionName, List<ProcessDefinitionVariablesBind> currentBinds) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			return;
		}
		
		Collection<VariableInstanceInfo> vars = getVariablesQuerier().getVariablesByProcessDefinitionNaiveWay(processDefinitionName);
		if (ListUtil.isEmpty(vars)) {
			return;
		}
		
		currentBinds = currentBinds == null ? new ArrayList<ProcessDefinitionVariablesBind>(0) : currentBinds;
		
		for (VariableInstanceInfo var: vars) {
			String variableName = var.getName();
			if (StringUtil.isEmpty(variableName)) {
				continue;
			}
			
			try {
				if (!bindExists(currentBinds, variableName, processDefinitionName)) {
					ProcessDefinitionVariablesBind bind = new ProcessDefinitionVariablesBind();
					bind.setProcessDefinition(processDefinitionName);
					bind.setVariableName(var.getName());
					bind.setVariableType(var.getType().getTypeKeys().get(0));
					persist(bind);
					currentBinds.add(bind);
					LOGGER.info("Added new bind: " + bind);
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error adding new bind: " + var + " for process: " + processDefinitionName, e);
			}
		}
	}
	
	private boolean bindExists(List<ProcessDefinitionVariablesBind> currentBinds, String variableName, String processDefinitionName) {
		if (ListUtil.isEmpty(currentBinds)) {
			return false;
		}
		
		String expression = variableName.concat("@").concat(processDefinitionName);
		for (ProcessDefinitionVariablesBind bind: currentBinds) {
			if (bind.toString().equals(expression)) {
				return true;
			}
		}
		return false;
	}
	
	VariableInstanceQuerier getVariablesQuerier() {
		return variablesQuerier;
	}

	public void onApplicationEvent(final ApplicationEvent event) {
		if (event instanceof VariableCreatedEvent) {
			Thread binder = new Thread(new Runnable() {
				public void run() {
					VariableCreatedEvent variableCreated = (VariableCreatedEvent) event;
					bindProcessVariables(variableCreated.getProcessDefinitionName());
				}
			});
			binder.start();
		}
	}
	
	private List<Long> getAllProcessInstances() {
		try {
			return getResultListByInlineQuery("select pi.id from org.jbpm.graph.exe.ProcessInstance pi", Long.class);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting all process instances", e);
		}
		return null;
	}
	
	private List<Long> getExisitingVariables() {
		try {
			return getResultListByInlineQuery("select distinct var.variableId from " + BPMVariableData.class.getName() + " var", Long.class);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting all process instances", e);
		}
		return null;
	}
	
	public void importVariablesData() {
		IWMainApplicationSettings settings = IWMainApplication.getDefaultIWMainApplication().getSettings();
		String property = "jbpm_vars_data_imported";
		if (settings.getBoolean(property, Boolean.FALSE)) {
			return;
		}
		
		prepareTable();
		doImportData();
		
		settings.setProperty(property, Boolean.TRUE.toString());
	}
	
	private void prepareTable() {
		createIndex(BPMVariableData.TABLE_NAME, "IDX_" + BPMVariableData.TABLE_NAME + "_VAR", BPMVariableData.COLUMN_VARIABLE_ID);
		createIndex(BPMVariableData.TABLE_NAME, "IDX_" + BPMVariableData.TABLE_NAME + "_VAL", BPMVariableData.COLUMN_VALUE);
		
		createTrigger("CREATE TRIGGER BPM_VARIABLE_INSERTED AFTER INSERT ON JBPM_VARIABLEINSTANCE " +
						"FOR EACH ROW BEGIN " +
							"IF NEW.stringvalue_ is not null THEN " +
								"insert into BPM_VARIABLE_DATA (variable_id, stringvalue) values (NEW.ID_, substr(NEW.stringvalue_, 1, 255)); " +
							"END IF; "+
						"END;"
		);
		createTrigger("CREATE TRIGGER BPM_VARIABLE_UPDATED AFTER UPDATE ON JBPM_VARIABLEINSTANCE "+
						"FOR EACH ROW BEGIN " +
							" update BPM_VARIABLE_DATA set stringvalue=substr(NEW.stringvalue_, 1, 255) where variable_id=NEW.ID_; " +
						"END;"
		);
		createTrigger("CREATE TRIGGER BPM_VARIABLE_DELETED BEFORE DELETE ON JBPM_VARIABLEINSTANCE " +
						"FOR EACH ROW BEGIN " +
							"delete from BPM_VARIABLE_DATA where variable_id=OLD.ID_; " +
						"END;"
		);
	}
	
	private void doImportData() {
		List<Long> piIds = getAllProcessInstances();
		if (ListUtil.isEmpty(piIds)) {
			return;
		}
		
		List<Long> varsIds = getExisitingVariables();
		if (ListUtil.isEmpty(varsIds)) {
			LOGGER.info("There are no existing variables in table " + BPMVariableData.TABLE_NAME);
		}
		
		int step = 100;
		try {
			for (int i = 0; i < piIds.size(); i = i + step) {
				List<Long> subList = null;
				try {
					int from = i;
					int to = piIds.size() < (i + step) ? (i + piIds.size()) : (i + step);
					to = to < from ? from : to;
					to = to > piIds.size() ? piIds.size() : to;
					subList = piIds.subList(from, to);
					if (importVariablesData(getVariablesQuerier().getFullVariablesByProcessInstanceIdsNaiveWay(subList, varsIds))) {
						flush();
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error getting variables by IDs: " + subList, e);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error while importing data for processes: " + piIds + "\n, number of processes: " + piIds.size(), e);
		}
	}
	
	private void createIndex(String table, String name, String column) {
		try {
			DatastoreInterface dataInterface = DatastoreInterface.getInstance();
			dataInterface.createIndex(table, name, new String[] {column});
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error creating index '" + name + "' for table '" + table + "' and column: '" + column + "'", e);
		}
	}
	
	private void createTrigger(String triggerSQL) {
		try {
			DatastoreInterface dataInterface = DatastoreInterface.getInstance();
			dataInterface.createTrigger(triggerSQL);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error creating trigger: " + triggerSQL, e);
		}
	}
	
	private boolean importVariablesData(Collection<VariableInstanceInfo> variables) {
		if (ListUtil.isEmpty(variables)) {
			LOGGER.info("No variables to import: empty list provided.");
			return Boolean.FALSE;
		}
		
		boolean peristedAny = Boolean.FALSE;;
		for (VariableInstanceInfo var: variables) {
			if (var instanceof VariableStringInstance) {
				VariableStringInstance stringVar = (VariableStringInstance) var;
				String value = stringVar.getValue();
				if (value != null && value.length() > 255) {
					value = value.substring(0, 254);
				}
				
				BPMVariableData varData = new BPMVariableData();
				varData.setValue(value);
				varData.setVariableId(stringVar.getId());
				try {
					persist(varData);
					peristedAny = Boolean.TRUE;
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error storing data to DB: " + varData, e);
				}
			}
		}
		
		return peristedAny;
	}
}