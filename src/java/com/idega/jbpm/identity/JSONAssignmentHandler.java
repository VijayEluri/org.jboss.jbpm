package com.idega.jbpm.identity;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.identity.assignment.ExpressionAssignmentHandler;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.CoreConstants;
import com.idega.util.expression.ELUtil;

/**
 * <p>Expects assignment expression in json notation. E.g.:</p>
 * 
 * <p>
 * <code>
 * {taskAssignment: {roles: {role: [
 *	{roleName: handler, accesses: {access: [read, write]}},
 *	{roleName: owner, accesses: {access: [read, write]}} 
 * ]} }}
 * </code>
 * </p>
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.13 $
 * 
 * Last modified: $Date: 2008/11/13 15:08:32 $ by $Author: juozas $
 */
@Service("jsonAssignmentHandler")
@Scope("prototype")
public class JSONAssignmentHandler  extends ExpressionAssignmentHandler {
	
	private static final long serialVersionUID = 8955094455268141204L;
	
	private static final String rolesAssignerBeanIdentifier = "bpmRolesAssiger";

	public void assign(Assignable assignable, ExecutionContext executionContext) {

		if(!(assignable instanceof TaskInstance))
			throw new IllegalArgumentException("Only TaskInstance is accepted for assignable");
		
		this.executionContext = executionContext;
		
		String exp = resolveJSONExp(getExpression().trim());
		
		if(exp != null) {
		
			TaskInstance taskInstance = (TaskInstance)assignable;
			
			List<Role> roles = JSONExpHandler.resolveRolesFromJSONExpression(exp);
			
			RolesAssiger rolesAssigner = getRolesAssigner();
			rolesAssigner.assign(taskInstance.getProcessInstance(), roles);
			rolesAssigner.createRolesPermissions(taskInstance, roles);
			rolesAssigner.assignIdentities(taskInstance, roles);
		}
	}
	
	public void setExpression(String expression){
		this.expression = expression;
	}
	
	public String getExpression(){
		return expression;
	}
	
	protected String resolveJSONExp(String exp) {
		
		if(exp.startsWith("variable(") && exp.endsWith(")")) {
		
			String variableName = exp.substring(9, exp.length()-1).trim();
			Object value = getVariable(variableName);
		      
			if(value == null || CoreConstants.EMPTY.equals(value)) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "No value found in variable: "+variableName+", json assignment expression expected");
				exp = null;
				
			} else if(value instanceof String) {
				
				exp = (String)value;
		    	  
			} else {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Wrong class variable value. Expected json expression in String. Class got: "+value.getClass().getName());
		    	exp = null;  
			}
		}
		
		return CoreConstants.EMPTY.equals(exp) ? null : exp;
	}
	
	protected RolesAssiger getRolesAssigner() {
		
		return ELUtil.getInstance().getBean(rolesAssignerBeanIdentifier);
	}
}