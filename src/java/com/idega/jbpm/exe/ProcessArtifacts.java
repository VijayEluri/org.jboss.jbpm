package com.idega.jbpm.exe;

import java.rmi.RemoteException;
import java.security.Permission;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.SubmitTaskParametersPermission;
import com.idega.jbpm.identity.permission.ViewTaskParametersPermission;
import com.idega.jbpm.presentation.beans.ProcessArtifactsParamsBean;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRow;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRows;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.20 $
 *
 * Last modified: $Date: 2008/03/17 12:19:00 $ by $Author: civilis $
 */
public class ProcessArtifacts {
	
	private BPMFactory bpmFactory;
	private IdegaJbpmContext idegaJbpmContext;
	
	private Logger logger = Logger.getLogger(ProcessArtifacts.class.getName());

	public Document getProcessDocumentsList(ProcessArtifactsParamsBean params) {
		
		Long processInstanceId = params.getPiId();
		
//		TODO: don't return null, but empty doc instead
		if(processInstanceId == null)
			return null;
		
		Collection<TaskInstance> processDocuments = getSubmittedTaskInstances(processInstanceId);
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

		int size = processDocuments.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		RolesManager rolesManager = getBpmFactory().getRolesManager();

		for (TaskInstance submittedDocument : processDocuments) {
			
			try {
				Permission permission = getTaskSubmitPermission(true, submittedDocument);
				rolesManager.checkPermission(permission);
				
			} catch (BPMAccessControlException e) {
				continue;
			}
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			String tidStr = String.valueOf(submittedDocument.getId());
			row.setId(tidStr);
			row.addCell(tidStr);
			row.addCell(submittedDocument.getName());
			row.addCell(submittedDocument.getEnd() == null ? CoreConstants.EMPTY :
				new IWTimestamp(submittedDocument.getEnd()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)
			);
		}
		
		try {
			return rows.getDocument();
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	protected Permission getTaskSubmitPermission(boolean authPooledActorsOnly, TaskInstance taskInstance) {
		
		SubmitTaskParametersPermission permission = new SubmitTaskParametersPermission("taskInstance", null, taskInstance);
		permission.setCheckOnlyInActorsPool(authPooledActorsOnly);
		
		return permission;
	}
	
	protected Permission getTaskViewPermission(boolean authPooledActorsOnly, TaskInstance taskInstance) {
		
		ViewTaskParametersPermission permission = new ViewTaskParametersPermission("taskInstance", null, taskInstance);
		permission.setCheckOnlyInActorsPool(authPooledActorsOnly);
		
		return permission;
	}
	
	public Document getProcessTasksList(ProcessArtifactsParamsBean params) {
		
		Long processInstanceId = params.getPiId();
		
		if(processInstanceId == null)
			return null;
	
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
			
			ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
			
			@SuppressWarnings("unchecked")
			List<Token> tokens = processInstance.findAllTokens();
			
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> tasks = processInstance.getTaskMgmtInstance().getUnfinishedTasks(processInstance.getRootToken());
			
			for (Token token : tokens) {
				
				if(!token.equals(processInstance.getRootToken())) {
			
					@SuppressWarnings("unchecked")
					Collection<TaskInstance> tsks = processInstance.getTaskMgmtInstance().getUnfinishedTasks(token);
					tasks.addAll(tsks);
				}
			}
			
			ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

			int size = tasks.size();
			rows.setTotal(size);
			rows.setPage(size == 0 ? 0 : 1);
			
			RolesManager rolesManager = getBpmFactory().getRolesManager();
			String loggedInUserId = String.valueOf(iwc.getCurrentUserId());
			
			for (TaskInstance taskInstance : tasks) {
				
				if(taskInstance.getToken().hasEnded())
					continue;
				
				try {
					
					Permission permission = getTaskSubmitPermission(true, taskInstance);
					rolesManager.checkPermission(permission);
					
				} catch (BPMAccessControlException e) {
					continue;
				}
				
				boolean disableSelection = false;
				String assignedToName;
				
				if(taskInstance.getActorId() != null) {
					
					if(taskInstance.getActorId().equals(loggedInUserId)) {
						disableSelection = false;
						assignedToName = "You";
						
					} else {
						disableSelection = true;
						
						try {
							assignedToName = getUserBusiness().getUser(Integer.parseInt(taskInstance.getActorId())).getName();
						} catch (Exception e) {
							Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving actor name for actorId: "+taskInstance.getActorId(), e);
							assignedToName = CoreConstants.EMPTY;
						}
					}
					
				} else {
					
					assignedToName = "No one";
				}
				
				String status = getTaskStatus(taskInstance);
				
				ProcessArtifactsListRow row = new ProcessArtifactsListRow();
				rows.addRow(row);
				
				String tidStr = String.valueOf(taskInstance.getId());
				row.setId(tidStr);
				row.addCell(tidStr);
				row.addCell(taskInstance.getName());
				row.addCell(taskInstance.getCreate() == null ? CoreConstants.EMPTY :
							new IWTimestamp(taskInstance.getCreate()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)
				);
				row.addCell(assignedToName);
				row.addCell(status);

				if(disableSelection) {
					
					row.setStyleClass("disabledSelection");
					row.setDisabledSelection(disableSelection);
				}
			}
			
			try {
				return rows.getDocument();
				
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Exception while parsing rows", e);
				return null;
			}
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	protected String getTaskStatus(TaskInstance taskInstance) {
		
		if(taskInstance.hasEnded())
			return "Ended";
		if(taskInstance.getStart() != null)
			return "In progress";
		
		return "Not started";
	}
	
	public org.jdom.Document getViewDisplay(Long taskInstanceId) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			long processDefinitionId = ctx.getTaskInstance(taskInstanceId).getProcessInstance().getProcessDefinition().getId();
			
			UIComponent viewUIComponent = getBpmFactory().getViewManager(processDefinitionId).loadTaskInstanceView(taskInstanceId, FacesContext.getCurrentInstance()).getViewForDisplay();
			return getBuilderService().getRenderedComponent(IWContext.getIWContext(FacesContext.getCurrentInstance()), viewUIComponent, true);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	protected Collection<TaskInstance> getSubmittedTaskInstances(Long processInstanceId) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
			
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getTaskInstances();
			
			for (Iterator<TaskInstance> iterator  = taskInstances.iterator(); iterator.hasNext();) {
				TaskInstance taskInstance = iterator.next();
				
				if(!taskInstance.hasEnded())
					iterator.remove();
			}
			
			return taskInstances;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	protected BuilderService getBuilderService() {
		
		try {
			return BuilderServiceFactory.getBuilderService(IWMainApplication.getDefaultIWApplicationContext());
		} catch (RemoteException e) {
			throw new RuntimeException("Error while retrieving builder service", e);
		}
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
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