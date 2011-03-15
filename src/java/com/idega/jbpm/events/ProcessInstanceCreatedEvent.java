package com.idega.jbpm.events;

import org.springframework.context.ApplicationEvent;

public class ProcessInstanceCreatedEvent extends ApplicationEvent {

	private static final long serialVersionUID = 8587349554332579987L;

	private String processDefinitionName;
	private Long processInstanceId;
	
	public ProcessInstanceCreatedEvent(String processDefinitionName, Long processInstanceId) {
		super(processDefinitionName);
		
		this.processDefinitionName = processDefinitionName;
		this.processInstanceId = processInstanceId;
	}

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

}