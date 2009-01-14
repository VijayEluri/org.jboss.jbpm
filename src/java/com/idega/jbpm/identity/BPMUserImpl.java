package com.idega.jbpm.identity;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.business.IBORuntimeException;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.URIUtil;

/**
 *  
 * Wrapper of at least one, or two user entities, which correspond to bpm-user account and/or to logged-in user's account. <br />
 * bpm-user account represents shared account, which unifies not logged in users, and/or logged in users. <br /> 
 * Use case example is when user gets to the process by following link.
 *   
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 * 
 * Last modified: $Date: 2009/01/14 10:07:31 $ by $Author: juozas $
 */
public class BPMUserImpl implements BPMUser {
	
	private Boolean isAssociated;
	private User bpmUser;
	private User realUser;
	private BPMUserFactory bpmUserFactory;
	private IWContext iwc;
	private Long processInstanceId;
	
	public User getBpmUser() {
		return bpmUser;
	}
	public void setBpmUser(User bpmUser) {
		setIsAssociated(null);
		processInstanceId = null;
		this.bpmUser = bpmUser;
	}
	public User getRealUser() {
		return realUser;
	}
	public void setRealUser(User realUser) {
		setIsAssociated(null);
		this.realUser = realUser;
	}
	public Boolean getIsAssociated(boolean clean) {
		
		if(clean || isAssociated == null) {

			if(getBpmUser() != null && getRealUser() != null) {
				
				isAssociated = getBpmUserFactory().isAssociated(getRealUser(), getBpmUser(), true);
			}
		}
		
		return isAssociated == null ? false : isAssociated;
	}
	public Boolean getIsAssociated() {
		
		return getIsAssociated(false);
	}
	public void setIsAssociated(Boolean isAssociated) {
		this.isAssociated = isAssociated;
	}
	
	public BPMUserFactory getBpmUserFactory() {
		return bpmUserFactory;
	}
	@Autowired
	public void setBpmUserFactory(BPMUserFactory bpmUserFactory) {
		this.bpmUserFactory = bpmUserFactory;
	}
	public Integer getIdToUse() {
		
		Object pk;
	
		if(getRealUser() != null && (getIsAssociated(false) || getBpmUser() == null)) {
			
			pk = getRealUser().getPrimaryKey();
		} else if(getBpmUser() != null) {
			pk = getBpmUser().getPrimaryKey();
		} else
			pk = null;
		
		if(pk != null) {
			
			if(pk instanceof Integer)
				return (Integer)pk;
			else
				return new Integer(pk.toString());
		}
		
		return null;
	}
	
	public User getUserToUse() {
		
		Object pk;
	
		if(getRealUser() != null && (getIsAssociated(false) || getBpmUser() == null)) {
			
			return getRealUser();
		} else if(getBpmUser() != null) {
			return getBpmUser();
		} else
			return null;
		
	
	}
	
	public String getUrlToTheProcess() {
		
		IWContext iwc = getIwc();
		
		if(iwc == null) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to get url to the process, but no IWContext set");
		}
		
		if(getProcessInstanceId() == null) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to get url to the process, but no process instance id resolved from bpm user="+getBpmUser().getPrimaryKey());
		}
		
		String fullUrl = getAssetsUrl(iwc);
		
		final URIUtil uriUtil = new URIUtil(fullUrl);
		
		uriUtil.setParameter(processInstanceIdParam, getProcessInstanceId().toString());
		uriUtil.setParameter(BPMUser.bpmUsrParam, getBpmUser().getPrimaryKey().toString());
		fullUrl = uriUtil.getUri();
		
		return fullUrl;
	}
	
	private String getAssetsUrl(IWContext iwc) {
		
//		TODO: try to resolve url from app prop, if fail, then use default page type, and resolve from it (as it is now)
		String fullUrl = getBuilderService(iwc).getFullPageUrlByPageType(iwc, defaultAssetsViewPageType, true);
		return fullUrl;
	}
	
	private BuilderService getBuilderService(IWApplicationContext iwc) {
		try {
			return BuilderServiceFactory.getBuilderService(iwc);
			
		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
		}
	}
	public void setIwc(IWContext iwc) {
		this.iwc = iwc;
	}
	IWContext getIwc() {
		
		if(iwc == null)
			iwc = IWContext.getCurrentInstance();
		
		return iwc;
	}
	Long getProcessInstanceId() {
		
		if(processInstanceId == null) {
		
			User usr = getBpmUser();
			String processInstanceIdStr = usr.getMetaData(BPMUser.PROCESS_INSTANCE_ID);
			processInstanceId = new Long(processInstanceIdStr);
		}
		
		return processInstanceId;
	}
}