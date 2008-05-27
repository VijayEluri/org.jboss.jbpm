package com.idega.jbpm.artifacts.presentation;

import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jboss.jbpm.IWBundleStarter;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.Phone;
import com.idega.core.location.data.Address;
import com.idega.core.location.data.Country;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.artifacts.ProcessArtifactsProvider;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.BPMAccessControlException;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.SubmitTaskParametersPermission;
import com.idega.jbpm.identity.permission.ViewTaskParametersPermission;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRow;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRows;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.Table2;
import com.idega.presentation.TableBodyRowGroup;
import com.idega.presentation.TableCell2;
import com.idega.presentation.TableHeaderCell;
import com.idega.presentation.TableHeaderRowGroup;
import com.idega.presentation.TableRow;
import com.idega.presentation.text.Heading3;
import com.idega.presentation.text.Link;
import com.idega.presentation.text.Text;
import com.idega.presentation.ui.CheckBox;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.user.util.UserComparator;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.13 $
 *
 * Last modified: $Date: 2008/05/27 10:58:20 $ by $Author: valdas $
 */
@Scope("singleton")
@Service(CoreConstants.SPRING_BEAN_NAME_PROCESS_ARTIFACTS)
public class ProcessArtifacts {
	
	private BPMFactory bpmFactory;
	private IdegaJbpmContext idegaJbpmContext;
	private VariablesHandler variablesHandler;
	private ProcessArtifactsProvider processArtifactsProvider;
	
	private Logger logger = Logger.getLogger(ProcessArtifacts.class.getName());
	
	private Document getDocumentsListDocument(Collection<TaskInstance> processDocuments, Long processInstanceId, boolean rightsChanger) {
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

		int size = processDocuments.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		RolesManager rolesManager = getBpmFactory().getRolesManager();

		String pdfUri = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getVirtualPathWithFileNameString("images/pdf.gif");
		for (TaskInstance submittedDocument : processDocuments) {
			
			try {
				Permission permission = getTaskSubmitPermission(true, submittedDocument);
				rolesManager.checkPermission(permission);
				
			} catch (BPMAccessControlException e) {
				continue;
			}
			
			String actorId = submittedDocument.getActorId();
			String submittedByName;
			
			if(actorId != null) {
				
				try {
					User usr = getUserBusiness().getUser(Integer.parseInt(actorId));
					
					if(BPMUser.bpmUserIdentifier.equals(usr.getMiddleName())) {
						
						submittedByName = usr.getFirstName();
					} else
						submittedByName = usr.getName();
					
				} catch (Exception e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving actor name for actorId: "+actorId, e);
					submittedByName = CoreConstants.EMPTY;
				}
				
			} else
				submittedByName = CoreConstants.EMPTY;
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			String tidStr = String.valueOf(submittedDocument.getId());
			row.setId(tidStr);
			//row.addCell(tidStr);
			
			row.addCell(submittedDocument.getName());
			row.addCell(submittedByName);
			row.addCell(submittedDocument.getEnd() == null ? CoreConstants.EMPTY :
				new IWTimestamp(submittedDocument.getEnd()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)
			);
			
			row.addCell(new StringBuilder("<img class=\"downloadCaseAsPdfStyle\" src=\"").append(pdfUri).append("\" onclick=\"downloadCaseDocument(event, '").append(tidStr).append("');\" />").toString());
			
			if (rightsChanger) {
				addRightsChangerCell(row, processInstanceId, tidStr, null, true);
			}
		}
		
		try {
			return rows.getDocument();
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	private Document getEmailsListDocument(Collection<TaskInstance> processEmails, Long processInstanceId, boolean rightsChanger) {
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

		int size = processEmails.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		RolesManager rolesManager = getBpmFactory().getRolesManager();
		
		VariablesHandler variablesHandler = getVariablesHandler();

		for (TaskInstance email : processEmails) {
			
			try {
				Permission permission = getTaskSubmitPermission(true, email);
				rolesManager.checkPermission(permission);
				
			} catch (BPMAccessControlException e) {
				continue;
			}
			
			Map<String, Object> vars = variablesHandler.populateVariables(email.getId());
			
			String subject = (String)vars.get("string:subject");
			String fromPersonal = (String)vars.get("string:fromPersonal");
			String fromAddress = (String)vars.get("string:fromAddress");
			
			String fromStr = fromPersonal;
			
			if(fromAddress != null) {
				
				if(fromStr == null) {
					fromStr = fromAddress;
				} else {
					fromStr = new StringBuilder(fromStr).append(" (").append(fromAddress).append(")").toString();
				}
			}
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			String tidStr = String.valueOf(email.getId());
			row.setId(tidStr);
			
			row.addCell(subject);
			row.addCell(fromStr == null ? CoreConstants.EMPTY : fromStr);
			row.addCell(email.getEnd() == null ? CoreConstants.EMPTY :
				new IWTimestamp(email.getEnd()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)
			);
			
			if (rightsChanger) {
				addRightsChangerCell(row, processInstanceId, tidStr, null, true);
			}
		}
		
		try {
			return rows.getDocument();
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}

	public Document getProcessDocumentsList(ProcessArtifactsParamsBean params) {
		Long processInstanceId = params.getPiId();
		
		if (processInstanceId == null) {
			ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
			rows.setTotal(0);
			rows.setPage(0);
			
			try {
				return rows.getDocument();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Collection<TaskInstance> processDocuments = getProcessArtifactsProvider().getSubmittedTaskInstances(processInstanceId);
		
		return getDocumentsListDocument(processDocuments, processInstanceId, params.isRightsChanger());
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
			BPMUser bpmUsr = getBpmFactory().getBpmUserFactory().getCurrentBPMUser();
			Integer loggedInUserIdInt = bpmUsr.getIdToUse();
			String loggedInUserId = loggedInUserIdInt == null ? null : String.valueOf(loggedInUserIdInt);
			IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
			String youLocalized = iwrb.getLocalizedString("cases_bpm.case_assigned_to_you", "You");
			String noOneLocalized = iwrb.getLocalizedString("cases_bpm.case_assigned_to_no_one", "No one");
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
					
					if(loggedInUserId != null && taskInstance.getActorId().equals(loggedInUserId)) {
						disableSelection = false;
						assignedToName = youLocalized;
						
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
					
					assignedToName = noOneLocalized;
				}
				
//				String status = getTaskStatus(iwrb, taskInstance);
				
				ProcessArtifactsListRow row = new ProcessArtifactsListRow();
				rows.addRow(row);
				
				String tidStr = String.valueOf(taskInstance.getId());
				row.setId(tidStr);
				//row.addCell(tidStr);
				row.addCell(taskInstance.getName());
				row.addCell(taskInstance.getCreate() == null ? CoreConstants.EMPTY :
							new IWTimestamp(taskInstance.getCreate()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)
				);
				row.addCell(assignedToName);
//				row.addCell(status);

				if(disableSelection) {
					
					row.setStyleClass("disabledSelection");
					row.setDisabledSelection(disableSelection);
				}
				
				if (params.isRightsChanger()) {
					addRightsChangerCell(row, processInstanceId, tidStr, null, true);
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
	
	private void addRightsChangerCell(ProcessArtifactsListRow row, Long processInstanceId, String taskInstanceId, Integer variableIdentifier, boolean setSameRightsForAttachments) {		
		String id = new StringBuilder("idPrefImg").append(taskInstanceId).toString();
		IWBundle bundle = IWMainApplication.getDefaultIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		StringBuilder image = new StringBuilder("<img id=\"").append(id).append("\" class=\"caseProcessResourceAccessRightsStyle\" src=\"");
		image.append(bundle.getVirtualPathWithFileNameString("images/preferences.png")).append("\" onclick=\"changeAccessRightsForBpmRelatedResource(event, ");
		if (processInstanceId == null) {
			image.append("null");
		}
		else {
			image.append("'").append(processInstanceId).append("'");
		}
		image.append(", '").append(taskInstanceId).append("', '").append(id).append("', ");
		if (variableIdentifier == null) {
			image.append("null");
		}
		else {
			image.append("'").append(variableIdentifier).append("'");
		}
		image.append(", ").append(setSameRightsForAttachments).append(");\" />");
		row.addCell(image.toString());
	}
	
	public Document getTaskAttachments(ProcessArtifactsParamsBean params) {
		
//		TODO: check permission to view task variables
		
		Long taskInstanceId = params.getTaskId();
		
		if(taskInstanceId == null)
			return null;
		
		List<BinaryVariable> binaryVariables = getProcessArtifactsProvider().getTaskAttachments(taskInstanceId);
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

		int size = binaryVariables.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		String kiloBytesStr = "KB";
		for (BinaryVariable binaryVariable : binaryVariables) {
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			String tidStr = String.valueOf(binaryVariable.getHash());
			row.setId(tidStr);
			String description = binaryVariable.getDescription();
			if(description != null && description.length() > 0) {
				row.addCell(description);
			} else {
				row.addCell(binaryVariable.getFileName());
			}
			
			Long fileSize = binaryVariable.getContentLength();
			if (fileSize == null) {
				row.addCell(new StringBuilder().append(0).append(CoreConstants.SPACE).append(kiloBytesStr).toString());
			}
			else {
				long kiloBytes = fileSize / 1024;
				long bytes = fileSize % 1024;
				row.addCell(new StringBuilder().append(kiloBytes).append(CoreConstants.DOT).append(bytes).append(CoreConstants.SPACE).append(kiloBytesStr).toString());
			}
			
			if (params.isRightsChanger()) {
				addRightsChangerCell(row, params.getPiId(), tidStr, binaryVariable.getHash(), false);
			}
		}
		
		try {
			return rows.getDocument();
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	public Document getEmailAttachments(ProcessArtifactsParamsBean params) {
		if (params == null) {
			return null;
		}
		
		return getTaskAttachments(params);
	}
	
	public Document getProcessEmailsList(ProcessArtifactsParamsBean params) {
		
		Long processInstanceId = params.getPiId();
		
		if(processInstanceId == null)
			return null;
		
		Collection<TaskInstance> processEmails = getProcessArtifactsProvider().getAttachedEmailsTaskInstances(processInstanceId);
		
		if(processEmails == null || processEmails.isEmpty()) {
			
			try {
			
				ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
				rows.setTotal(0);
				rows.setPage(0);
				
				return rows.getDocument();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Exception while parsing rows", e);
				return null;
			}
			
		} else		
			return getEmailsListDocument(processEmails, processInstanceId, params.isRightsChanger());
	}
	
	private List<User> getPeopleConnectedToProcess(Long processInstanceId) {
		if (processInstanceId == null) {
			return null;
		}
		
		Collection<User> users = null;
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		try {
			ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
			users = getBpmFactory().getRolesManager().getAllUsersForRoles(null, processInstance);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
		
		if (users == null || users.isEmpty()) {
			return null;
		}
		
		List<User> connectedPeople = new ArrayList<User>(users);
		try {
			Collections.sort(connectedPeople, new UserComparator(CoreUtil.getIWContext().getCurrentLocale()));
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return connectedPeople;
	}
	
	@SuppressWarnings("unchecked")
	public Document getProcessContactsList(ProcessArtifactsParamsBean params) {
		if (params == null) {
			return null;
		}
		
		Long processInstanceId = params.getPiId();
		if (processInstanceId == null) {
			return null;
		}
		
		Collection<User> peopleConnectedToProcess = getPeopleConnectedToProcess(processInstanceId);
		List<User> uniqueUsers = new ArrayList<User>();
		if (peopleConnectedToProcess != null) {
			for(User user: peopleConnectedToProcess) {
				if (!uniqueUsers.contains(user)) {
					uniqueUsers.add(user);
				}
			}
		}
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		rows.setTotal(uniqueUsers.size());
		rows.setPage(uniqueUsers.isEmpty() ? 0 : 1);
		
		for(User user: uniqueUsers) {
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			
			row.addCell(BPMUser.bpmUserIdentifier.equals(user.getMiddleName()) ? user.getFirstName() : user.getName());
			row.addCell(getUserEmails(user.getEmails()));
			row.addCell(getUserPhones(user.getPhones()));
			row.addCell(getUserAddress(user));
		}
		
		try {
			return rows.getDocument();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception parsing rows for contacts", e);
		}
		
		return null;
	}
	
	private String getUserAddress(User user) {
		UserBusiness userBusiness = null;
		try {
			userBusiness = (UserBusiness) IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), UserBusiness.class);
		} catch (IBOLookupException e) {
			logger.log(Level.SEVERE, "Can not get instance of " + UserBusiness.class.getSimpleName(), e);
		}
		if (userBusiness == null) {
			return CoreConstants.MINUS;
		}
		
		Address mainAddress = null;
		try {
			mainAddress = userBusiness.getUsersMainAddress(Integer.valueOf(user.getId()));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (EJBException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (mainAddress == null) {
			return CoreConstants.MINUS;
		}
		
		StringBuilder userAddress = new StringBuilder();
		String streetAddress = mainAddress.getStreetAddress();
		if (streetAddress != null && !CoreConstants.EMPTY.equals(streetAddress)) {
			userAddress.append(streetAddress);
		}
		
		String postalAddress = mainAddress.getPostalAddress();
		if (postalAddress != null && !CoreConstants.EMPTY.equals(postalAddress)) {
			userAddress.append(postalAddress).append(CoreConstants.SPACE);
		}
		
		Country country = mainAddress.getCountry();
		if (country != null) {
			String countryName = country.getName();
			if (countryName != null && !CoreConstants.EMPTY.equals(countryName)) {
				userAddress.append(countryName);
			}
		}
		
		return userAddress.toString();
	}
	
	private String getUserPhones(Collection<Phone> phones) {
		if (phones == null || phones.isEmpty()) {
			return CoreConstants.MINUS;
		}
		
		int phonesCounter = 0;
		String phoneNumber = null;
		StringBuilder userPhones = new StringBuilder();
		for(Phone phone: phones) {
			phoneNumber = phone.getNumber();
			userPhones.append(phone == null ? CoreConstants.MINUS : phoneNumber);
			if ((phonesCounter + 1) < phones.size()) {
				userPhones.append(CoreConstants.SEMICOLON);
			}
			phonesCounter++;
		}
		
		String result = userPhones.toString();
		return result.equals(CoreConstants.EMPTY) ? CoreConstants.MINUS : result;
	}
	
	private String getUserEmails(Collection<Email> emails) {
		if (emails == null || emails.isEmpty()) {
			return CoreConstants.MINUS;
		}
		
		int emailsCounter = 0;
		String emailValue = null;
		StringBuilder userEmails = new StringBuilder();
		for(Email email: emails) {
			emailValue = email.getEmailAddressMailtoFormatted();
			userEmails.append(emailValue == null ? CoreConstants.MINUS : emailValue);
			if ((emailsCounter + 1) < emails.size()) {
				userEmails.append(CoreConstants.SEMICOLON);
			}
			emailsCounter++;
		}
		
		String result = userEmails.toString();
		return result.equals(CoreConstants.EMPTY) ? CoreConstants.MINUS : result;
	}
	
	protected String getTaskStatus(IWResourceBundle iwrb, TaskInstance taskInstance) {
		
		if(taskInstance.hasEnded())
			return iwrb.getLocalizedString("ended", "Ended");
		if(taskInstance.getStart() != null)
			return iwrb.getLocalizedString("in_progess", "In progress");
		
		return iwrb.getLocalizedString("not_started", "Not started");
	}
	
	public org.jdom.Document getViewDisplay(Long taskInstanceId) {
		try {
			return getBuilderService().getRenderedComponent(IWContext.getIWContext(FacesContext.getCurrentInstance()), getViewInUIComponent(taskInstanceId), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public UIComponent getViewInUIComponent(Long taskInstanceId) throws Exception {
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			long processDefinitionId = ctx.getTaskInstance(taskInstanceId).getProcessInstance().getProcessDefinition().getId();
			return getBpmFactory().getProcessManager(processDefinitionId).getTaskInstance(taskInstanceId).loadView().getViewForDisplay();
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
	
	public enum Permisison {
		EDIT_PERMISSION { public void check() throws AccessControlException { return; } };
			
		public abstract void check() throws AccessControlException;
	}
	
	public boolean hasUserRolesEditorRights(Long processInstanceId) {
		if (processInstanceId == null) {
			return false;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return false;
		}
		
		User currentUser = null;
		try {
			currentUser = iwc.getCurrentUser();
		} catch(Exception e) {
			e.printStackTrace();
		}
		if (currentUser == null) {
			return false;
		}
		
		try {
			Permisison.EDIT_PERMISSION.check();
			return true;
		} catch(AccessControlException e) {
			logger.log(Level.SEVERE, "Current user does not have rights to change access rights to resources of process: " + processInstanceId, e);
		}
		return false;
	}

	public void setAccessRightsForProcessResource(String roleName, Long taskInstanceId, String fileHashValue, boolean hasReadAccess, boolean setSameRightsForAttachments) {
		
		if (roleName == null || CoreConstants.EMPTY.equals(roleName) || taskInstanceId == null) {
			logger.log(Level.WARNING, "setAccessRightsForProcessResource called, but insufficient parameters provided. Got: roleName="+roleName+", taskInstanceId="+taskInstanceId);
			return;
		}
		
		getBpmFactory().getRolesManager().setTaskRolePermissionsTIScope(
				new Role(roleName, hasReadAccess ? Access.read : null),
				taskInstanceId, setSameRightsForAttachments, fileHashValue
		);
	}
	
//	TODO: processInstanceId is not needed (anywhere regarding permissions)
	public org.jdom.Document getAccessRightsSetterBox(Long processInstanceId, Long taskInstanceId, String fileHashValue, boolean setSameRightsForAttachments) {
		if (taskInstanceId == null) {
			return null;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		BuilderService builder = getBuilderService();
		IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
		if (builder == null) {
			return null;
		}
		Layer container = new Layer();

		List<Role> roles = getBpmFactory().getRolesManager().getRolesPermissionsForTaskInstance(taskInstanceId, CoreConstants.EMPTY.equals(fileHashValue) ? null : fileHashValue);
		
		if (roles == null || roles.isEmpty()) {
			container.add(new Heading3(iwrb.getLocalizedString("no_roles_to_set_permissions", "There are no roles to set access rights")));
		}
		else {
			container.add(new Heading3(iwrb.getLocalizedString("set_access_rights", "Set access rights")));
			
			Layer checkBoxes = new Layer();
			container.add(checkBoxes);
			Table2 table = new Table2();
			checkBoxes.add(table);
			TableHeaderRowGroup headerRowGroup = table.createHeaderRowGroup();
			TableRow headerRow = headerRowGroup.createRow();
			//	Role name
			TableHeaderCell headerCell = headerRow.createHeaderCell();
			headerCell.add(new Text(iwrb.getLocalizedString("role_name", "Role name")));
			//	Permission to read
			headerCell = headerRow.createHeaderCell();
			headerCell.add(new Text(iwrb.getLocalizedString("allow_disallow_to_read", "Allow/disallow to read")));
			//	Set same rights for attachments
			if (setSameRightsForAttachments) {
				headerCell = headerRow.createHeaderCell();
				headerCell.add(new Text(iwrb.getLocalizedString("set_same_permission_for_attachements", "Set same access rights to attachments")));
			}
			
			String roleName = null;
			TableBodyRowGroup bodyRowGroup = table.createBodyRowGroup();
			
			for (Role role : roles) {
				roleName = role.getRoleName();
				
				TableRow bodyRow = bodyRowGroup.createRow();
				TableCell2 cell = bodyRow.createCell();
				cell.add(new Text(iwrb.getLocalizedString(roleName, roleName)));
				
				CheckBox sameRigthsSetter = null;
				if (setSameRightsForAttachments) {
					sameRigthsSetter = new CheckBox();
				}
				
				CheckBox box = new CheckBox(roleName);
				box.setChecked(role.getAccesses() != null && role.getAccesses().contains(Access.read));
				StringBuilder action = new StringBuilder("setAccessRightsForBpmRelatedResource('").append(box.getId()).append("', ");
				if (processInstanceId == null) {
					action.append("null");
				}
				else {
					action.append("'").append(processInstanceId).append("'");
				}
				action.append(", '").append(taskInstanceId).append("', ");
				if (fileHashValue == null) {
					action.append("null");
				}
				else {
					action.append("'").append(fileHashValue).append("'");
				}
				action.append(", ");
				if (sameRigthsSetter == null) {
					action.append("null");
				}
				else {
					action.append("'").append(sameRigthsSetter.getId()).append("'");
				}
				action.append(");");
				box.setOnClick(action.toString());
				cell = bodyRow.createCell();
				cell.add(box);
				
				if (setSameRightsForAttachments) {
					cell = bodyRow.createCell();
					sameRigthsSetter.setChecked(true);
					cell.add(sameRigthsSetter);
				}
			}
		}
		
		Layer buttonsContainer = new Layer();
		container.add(buttonsContainer);
		Link closeLink = new Link(iwrb.getLocalizedString("close", "Close"));
		container.add(closeLink);
		closeLink.setURL("javascript:void(0);");
		closeLink.setOnClick("closeAccessRightsSetterBox();");
		
		return builder.getRenderedComponent(iwc, container, false);
	}
	
	/*
	@Transactional(readOnly = true)
	protected boolean isAllowedToReadTaskResource(ProcessRole role, Long taskInstanceId) {
		if (role == null || taskInstanceId == null) {
			return false;
		}
		
		List<ActorPermissions> permissions = null;
		try {
			permissions = role.getActorPermissions();
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Unable to get permissions for taskInstanceId: " + taskInstanceId, e);
		}
		try {
			if (permissions == null || permissions.isEmpty()) {	//	FIXME:	always throws exception
				return false;
			}
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Unable to get permissions for taskInstanceId: " + taskInstanceId, e);
			return false;
		}
		
		for(ActorPermissions permission: permissions) {
			try {
				if (taskInstanceId.equals(permission.getTaskInstanceId())) {
					return permission.getReadPermission();
				}
			} catch(Exception e) {
				logger.log(Level.SEVERE, "Unable to check permission for taskInstanceId: " + taskInstanceId, e);
			}
		}
		
		return false;
	}
	*/
	
	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
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
	
	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	@Autowired
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}

	public ProcessArtifactsProvider getProcessArtifactsProvider() {
		return processArtifactsProvider;
	}

	@Autowired
	public void setProcessArtifactsProvider(
			ProcessArtifactsProvider processArtifactsProvider) {
		this.processArtifactsProvider = processArtifactsProvider;
	}
}