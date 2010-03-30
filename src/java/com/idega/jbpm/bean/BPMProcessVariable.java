package com.idega.jbpm.bean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

public class BPMProcessVariable {
	
	public static final List<String> DATE_TYPES = Collections.unmodifiableList(Arrays.asList("D"));
	public static final List<String> DOUBLE_TYPES = Collections.unmodifiableList(Arrays.asList("O"));
	public static final List<String> LONG_TYPES = Collections.unmodifiableList(Arrays.asList("L", "H"));
	public static final List<String> STRING_TYPES = Collections.unmodifiableList(Arrays.asList("S", "I"));
	public static final List<String> NULL_TYPES = Collections.unmodifiableList(Arrays.asList("N"));
	public static final List<String> JCR_NODE_TYPES = Collections.unmodifiableList(Arrays.asList("J"));
	public static final List<String> BYTE_ARRAY_TYPES = Collections.unmodifiableList(Arrays.asList("B"));
	
	private String name;
	private String value;
	private String type;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public boolean isTypeOf(List<String> types) {
		return (ListUtil.isEmpty(types) || StringUtil.isEmpty(getType())) ? false : types.contains(getType());
	}
	
	public boolean isDateType() {
		return isTypeOf(DATE_TYPES);
	}
	
	public boolean isDoubleType() {
		return isTypeOf(DOUBLE_TYPES);
	}
	
	public boolean isLongType() {
		return isTypeOf(LONG_TYPES);
	}
	
	public boolean isStringType() {
		return isTypeOf(STRING_TYPES);
	}
	
	@Override
	public String toString() {
		return new StringBuilder("Name: " ).append(getName()).append(", type: ").append(getType()).append(", value: ").append(getValue()).toString();
	}
}
