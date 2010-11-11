package com.idega.jbpm.bean;

import java.io.Serializable;
import java.util.logging.Logger;

import com.idega.util.StringUtil;

public abstract class VariableInstanceInfo implements Serializable {

	private static final long serialVersionUID = 7094674925493141143L;
	private static final Logger LOGGER = Logger.getLogger(VariableInstanceInfo.class.getName());
	
	private String name;
	private VariableInstanceType type;
	
	private Long id;
	private Long processInstanceId;
	
	public VariableInstanceInfo() {
		super();
	}
	
	public VariableInstanceInfo(Serializable value) {
		this(null, value);
	}
	
	public VariableInstanceInfo(String name, Serializable value) {
		this(name, value, null);
	}
	
	public VariableInstanceInfo(String name, VariableInstanceType type) {
		this(name, null, type);
	}
	
	public VariableInstanceInfo(String name, Serializable value, VariableInstanceType type) {
		this();
		
		this.name = name;
		setValue(value);
		this.type = type;
	}
	
	public VariableInstanceInfo(String name, String type) {
		this();
		this.name = name;
		
		VariableInstanceType varType = null;
		if (StringUtil.isEmpty(type)) {
			LOGGER.warning("Type is not defined!");
		} else {
			if (VariableInstanceType.STRING.getTypeKeys().contains(type)) {
				varType = VariableInstanceType.STRING;
			} else if (VariableInstanceType.LONG.getTypeKeys().contains(type)) {
				varType = VariableInstanceType.LONG;
			} else if (VariableInstanceType.DOUBLE.getTypeKeys().contains(type)) {
				varType = VariableInstanceType.DOUBLE;
			} else if (VariableInstanceType.DATE.getTypeKeys().contains(type)) {
				varType = VariableInstanceType.DATE;
			} else if (VariableInstanceType.BYTE_ARRAY.getTypeKeys().contains(type)) {
				varType = VariableInstanceType.BYTE_ARRAY;
			} else {
				LOGGER.warning("Unknown type: " + type);
			}
		}
		this.type = varType == null ? VariableInstanceType.NULL : varType;
	}
	
	public abstract Serializable getValue();

	public abstract void setValue(Serializable value);

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public VariableInstanceType getType() {
		return type;
	}

	public void setType(VariableInstanceType type) {
		this.type = type;
	}
	
	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Variable " + getName() + ", type " + getType() + ", value: " + getValue() + ", process instance ID: " + getProcessInstanceId();
	}
	
	public static VariableInstanceInfo getDefaultVariable(String name) {
		VariableInstanceInfo info = new VariableInstanceInfo() {
			private static final long serialVersionUID = 1L;
			@Override
			public void setValue(Serializable value) {
			}
			@Override
			public Serializable getValue() {
				return null;
			}
		};
		info.setName(name);
		return info;
	}
}