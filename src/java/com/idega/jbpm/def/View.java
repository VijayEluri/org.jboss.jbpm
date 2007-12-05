package com.idega.jbpm.def;

import java.util.Map;

import javax.faces.component.UIComponent;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2007/12/05 10:36:31 $ by $Author: civilis $
 */
public interface View {

	public abstract void setViewId(String viewId);
	public abstract String getViewId();
	
	public abstract void setViewType(String viewType);
	public abstract String getViewType();
	
	public abstract UIComponent getViewForDisplay();
	
	public abstract void populate(Map<String, Object> variables);
	
	public abstract boolean isSubmitable();
	public abstract void setSubmitable(boolean submitable);
	
	public abstract void addParameters(Map<String, String> parameters);
}