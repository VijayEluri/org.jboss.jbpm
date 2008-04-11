package com.idega.jbpm.exe.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.def.View;
import com.idega.jbpm.def.ViewFactory;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.BPMManagersFactory;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ViewManager;
import com.idega.jbpm.identity.RolesManager;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2008/04/11 01:28:24 $ by $Author: civilis $
 */
public class BPMFactoryImpl implements BPMFactory, ApplicationContextAware {
	
	private ApplicationContext applicationContext;
	private final Map<String, String> creatorTypeCreatorBeanIdentifier;
	private final Map<String, String> viewTypeFactoryBeanIdentifier;
	
	private BPMDAO bindsDAO;
	private IdegaJbpmContext idegaJbpmContext;
	private RolesManager rolesManager;
	
	public BPMFactoryImpl() {
		creatorTypeCreatorBeanIdentifier = new HashMap<String, String>(5);
		viewTypeFactoryBeanIdentifier = new HashMap<String, String>(5);
	}
	
	public ProcessManager getProcessManager(long processDefinitionId) {

		return getManagersCreator(processDefinitionId).getProcessManager();
	}

	public ViewManager getViewManager(long processDefinitionId) {
		
		return getManagersCreator(processDefinitionId).getViewManager();
	}
	
	public View takeView(long taskInstanceId, boolean submitable, List<String> preferredTypes) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance ti = ctx.getTaskInstance(taskInstanceId);
			
			View view = getViewByTask(ti.getTask().getId(), submitable, preferredTypes);
			
			if(view != null) {
				
				view.takeView();
				view.getViewToTask().bind(view, ti);
			}
			
			return view;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public View getViewByTask(long taskId, boolean submitable, List<String> preferredTypes) {
	
		List<ViewTaskBind> binds = getBindsDAO().getViewTaskBindsByTaskId(taskId);
		
		if(binds == null || binds.isEmpty()) {
			Logger.getLogger(BPMFactory.class.getName()).log(Level.WARNING, "No view task bindings resolved for task. Task id: "+taskId);
			return null;
		}
		
		ViewTaskBind viewTaskBind = getPreferredViewTaskBind(binds, preferredTypes);
		String viewType = viewTaskBind.getViewType();
		
		ViewFactory viewFactory = getViewFactory(viewType);
		return viewFactory.getView(viewTaskBind.getViewIdentifier(), submitable);
	}
	
	public View getViewByTaskInstance(long taskInstanceId, boolean submitable, List<String> preferredTypes) {
		
		List<ViewTaskBind> binds = getBindsDAO().getViewTaskBindsByTaskInstanceId(taskInstanceId);
		
		if(binds == null || binds.isEmpty()) {
			Logger.getLogger(BPMFactory.class.getName()).log(Level.WARNING, "No view task bindings resolved for task intance. Task intance id: "+taskInstanceId);
			return null;
		}
		
		ViewTaskBind viewTaskBind = getPreferredViewTaskBind(binds, preferredTypes);
		String viewType = viewTaskBind.getViewType();
		
		ViewFactory viewFactory = getViewFactory(viewType);
		return viewFactory.getView(viewTaskBind.getViewIdentifier(), submitable);
	}
	
	protected ViewTaskBind getPreferredViewTaskBind(List<ViewTaskBind> binds, List<String> preferredTypes) {
		
		ViewTaskBind viewTaskBind = null;
		
		if(preferredTypes != null) {
			
			for (String preferredType : preferredTypes) {
				
				for (ViewTaskBind bind : binds) {
				
					if(preferredType.equals(bind.getViewType())) {
						viewTaskBind = bind;
						break;
					}
				}
			}
		}
		
		if(viewTaskBind == null && !binds.isEmpty())
			viewTaskBind = binds.get(0);
		
		return viewTaskBind;
	}
	
	protected ViewFactory getViewFactory(String viewType) {
		
		ViewFactory viewFactory;
		
		if(getViewTypeFactoryBeanIdentifier().containsKey(viewType)) {
			viewFactory = (ViewFactory)getApplicationContext().getBean(getViewTypeFactoryBeanIdentifier().get(viewType));
			
		} else {
			throw new IllegalStateException("No View Factory registered for view type: "+viewType);
		}
		
		return viewFactory;
	}
	
	protected BPMManagersFactory getManagersCreator(long processDefinitionId) {
		
		String managersType = resolveManagersType(processDefinitionId);
		
		if("default".equals(managersType))
//			TODO: support this
			throw new UnsupportedOperationException("No managers type found for process definition id provided: "+processDefinitionId+". Default managers implementation not supported yet");
		
		BPMManagersFactory creator;
		
		if(getCreatorTypeCreatorBeanIdentifier().containsKey(managersType)) {
			creator = (BPMManagersFactory)getApplicationContext().getBean(creatorTypeCreatorBeanIdentifier.get(managersType));
			
		} else {
			throw new IllegalStateException("No managers creator registered for type resolved: "+managersType+", process definition id: "+processDefinitionId);
		}
		
		return creator;
	}
	
	protected String resolveManagersType(long processDefinitionId) {

		ManagersTypeProcessDefinitionBind bind = getBindsDAO().getManagersTypeProcDefBind(processDefinitionId);
		
		if(bind == null) {
			
			return "default";
		}

		return bind.getManagersType();
	}
	
	@Autowired
	public void setBPManagersFactories(List<BPMManagersFactory> bpmManagersFactories) {
		
		for (BPMManagersFactory managersFactory : bpmManagersFactories) {
		
			if(managersFactory.getManagersType() == null)
				throw new IllegalArgumentException("Managers factory type not specified for factory: "+managersFactory);
			
			String beanIdentifier = managersFactory.getBeanIdentifier();
			
			if(beanIdentifier == null) {
				Logger.getLogger(BPMFactory.class.getName()).log(Level.WARNING, "No bean identifier provided for managers factory, ignoring. Managers factory: "+managersFactory.getClass().getName());
			} else
				getCreatorTypeCreatorBeanIdentifier().put(managersFactory.getManagersType(), beanIdentifier);
		}
	}
	
	@Autowired
	public void setViewsFactories(List<ViewFactory> viewsFactories) {
		
		for (ViewFactory viewFactory : viewsFactories) {
			
			if(viewFactory.getViewType() == null)
				throw new IllegalArgumentException("View factory type not specified for factory: "+viewFactory);
			
			String beanIdentifier = viewFactory.getBeanIdentifier();
			
			if(beanIdentifier == null) {
				Logger.getLogger(BPMFactory.class.getName()).log(Level.WARNING, "No bean identifier provided for view factory, ignoring. View factory: "+viewFactory.getClass().getName());
			} else
				getViewTypeFactoryBeanIdentifier().put(viewFactory.getViewType(), beanIdentifier);
		}
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	protected ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	protected Map<String, String> getCreatorTypeCreatorBeanIdentifier() {
		
		return creatorTypeCreatorBeanIdentifier;
	}

	public BPMDAO getBindsDAO() {
		return bindsDAO;
	}

	@Autowired
	public void setBindsDAO(BPMDAO bindsDAO) {
		this.bindsDAO = bindsDAO;
	}

	public Map<String, String> getViewTypeFactoryBeanIdentifier() {
		
		return viewTypeFactoryBeanIdentifier;
	}

	public ProcessManager getProcessManagerByTaskInstanceId(long taskInstanceId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			long pdId = taskInstance.getProcessInstance().getProcessDefinition().getId();
			return getProcessManager(pdId);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}
}