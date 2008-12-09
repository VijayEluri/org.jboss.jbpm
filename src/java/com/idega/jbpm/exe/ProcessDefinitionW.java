package com.idega.jbpm.exe;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.jbpm.graph.def.ProcessDefinition;

import com.idega.block.process.variables.Variable;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.13 $
 * 
 *          Last modified: $Date: 2008/12/09 02:47:47 $ by $Author: civilis $
 */
public interface ProcessDefinitionW {

	public abstract void startProcess(ViewSubmission viewSubmission);

	public abstract View loadInitView(Integer initiatorId);

	public abstract void setProcessDefinitionId(Long processDefinitionId);

	public abstract Long getProcessDefinitionId();

	public abstract ProcessDefinition getProcessDefinition();

	public abstract void setRolesCanStartProcess(List<String> roles,
			Object context);

	public abstract List<String> getRolesCanStartProcess(Object context);

	public abstract String getStartTaskName();

	public abstract List<Variable> getTaskVariableList(String taskName);

	public abstract Collection<String> getTaskNodeTransitionsNames(String taskName);
	
	public abstract String getProcessName(Locale locale);
}