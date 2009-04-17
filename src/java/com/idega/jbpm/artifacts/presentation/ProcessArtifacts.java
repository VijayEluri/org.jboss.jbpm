package com.idega.jbpm.artifacts.presentation;

import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jboss.jbpm.IWBundleStarter;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.idega.block.email.presentation.EmailSender;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.NotLoggedOnException;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.Phone;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.exe.BPMDocument;
import com.idega.jbpm.exe.BPMEmailDocument;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRow;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRows;
import com.idega.jbpm.rights.Right;
import com.idega.jbpm.signing.SigningHandler;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.presentation.Image;
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
import com.idega.presentation.ui.GenericButton;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.FileUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

/**
 * TODO: access control checks shouldn't be done here at all - remake!
 * TODO: All this class is too big and total mess almost. Refactor 
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.109 $ Last modified: $Date: 2009/04/17 11:24:09 $ by $Author: valdas $
 */
@Scope("singleton")
@Service(ProcessArtifacts.SPRING_BEAN_NAME_PROCESS_ARTIFACTS)
public class ProcessArtifacts {
	
	public static final String SPRING_BEAN_NAME_PROCESS_ARTIFACTS = "BPMProcessAssets";
	
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMContext idegaJbpmContext;
	@Autowired
	private VariablesHandler variablesHandler;
	@Autowired
	private PermissionsFactory permissionsFactory;
	@Autowired
	private BuilderLogicWrapper builderLogicWrapper;
	@Autowired(required = false)
	private SigningHandler signingHandler;
	
	private Logger logger = Logger.getLogger(ProcessArtifacts.class.getName());
	
	private GridEntriesBean getDocumentsListDocument(IWContext iwc,
	        Collection<BPMDocument> processDocuments, Long processInstanceId,
	        ProcessArtifactsParamsBean params) {
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		
		int size = processDocuments.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		Locale userLocale = iwc.getCurrentLocale();
		IWBundle bundle = iwc.getIWMainApplication().getBundle(
		    IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		
		String message = iwrb.getLocalizedString("generating", "Generating...");
		String pdfUri = bundle
		        .getVirtualPathWithFileNameString("images/pdf.gif");
		String signPdfUri = bundle
		        .getVirtualPathWithFileNameString("images/pdf_sign.jpeg");
		String errorMessage = iwrb.getLocalizedString("error_generating_pdf",
		    "Sorry, unable to generate PDF file from selected document");
		
		GridEntriesBean entries = new GridEntriesBean(processInstanceId);
		
		for (BPMDocument submittedDocument : processDocuments) {
			
			Long taskInstanceId = submittedDocument.getTaskInstanceId();
			ProcessInstanceW piw = getBpmFactory()
			        .getProcessManagerByTaskInstanceId(taskInstanceId)
			        .getTaskInstance(taskInstanceId).getProcessInstanceW();
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			
			String rowId = taskInstanceId.toString();
			row.setId(rowId);
			
			row.addCell(submittedDocument.getDocumentName());
			row.addCell(submittedDocument.getSubmittedByName());
			row
			        .addCell(submittedDocument.getEndDate() == null ? CoreConstants.EMPTY
			                : new IWTimestamp(submittedDocument.getEndDate())
			                        .getLocaleDateAndTime(userLocale,
			                            IWTimestamp.SHORT, IWTimestamp.SHORT));
			row.setDateCellIndex(row.getCells().size() - 1);
			
			if (params.getDownloadDocument()) {
				row
				        .addCell(new StringBuilder(
				                "<img class=\"downloadCaseAsPdfStyle\" src=\"")
				                .append(pdfUri)
				                .append(
				                    "\" onclick=\"CasesBPMAssets.downloadCaseDocument(event, '")
				                .append(taskInstanceId).append("');\" />")
				                .toString());
			}
			
			if (params.getAllowPDFSigning()) {
				if (hasDocumentGeneratedPDF(taskInstanceId)
				        || !submittedDocument.isSignable()) {
					// Sign icon will be in attachments' list (if not signed)
					row.addCell(CoreConstants.EMPTY);
				} else if (getSigningHandler() != null && !piw.hasEnded()) {
					row
					        .addCell(new StringBuilder(
					                "<img class=\"signGeneratedFormToPdfStyle\" src=\"")
					                .append(signPdfUri)
					                .append(
					                    "\" onclick=\"CasesBPMAssets.signCaseDocument")
					                .append(
					                    getJavaScriptActionForPDF(iwrb,
					                        taskInstanceId, null, message,
					                        errorMessage)).append("\" />")
					                .toString());
				}
			}
			
			// FIXME: don't use client side stuff for validating security constraints, check if
			// rights changer in this method.
			if (params.isRightsChanger()) {
				addRightsChangerCell(row, processInstanceId, taskInstanceId,
				    null, null, true);
			}
			
			if (!submittedDocument.isHasViewUI()) {
				entries.setRowHasViewUI(rowId, false);
			}
		}
		
		try {
			entries.setGridEntries(rows.getDocument());
			return entries;
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	private boolean hasDocumentGeneratedPDF(Long taskInstanceId) {
		
		try {
			List<BinaryVariable> binaryVariables = getBpmFactory()
			        .getProcessManagerByTaskInstanceId(taskInstanceId)
			        .getTaskInstance(taskInstanceId).getAttachments();
			
			if (ListUtil.isEmpty(binaryVariables)) {
				return false;
			}
			
			String expectedName = getFileNameForGeneratedPDFFromTaskInstance(taskInstanceId
			        .toString());
			for (BinaryVariable bv : binaryVariables) {
				if (expectedName.equals(bv.getFileName())) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public String getFileNameForGeneratedPDFFromTaskInstance(
	        String taskInstanceId) {
		return new StringBuilder("Document_").append(taskInstanceId).append(
		    ".pdf").toString();
	}
	
	private Document getEmailsListDocument(Collection<BPMEmailDocument> processEmails, Long processInstanceId, boolean rightsChanger, User currentUser) {
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		
		int size = processEmails.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		
		String userEmail = null;
		if (currentUser != null) {
			Email email = null;
			try {
				email = getUserBusiness().getUsersMainEmail(currentUser);
			} catch (Exception e) {}
			userEmail = email == null ? null : email.getEmailAddress();
		}

		String sendEmailComponent = getBuilderLogicWrapper().getBuilderService(iwc).getUriToObject(EmailSender.class, Arrays.asList(
			new AdvancedProperty("iframe", Boolean.TRUE.toString()),
			new AdvancedProperty(EmailSender.FROM_PARAMETER, userEmail)
		));
		
		for (BPMEmailDocument email : processEmails) {
			
			String plainFrom = email.getFromAddress();
			String fromStr = plainFrom;
			
			if (email.getFromAddress() != null) {
				
				if (fromStr == null) {
					fromStr = email.getFromAddress();
				} else {
					fromStr = new StringBuilder(fromStr).append(" (").append(
					    email.getFromAddress()).append(")").toString();
				}
			}
			
			Long taskInstanceId = email.getTaskInstanceId();
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			row.setId(taskInstanceId.toString());
			
			row.addCell(email.getSubject());
			row.addCell(getEmailCell(sendEmailComponent, plainFrom, fromStr));
			row.addCell(email.getEndDate() == null ? CoreConstants.EMPTY
			        : new IWTimestamp(email.getEndDate()).getLocaleDateAndTime(
			            iwc.getCurrentLocale(), IWTimestamp.SHORT,
			            IWTimestamp.SHORT));
			row.setDateCellIndex(row.getCells().size() - 1);
			
			if (rightsChanger) {
				addRightsChangerCell(row, processInstanceId, taskInstanceId,
				    null, null, true);
			}
		}
		
		try {
			return rows.getDocument();
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	private String getEmailCell(String componentUri, String emailAddress, String valueToShow) {
		if (StringUtil.isEmpty(emailAddress)) {
			return CoreConstants.EMPTY;
		}
		
		componentUri = new StringBuilder(componentUri).append("&").append(EmailSender.RECIPIENT_TO_PARAMETER).append("=").append(emailAddress).toString();
		return new StringBuilder("<a class=\"emailSenderLightboxinBPMCasesStyle\" href=\"").append(componentUri).append("\" ")
			.append("onclick=\"CasesBPMAssets.showSendEmailWindow(event);\">").append(valueToShow).append("</a>").toString();
	}
	
	public GridEntriesBean getProcessDocumentsList(
	        ProcessArtifactsParamsBean params) {
		Long processInstanceId = params.getPiId();
		
		if (processInstanceId == null) {
			ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
			rows.setTotal(0);
			rows.setPage(0);
			
			try {
				GridEntriesBean entries = new GridEntriesBean();
				entries.setProcessInstanceId(processInstanceId);
				entries.setGridEntries(rows.getDocument());
				return entries;
			} catch (Exception e) {
				logger.log(Level.SEVERE,
				    "Exception while creating empty grid entries", e);
			}
		}
		
		IWContext iwc = IWContext.getCurrentInstance();
		User loggedInUser = getBpmFactory().getBpmUserFactory()
		        .getCurrentBPMUser().getUserToUse();
		Locale userLocale = iwc.getCurrentLocale();
		
		Collection<BPMDocument> processDocuments = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId)
		        .getSubmittedDocumentsForUser(loggedInUser, userLocale);
		
		return getDocumentsListDocument(iwc, processDocuments,
		    processInstanceId, params);
	}
	
	public Document getProcessTasksList(ProcessArtifactsParamsBean params) {
		
		Long processInstanceId = params.getPiId();
		
		if (processInstanceId == null)
			return null;
		
		IWContext iwc = IWContext.getCurrentInstance();
		User loggedInUser = getBpmFactory().getBpmUserFactory()
		        .getCurrentBPMUser().getUserToUse();
		Locale userLocale = iwc.getCurrentLocale();
		
		IWBundle bundle = iwc.getIWMainApplication().getBundle(
		    IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		
		Collection<BPMDocument> tasksDocuments = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId).getTaskDocumentsForUser(
		            loggedInUser, userLocale);
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		
		int size = tasksDocuments.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		String noOneLocalized = iwrb.getLocalizedString(
		    "cases_bpm.case_assigned_to_no_one", "No one");
		String takeTaskImage = bundle
		        .getVirtualPathWithFileNameString("images/take_task.png");
		String takeTaskTitle = iwrb.getLocalizedString(
		    "cases_bpm.case_take_task", "Take task");
		boolean allowReAssignTask = false;
		
		for (BPMDocument taskDocument : tasksDocuments) {
			
			boolean disableSelection = false; // this is not used now, and implementation can be
			// different when we finally decide to use it
			
			Long taskInstanceId = taskDocument.getTaskInstanceId();
			
			final boolean addTaskAssigment;
			String assignedToName;
			
			if (StringUtil.isEmpty(taskDocument.getAssignedToName())) {
				
				addTaskAssigment = true; // Because is not assigned yet
				assignedToName = noOneLocalized;
				
			} else {
				
				addTaskAssigment = false;
				assignedToName = taskDocument.getAssignedToName();
			}
			
			if (addTaskAssigment || allowReAssignTask) {
				String imageId = new StringBuilder("id").append(taskInstanceId)
				        .append("_assignTask").toString();
				StringBuilder assignedToCell = new StringBuilder("<img src=\"")
				        .append(takeTaskImage).append("\" title=\"").append(
				            takeTaskTitle).append("\"");
				assignedToCell
				        .append(" id=\"")
				        .append(imageId)
				        .append("\"")
				        .append(
				            " onclick=\"CasesBPMAssets.takeCurrentProcessTask(event, '")
				        .append(taskInstanceId);
				assignedToCell.append("', '").append(imageId).append("', ")
				        .append(allowReAssignTask).append(");\" />");
				
				assignedToName = new StringBuilder(assignedToCell.toString())
				        .append(CoreConstants.SPACE).append(assignedToName)
				        .toString();
			}
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			
			row.setId(taskInstanceId.toString());
			
			row.addCell(taskDocument.getDocumentName());
			row
			        .addCell(taskDocument.getCreateDate() == null ? CoreConstants.EMPTY
			                : new IWTimestamp(taskDocument.getCreateDate())
			                        .getLocaleDateAndTime(userLocale,
			                            IWTimestamp.SHORT, IWTimestamp.SHORT));
			row.setDateCellIndex(row.getCells().size() - 1);
			// TODO commented for future use. 'Taken by' column isn't shown now
			// row.addCell(assignedToName);
			
			disableSelection = true;
			
			if (disableSelection) {
				
				row.setStyleClass("disabledSelection");
				row.setDisabledSelection(disableSelection);
			}
			
			if (params.isRightsChanger()) {
				addRightsChangerCell(row, processInstanceId, taskInstanceId,
				    null, null, true);
			}
		}
		
		if (ListUtil.isEmpty(rows.getRows())) {
			int cellsCount = 2;
			if (params.isRightsChanger()) {
				cellsCount++;
			}
			addMessageIfNoContentExists(rows, iwrb.getLocalizedString(
			    "no_tasks_available_currently",
			    "You currently don't have any tasks awaiting"), cellsCount);
		}
		
		try {
			return rows.getDocument();
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	private void addMessageIfNoContentExists(ProcessArtifactsListRows rows,
	        String message, int cellsCount) {
		rows.setTotal(1);
		rows.setPage(1);
		
		ProcessArtifactsListRow row = new ProcessArtifactsListRow();
		rows.addRow(row);
		
		row.setId("-1");
		
		row.addCell(message);
		cellsCount--;
		for (int i = 0; i < cellsCount; i++) {
			row.addCell(CoreConstants.EMPTY);
		}
		
		row.setStyleClass("disabledSelection");
		row.setDisabledSelection(true);
	}
	
	private void addRightsChangerCell(ProcessArtifactsListRow row,
	        Long processInstanceId, Long taskInstanceId,
	        Integer variableIdentifier, String userId,
	        boolean setSameRightsForAttachments) {
		
		final IWBundle bundle = IWMainApplication.getDefaultIWMainApplication()
		        .getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		final StringBuilder imageHtml = new StringBuilder(
		        "<img class=\"caseProcessResourceAccessRightsStyle\" src=\"");
		
		imageHtml
		        .append(
		            bundle
		                    .getVirtualPathWithFileNameString("images/preferences.png"))
		        .append("\" ondblclick=\"function() {}\"")
		        .append(
		            " onclick=\"CasesBPMAssets.showAccessRightsForBpmRelatedResourceChangeMenu(event, ")
		        .append(processInstanceId).append(", ").append(taskInstanceId)
		        .append(", this, ").append(variableIdentifier).append(", ")
		        .append(setSameRightsForAttachments).append(", ")
		        .append(userId).append(");\" />");
		
		row.addCell(imageHtml.toString());
	}
	
	public Document getTaskAttachments(ProcessArtifactsParamsBean params) {
		
		// TODO: check permission to view task variables
		
		Long taskInstanceId = params.getTaskId();
		
		if (taskInstanceId == null)
			return null;
		
		TaskInstanceW tiw = getBpmFactory().getProcessManagerByTaskInstanceId(
		    taskInstanceId).getTaskInstance(taskInstanceId);
		
		List<BinaryVariable> binaryVariables = tiw.getAttachments();
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		
		if (binaryVariables == null || binaryVariables.size() == 0) {
			return null; // This will result in 'closed' row in grid
		}
		
		int size = binaryVariables.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		IWContext iwc = CoreUtil.getIWContext();
		IWBundle bundle = iwc.getIWMainApplication().getBundle(
		    IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		
		String message = iwrb.getLocalizedString("signing", "Signing...");
		String image = bundle
		        .getVirtualPathWithFileNameString("images/pdf_sign.jpeg");
		String errorMessage = iwrb.getLocalizedString(
		    "unable_to_sign_attachment",
		    "Sorry, unable to sign selected attachment");
		
		for (BinaryVariable binaryVariable : binaryVariables) {
			
			if (binaryVariable.getHash() == null
			        || (binaryVariable.getHidden() != null && binaryVariable
			                .getHidden() == true))
				continue;
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			row.setId(binaryVariable.getHash().toString());
			
			String description = binaryVariable.getDescription();
			row.addCell(StringUtil.isEmpty(description) ? binaryVariable
			        .getFileName() : description);
			
			String fileName = binaryVariable.getFileName();
			row.addCell(new StringBuilder(
			        "<a href=\"javascript:void(0)\" rel=\"").append(fileName)
			        .append("\">").append(fileName).append("</a>").toString());
			
			Long fileSize = binaryVariable.getContentLength();
			row.addCell(FileUtil.getHumanReadableSize(fileSize == null ? Long
			        .valueOf(0) : fileSize));
			
			if (params.getAllowPDFSigning() && getSigningHandler() != null
			        && tiw.isSignable() && binaryVariable.isSignable()
			        && !tiw.getProcessInstanceW().hasEnded()) {
				if (isPDFFile(binaryVariable.getFileName())
				        && (binaryVariable.getSigned() == null || !binaryVariable
				                .getSigned())) {
					row
					        .addCell(new StringBuilder("<img src=\"")
					                .append(image)
					                .append(
					                    "\" onclick=\"CasesBPMAssets.signCaseAttachment")
					                .append(
					                    getJavaScriptActionForPDF(iwrb,
					                        taskInstanceId, binaryVariable
					                                .getHash().toString(),
					                        message, errorMessage)).append(
					                    "\" />").toString());
				} else {
					row.addCell(CoreConstants.EMPTY);
				}
			}
			if (params.isRightsChanger()) {
				addRightsChangerCell(row, params.getPiId(), taskInstanceId,
				    binaryVariable.getHash(), null, false);
			}
		}
		
		try {
			if (!ListUtil.isEmpty(rows.getRows())) {
				
				return rows.getDocument();
			} else {
				return null;
			}
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	private String getJavaScriptActionForPDF(IWResourceBundle iwrb,
	        Long taskInstanceId, String hashValue, String message,
	        String errorMessage) {
		hashValue = StringUtil.isEmpty(hashValue) ? CoreConstants.MINUS
		        : hashValue;
		
		return new StringBuilder("(event, '").append(taskInstanceId).append(
		    "', '").append(hashValue).append("','").append(message).append(
		    "', '").append(
		    iwrb.getLocalizedString("document_signing_form",
		        "Document signing form")).append("', '")
		        .append(
		            iwrb.getLocalizedString("close_signing_form",
		                "Close signing form")).append("', '").append(
		            errorMessage).append("');").toString();
	}
	
	private boolean isPDFFile(String fileName) {
		if (StringUtil.isEmpty(fileName)
		        || fileName.indexOf(CoreConstants.DOT) == -1) {
			return false;
		}
		
		String fileNameEnd = fileName.substring(fileName
		        .indexOf(CoreConstants.DOT));
		return StringUtil.isEmpty(fileNameEnd) ? false : fileNameEnd
		        .equalsIgnoreCase(".pdf");
	}
	
	public Document getEmailAttachments(ProcessArtifactsParamsBean params) {
		if (params == null) {
			return null;
		}
		
		return getTaskAttachments(params);
	}
	
	public Document getProcessEmailsList(ProcessArtifactsParamsBean params) {
		
		Long processInstanceId = params.getPiId();
		
		if (processInstanceId == null)
			return null;
		
		User loggedInUser = getBpmFactory().getBpmUserFactory()
		        .getCurrentBPMUser().getUserToUse();
		
		Collection<BPMEmailDocument> processEmails = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId).getAttachedEmails(
		            loggedInUser);
		
		if (ListUtil.isEmpty(processEmails)) {
			
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
			return getEmailsListDocument(processEmails, processInstanceId, params.isRightsChanger(), loggedInUser);
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
		
		ProcessInstanceW piw = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId);
		
		Collection<User> peopleConnectedToProcess = piw
		        .getUsersConnectedToProcess();
		List<User> uniqueUsers = new ArrayList<User>();
		if (peopleConnectedToProcess != null) {
			for (User user : peopleConnectedToProcess) {
				if (!uniqueUsers.contains(user)) {
					uniqueUsers.add(user);
				}
			}
		}
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		rows.setTotal(uniqueUsers.size());
		rows.setPage(uniqueUsers.isEmpty() ? 0 : 1);
		
		String systemEmail = null;
		try {
			systemEmail = IWMainApplication.getDefaultIWApplicationContext()
			        .getApplicationSettings().getProperty(
			            CoreConstants.PROP_SYSTEM_ACCOUNT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String processIdentifier = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId).getProcessIdentifier();
		
		IWBundle bundle = IWMainApplication.getDefaultIWMainApplication()
		        .getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		for (User user : uniqueUsers) {
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			
			row.addCell(user.getName());
			row.addCell(getUserEmails(user.getEmails(), processIdentifier,
			    systemEmail));
			row.addCell(new StringBuilder(getUserPhones(user.getPhones()))
			        .append(getUserImage(bundle, user)).toString());
			
			if (params.isRightsChanger()) {
				addRightsChangerCell(row, processInstanceId, null, null, user
				        .getPrimaryKey().toString(), false);
			}
		}
		
		try {
			return rows.getDocument();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception parsing rows for contacts", e);
		}
		
		return null;
	}
	
	private String getUserImage(IWBundle bundle, User user) {
		String pictureUri = null;
		UserBusiness userBusiness = getUserBusiness();
		Image image = userBusiness.getUserImage(user);
		if (image == null) {
			// Default image
			boolean male = true;
			try {
				male = getUserBusiness().isMale(user.getGenderID());
			} catch (Exception e) {
				male = true;
			}
			pictureUri = new StringBuilder(bundle
			        .getVirtualPathWithFileNameString("images/")).append(
			    male ? "user_male" : "user_female").append(".png").toString();
		} else {
			pictureUri = image.getMediaURL(IWMainApplication
			        .getDefaultIWApplicationContext());
		}
		
		return new StringBuilder(
		        "<img class=\"userProfilePictureInCasesList\" src=\"").append(
		    pictureUri).append("\" />").toString();
	}
	
	private boolean canAddValueToCell(String value) {
		if (value == null) {
			return false;
		}
		
		if (CoreConstants.EMPTY.equals(value) || "null".equals(value)) {
			return false;
		}
		
		return true;
	}
	
	private String getUserPhones(Collection<Phone> phones) {
		if (phones == null || phones.isEmpty()) {
			return CoreConstants.MINUS;
		}
		
		int phonesCounter = 0;
		String phoneNumber = null;
		StringBuilder userPhones = new StringBuilder();
		boolean addSemicolon = false;
		for (Phone phone : phones) {
			phoneNumber = phone.getNumber();
			addSemicolon = false;
			
			if (!canAddValueToCell(phoneNumber)) {
				userPhones.append(CoreConstants.EMPTY);
			} else {
				addSemicolon = true;
				userPhones.append(phoneNumber);
			}
			if ((phonesCounter + 1) < phones.size() && addSemicolon) {
				userPhones.append(CoreConstants.SEMICOLON).append(
				    CoreConstants.SPACE);
			}
			
			phonesCounter++;
		}
		
		String result = userPhones.toString();
		return result.equals(CoreConstants.EMPTY) ? CoreConstants.MINUS
		        : result;
	}
	
	private String getUserEmails(Collection<Email> emails,
	        String caseIdentifier, String systemEmail) {
		if (emails == null || emails.isEmpty()) {
			return CoreConstants.MINUS;
		}
		
		int emailsCounter = 0;
		String emailValue = null;
		StringBuilder userEmails = new StringBuilder();
		boolean addSemicolon = false;
		for (Email email : emails) {
			emailValue = email.getEmailAddress();
			addSemicolon = false;
			
			if (!canAddValueToCell(emailValue)) {
				userEmails.append(CoreConstants.EMPTY);
			} else {
				addSemicolon = true;
				userEmails.append(getContactEmailFormatted(emailValue,
				    caseIdentifier, systemEmail));
			}
			if ((emailsCounter + 1) < emails.size() && addSemicolon) {
				userEmails.append(CoreConstants.SPACE);
			}
			
			emailsCounter++;
		}
		
		String result = userEmails.toString();
		return result.equals(CoreConstants.EMPTY) ? CoreConstants.MINUS
		        : result;
	}
	
	private String getContactEmailFormatted(String emailAddress,
	        String caseIdentifier, String systemEmail) {
		StringBuffer link = new StringBuffer("<a href=\"mailto:")
		        .append(emailAddress);
		
		boolean firstParamAdded = false;
		if (caseIdentifier != null) {
			link.append("?subject=(").append(caseIdentifier).append(")");
			firstParamAdded = true;
		}
		if (systemEmail != null) {
			if (firstParamAdded) {
				link.append("&");
			} else {
				link.append("?");
			}
			link.append("cc=").append(systemEmail);
		}
		
		link.append("\">").append(emailAddress).append("</a>");
		return link.toString();
	}
	
	protected String getTaskStatus(IWResourceBundle iwrb,
	        TaskInstance taskInstance) {
		
		if (taskInstance.hasEnded())
			return iwrb.getLocalizedString("ended", "Ended");
		if (taskInstance.getStart() != null)
			return iwrb.getLocalizedString("in_progess", "In progress");
		
		return iwrb.getLocalizedString("not_started", "Not started");
	}
	
	public org.jdom.Document getViewDisplay(Long taskInstanceId) {
		try {
			return getBuilderService().getRenderedComponent(
			    IWContext.getIWContext(FacesContext.getCurrentInstance()),
			    getViewInUIComponent(taskInstanceId), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public UIComponent getViewInUIComponent(Long taskInstanceId)
	        throws Exception {
		return getViewInUIComponent(taskInstanceId, false);
	}
	
	public UIComponent getViewInUIComponent(Long taskInstanceId,
	        boolean pdfViewer) throws Exception {
		return getBpmFactory()
		        .getProcessManagerByTaskInstanceId(taskInstanceId)
		        .getTaskInstance(taskInstanceId).loadView().getViewForDisplay(
		            pdfViewer);
	}
	
	protected BuilderService getBuilderService() {
		
		try {
			return BuilderServiceFactory.getBuilderService(IWMainApplication
			        .getDefaultIWApplicationContext());
		} catch (RemoteException e) {
			throw new RuntimeException(
			        "Error while retrieving builder service", e);
		}
	}
	
	public boolean hasUserRolesEditorRights(Long processInstanceId) {
		
		if (processInstanceId == null)
			return false;
		
		try {
			Permission perm = getPermissionsFactory().getRightsMgmtPermission(
			    processInstanceId);
			getBpmFactory().getRolesManager().checkPermission(perm);
			
		} catch (AccessControlException e) {
			return false;
		}
		
		return true;
	}
	
	public String setAccessRightsForProcessResource(String roleName,
	        Long processInstanceId, Long taskInstanceId,
	        String variableIdentifier, boolean hasReadAccess,
	        boolean setSameRightsForAttachments, Integer userId) {
		
		IWContext iwc = IWContext.getIWContext(FacesContext
		        .getCurrentInstance());
		IWBundle bundle = IWMainApplication.getDefaultIWMainApplication()
		        .getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		
		if (roleName == null || CoreConstants.EMPTY.equals(roleName)
		        || (taskInstanceId == null && userId == null)) {
			logger
			        .log(
			            Level.WARNING,
			            "setAccessRightsForProcessResource called, but insufficient parameters provided. Got: roleName="
			                    + roleName
			                    + ", taskInstanceId="
			                    + taskInstanceId + ", userId=" + userId);
			return iwrb.getLocalizedString(
			    "attachments_permissions_update_failed",
			    "Attachments permissions update failed!");
		}
		
		if (taskInstanceId != null) {
			
			TaskInstanceW tiw = getBpmFactory()
			        .getProcessManagerByTaskInstanceId(taskInstanceId)
			        .getTaskInstance(taskInstanceId);
			
			tiw.setTaskRolePermissions(new Role(roleName,
			        hasReadAccess ? Access.read : null),
			    setSameRightsForAttachments, variableIdentifier);
			
		} else {
			
			ProcessInstanceW piw = getBpmFactory()
			        .getProcessManagerByProcessInstanceId(processInstanceId)
			        .getProcessInstance(processInstanceId);
			
			piw.setContactsPermission(new Role(roleName,
			        hasReadAccess ? Access.contactsCanBeSeen : null), userId);
		}
		
		return iwrb.getLocalizedString(
		    "attachments_permissions_successfully_updated",
		    "Permissions successfully updated.");
	}
	
	public org.jdom.Document setRoleDefaultContactsForUser(
	        Long processInstanceId, Integer userId) {
		
		getBpmFactory().getRolesManager().setContactsPermission(
		    new Role("default"), processInstanceId, userId);
		
		return getContactsAccessRightsSetterBox(processInstanceId, userId);
	}
	
	public org.jdom.Document getAccessRightsSetterBox(Long processInstanceId,
	        Long taskInstanceId, String fileHashValue,
	        boolean setSameRightsForAttachments) {
		
		return getAccessRightsSetterBox(processInstanceId, taskInstanceId,
		    fileHashValue, setSameRightsForAttachments, null);
	}
	
	public org.jdom.Document getContactsAccessRightsSetterBox(
	        Long processInstanceId, Integer userId) {
		
		return getAccessRightsSetterBox(processInstanceId, null, null, null,
		    userId);
	}
	
	private org.jdom.Document getAccessRightsSetterBox(Long processInstanceId,
	        Long taskInstanceId, String fileHashValue,
	        Boolean setSameRightsForAttachments, Integer userId) {
		
		if (taskInstanceId == null && userId == null) {
			return null;
		}
		final IWContext iwc = IWContext.getCurrentInstance();
		if (iwc == null) {
			return null;
		}
		
		BuilderService builder = getBuilderService();
		IWBundle bundle = iwc.getIWMainApplication().getBundle(
		    IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		if (builder == null) {
			return null;
		}
		Layer container = new Layer();
		
		final Collection<Role> roles;
		
		if (taskInstanceId != null) {
			
			// TODO: add method to taskInstanceW and get permissions from there
			TaskInstanceW tiw = getBpmFactory()
			        .getProcessManagerByTaskInstanceId(taskInstanceId)
			        .getTaskInstance(taskInstanceId);
			
			if (StringUtil.isEmpty(fileHashValue)) {
				
				roles = tiw.getRolesPermissions();
				
			} else {
				
				roles = tiw.getAttachmentRolesPermissions(fileHashValue);
			}
			
		} else {
			
			ProcessInstanceW piw = getBpmFactory()
			        .getProcessManagerByProcessInstanceId(processInstanceId)
			        .getProcessInstance(processInstanceId);
			
			roles = piw.getRolesContactsPermissions(userId);
		}
		
		ArrayList<String[]> accessParamsList = new ArrayList<String[]>();
		
		Link closeLink = new Link(iwrb.getLocalizedString("close", "Close"));
		closeLink.setURL("javascript:void(0);");
		closeLink
		        .setOnClick("CasesBPMAssets.closeAccessRightsSetterBox(this);");
		
		if (ListUtil.isEmpty(roles)) {
			container.add(new Heading3(iwrb.getLocalizedString(
			    "no_roles_to_set_permissions",
			    "There are no roles to set access rights")));
			
			Layer buttonsContainer = new Layer();
			container.add(buttonsContainer);
			
			container.add(closeLink);
		} else {
			
			if (userId == null || setSameRightsForAttachments == null)
				setSameRightsForAttachments = false;
			
			container.add(new Heading3(iwrb.getLocalizedString(
			    "set_access_rights", "Set access rights")));
			
			Layer checkBoxes = new Layer();
			container.add(checkBoxes);
			Table2 table = new Table2();
			checkBoxes.add(table);
			TableHeaderRowGroup headerRowGroup = table.createHeaderRowGroup();
			TableRow headerRow = headerRowGroup.createRow();
			// Role name
			TableHeaderCell headerCell = headerRow.createHeaderCell();
			headerCell.add(new Text(iwrb.getLocalizedString("role_name",
			    "Role name")));
			// Permission to read
			headerCell = headerRow.createHeaderCell();
			
			if (taskInstanceId != null)
				headerCell.add(new Text(iwrb.getLocalizedString(
				    "allow_disallow_to_read", "Allow/disallow to read")));
			else
				headerCell.add(new Text(iwrb.getLocalizedString(
				    "allow_disallow_to_see_role_contacts",
				    "Allow/disallow to see contacts of role")));
			
			// Set same rights for attachments
			if (setSameRightsForAttachments) {
				headerCell = headerRow.createHeaderCell();
				headerCell.add(new Text(iwrb.getLocalizedString(
				    "set_same_permission_for_attachements",
				    "Set same access rights to attachments")));
			}
			
			TableBodyRowGroup bodyRowGroup = table.createBodyRowGroup();
			
			for (Role role : roles) {
				String roleName = role.getRoleName();
				
				TableRow bodyRow = bodyRowGroup.createRow();
				TableCell2 cell = bodyRow.createCell();
				
				// cell.add(new Text(iwrb.getLocalizedString(roleName, roleName)));
				cell.add(new Text(iwc.getIWMainApplication()
				        .getLocalisedStringMessage(roleName, roleName, null,
				            iwc.getCurrentLocale())));
				
				GenericButton sameRigthsSetter = null;
				if (setSameRightsForAttachments) {
					sameRigthsSetter = new GenericButton();
					Image setRightImage = new Image(
					        bundle
					                .getVirtualPathWithFileNameString("images/same_rights_button.png"));
					setRightImage.setTitle(iwrb.getLocalizedString(
					    "set_same_access_to_attachments_for_this_role",
					    "Set same access to attachments for this role"));
					setRightImage.setStyleClass("setSameAccessRightsStyle");
					sameRigthsSetter.setButtonImage(setRightImage);
				}
				
				CheckBox box = new CheckBox(roleName);
				
				if (taskInstanceId != null)
					box.setChecked(role.getAccesses() != null
					        && role.getAccesses().contains(Access.read));
				else
					box.setChecked(role.getAccesses() != null
					        && role.getAccesses().contains(
					            Access.contactsCanBeSeen));
				
				StringBuilder action = new StringBuilder(
				        "CasesBPMAssets.setAccessRightsForBpmRelatedResource('")
				        .append(box.getId()).append("', ");
				action.append(processInstanceId);
				action.append(", ").append(taskInstanceId).append(", ").append(
				    userId).append(", ");
				
				if (fileHashValue == null) {
					action.append("null");
				} else {
					action.append("'").append(fileHashValue).append("'");
				}
				action.append(", ");
				
				StringBuilder actionForCheckbox = new StringBuilder(action);
				box.setOnClick(actionForCheckbox.append("null").append(");")
				        .toString());
				cell = bodyRow.createCell();
				cell.setStyleClass("alignCenterText");
				cell.add(box);
				
				if (setSameRightsForAttachments) {
					String[] accessRightsParams = { box.getId(),
					        processInstanceId.toString(),
					        taskInstanceId.toString(), fileHashValue,
					        sameRigthsSetter.getId() };
					
					accessParamsList.add(accessRightsParams);
					
					cell = bodyRow.createCell();
					StringBuilder actionForButton = new StringBuilder(action);
					sameRigthsSetter.setOnClick(actionForButton.append("'")
					        .append(sameRigthsSetter.getId()).append("'")
					        .append(");").toString());
					cell.setStyleClass("alignCenterText");
					cell.add(sameRigthsSetter);
				}
			}
			
			TableRow bodyRow = bodyRowGroup.createRow();
			TableCell2 cell = bodyRow.createCell();
			
			cell.add(closeLink);
			
			if (taskInstanceId == null) {
				
				Link setDefaultsLink = new Link(iwrb.getLocalizedString(
				    "bpm_resetToDefault", "Reset to default"));
				setDefaultsLink.setURL("javascript:void(0);");
				
				StringBuffer onclick = new StringBuffer(
				        "CasesBPMAssets.setRoleDefaultContactsForUser(this, ");
				
				onclick.append(processInstanceId).append(", ").append(userId)
				        .append(");");
				
				setDefaultsLink.setOnClick(onclick.toString());
				setDefaultsLink.setStyleClass("setRoleDefaults");
				
				cell.add(setDefaultsLink);
			}
			
			if (setSameRightsForAttachments) {
				
				GenericButton saveAllRightsButton = new GenericButton();
				Image saveRigtsImage = new Image(
				        bundle
				                .getVirtualPathWithFileNameString("images/save_rights_button.png"));
				saveRigtsImage.setTitle(iwrb.getLocalizedString(
				    "set_same_access_to_attachments_for_all_roles",
				    "Set same access to attachments for all roles"));
				saveRigtsImage.setStyleClass("setSameAccessRightsStyle");
				saveAllRightsButton.setButtonImage(saveRigtsImage);
				
				StringBuilder paramsArray = new StringBuilder("[ ");
				
				for (String[] params : accessParamsList) {
					paramsArray.append(" [ ");
					paramsArray.append(params[0] == null ? "null ," : "'"
					        + params[0] + "', ");
					paramsArray.append(params[1] == null ? "null ," : "'"
					        + params[1] + "', ");
					paramsArray.append(params[2] == null ? "null ," : "'"
					        + params[2] + "', ");
					paramsArray.append(params[3] == null ? "null ," : "'"
					        + params[3] + "', ");
					paramsArray.append(params[4] == null ? "null" : "'"
					        + params[4] + "'");
					paramsArray.append("] , ");
				}
				paramsArray.append("]");
				
				saveAllRightsButton
				        .setOnClick("for each (params in "
				                + paramsArray.toString()
				                + ") {CasesBPMAssets.setAccessRightsForBpmRelatedResource(params[0] ,params[1] ,params[2] ,params[3] ,params[4]); }");
				
				cell = bodyRow.createCell();
				cell.empty();
				cell = bodyRow.createCell();
				cell.setStyleClass("alignCenterText");
				cell.add(saveAllRightsButton);
			}
		}
		
		return builder.getRenderedComponent(iwc, container, false);
	}
	
	public String takeBPMProcessTask(Long taskInstanceId, boolean reAssign) {
		if (taskInstanceId == null) {
			return null;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return null;
		}
		
		User currentUser = null;
		try {
			currentUser = iwc.getCurrentUser();
		} catch (NotLoggedOnException e) {
			e.printStackTrace();
		}
		if (currentUser == null) {
			return null;
		}
		
		try {
			ProcessManager processManager = getBpmFactory()
			        .getProcessManagerByTaskInstanceId(taskInstanceId);
			TaskInstanceW taskInstance = processManager
			        .getTaskInstance(taskInstanceId);
			
			User assignedTo = taskInstance.getAssignedTo();
			if (assignedTo != null && !reAssign) {
				return assignedTo.getName();
			} else {
				taskInstance.assign(currentUser);
			}
			
			return getAssignedToYouLocalizedString(iwc.getIWMainApplication()
			        .getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER)
			        .getResourceBundle(iwc));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String watchOrUnwatchBPMProcessTask(Long processInstanceId) {
		String errorMessage = "Sorry, error occurred - can not fulfill your action";
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return errorMessage;
		}
		
		IWResourceBundle iwrb = null;
		try {
			iwrb = iwc.getIWMainApplication().getBundle(
			    IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Can not get IWResourceBundle", e);
		}
		if (iwrb == null) {
			return errorMessage;
		}
		
		errorMessage = iwrb.getLocalizedString(
		    "cases_bpm.can_not_fulfill_action", errorMessage);
		
		if (processInstanceId == null) {
			return errorMessage;
		}
		
		ProcessWatch pwatch = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId).getProcessWatcher();
		
		if (pwatch.isWatching(processInstanceId)) {
			// Remove
			if (pwatch.removeWatch(processInstanceId)) {
				return pwatch.getWatchCaseStatusLabel(false);
			}
			
			return errorMessage;
		}
		
		// Add
		if (pwatch.takeWatch(processInstanceId)) {
			return pwatch.getWatchCaseStatusLabel(true);
		}
		
		return errorMessage;
	}
	
	public String assignCase(String handlerIdStr, Long processInstanceId) {
		
		ProcessInstanceW piw = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId);
		
		if (piw.hasRight(Right.processHandler)) {
			
			Integer handlerId = handlerIdStr == null
			        || handlerIdStr.length() == 0 ? null : new Integer(
			        handlerIdStr);
			
			piw.assignHandler(handlerId);
			
			return "great success";
		}
		
		return "no rights to take case";
	}
	
	public List<AdvancedProperty> getAllHandlerUsers(Long processInstanceId) {
		
		if (processInstanceId != null) {
			
			ProcessInstanceW piw = getBpmFactory()
			        .getProcessManagerByProcessInstanceId(processInstanceId)
			        .getProcessInstance(processInstanceId);
			
			if (piw.hasRight(Right.processHandler)
			        && piw.hasHandlerAssignmentSupport()) {
				
				RolesManager rolesManager = getBpmFactory().getRolesManager();
				
				List<String> caseHandlersRolesNames = rolesManager
				        .getRolesForAccess(processInstanceId,
				            Access.caseHandler);
				Collection<User> users = rolesManager.getAllUsersForRoles(
				    caseHandlersRolesNames, processInstanceId);
				
				Integer assignedCaseHandlerId = piw.getHandlerId();
				
				String assignedCaseHandlerIdStr = assignedCaseHandlerId == null ? null
				        : String.valueOf(assignedCaseHandlerId);
				
				ArrayList<AdvancedProperty> allHandlers = new ArrayList<AdvancedProperty>(
				        1);
				
				IWContext iwc = IWContext.getIWContext(FacesContext
				        .getCurrentInstance());
				IWBundle bundle = iwc.getIWMainApplication().getBundle(
				    IWBundleStarter.IW_BUNDLE_IDENTIFIER);
				IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
				
				AdvancedProperty ap = new AdvancedProperty(CoreConstants.EMPTY,
				        iwrb.getLocalizedString("bpm.caseHandler",
				            "Case handler"));
				allHandlers.add(ap);
				
				for (User user : users) {
					
					String pk = String.valueOf(user.getPrimaryKey());
					
					ap = new AdvancedProperty(pk, user.getName());
					
					if (pk.equals(assignedCaseHandlerIdStr)) {
						
						ap.setSelected(true);
					}
					
					allHandlers.add(ap);
				}
				
				return allHandlers;
			}
		}
		
		return null;
	}
	
	private String getAssignedToYouLocalizedString(IWResourceBundle iwrb) {
		return iwrb.getLocalizedString("cases_bpm.case_assigned_to_you", "You");
	}
	
	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}
	
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}
	
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	protected UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil
			        .getIWContext(), UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}
	
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}
	
	public BuilderLogicWrapper getBuilderLogicWrapper() {
		return builderLogicWrapper;
	}
	
	public void setBuilderLogicWrapper(BuilderLogicWrapper builderLogicWrapper) {
		this.builderLogicWrapper = builderLogicWrapper;
	}
	
	SigningHandler getSigningHandler() {
		return signingHandler;
	}
	
	void setSigningHandler(SigningHandler signingHandler) {
		this.signingHandler = signingHandler;
	}
	
	public String getSigningAction(String taskInstanceId, String hashValue) {
		return getSigningHandler().getSigningAction(
		    Long.valueOf(taskInstanceId), hashValue);
	}
	
	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}
}