package com.idega.jbpm.identity;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.idega.jbpm.identity.permission.Access;
import com.idega.util.CoreConstants;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;


/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/03/12 15:43:03 $ by $Author: civilis $
 */
public class JSONExpHandler {
	
	private class TaskAssignment { List<Role> roles; }
	
	private static final String taskAssignment = "taskAssignment";
	private static final String role = "role";
	private static final String access = "access";
	
	public static List<Role> resolveRolesFromJSONExpression(String expression) {
		
		expression = expression.trim();
		
		if(!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT) || !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {
			
			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);
			
			if(lbi < 0 || rbi < 0) {
				
				Logger.getLogger(JSONExpHandler.class.getName(), "Expression provided does not contain json expression. Expression: "+expression);
				return Collections.emptyList();
			}
				
			expression = expression.substring(lbi, rbi+1);
		}
		
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(taskAssignment, TaskAssignment.class);
		xstream.alias(role, Role.class);
		xstream.alias(access, Access.class);
		
		TaskAssignment assignmentExp = (TaskAssignment)xstream.fromXML(expression);

		return assignmentExp.roles;
	}
}