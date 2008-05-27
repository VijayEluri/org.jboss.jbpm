package com.idega.jbpm.identity;

import com.idega.user.data.User;

/**
 *  
 * Wrapper of at least one, or two user entities, which correspond to bpm-user account and-if to logged-in user's account. <br />
 * bpm-user account represents shared account, which unifies not logged in users, and/or logged in users. <br /> 
 * Use case example is when user gets to the process by following link.
 *   
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 * Last modified: $Date: 2008/05/25 14:58:40 $ by $Author: civilis $
 */
public interface BPMUser {
	
	public static final String bpmUserIdentifier = "BPM_USER";

	public abstract User getBpmUser();

	public abstract User getRealUser();

	public abstract Boolean getIsAssociated();

	public abstract Integer getIdToUse();
}