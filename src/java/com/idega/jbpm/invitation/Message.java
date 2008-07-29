package com.idega.jbpm.invitation;

import java.io.Serializable;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/07/29 07:58:44 $ by $Author: anton $
 */
public class Message implements Serializable {

	private static final long serialVersionUID = -8656603555398778383L;
	private String subject;
	private String text;
	private String from;
	private String caseIdentifier;
	private boolean setCaseIdentifier = true;
	
	public String getCaseIdentifier() {
		return caseIdentifier;
	}
	public void setCaseIdentifier(String caseIdentifier) {
		this.caseIdentifier = caseIdentifier;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public boolean isSetCaseIdentifier() {
		return setCaseIdentifier;
	}
	public void setSetCaseIdentifier(boolean setCaseIdentifier) {
		this.setCaseIdentifier = setCaseIdentifier;
	}
}