package com.idega.jbpm.variables;

import java.util.List;
import java.util.Map;



/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/19 13:52:39 $ by $Author: civilis $
 */
public interface VariablesHandler {

	public abstract void submitVariables(Map<String, Object> variables,
			long taskInstanceId, boolean validate);

	public abstract Map<String, Object> populateVariables(long taskInstanceId);
	
	public abstract List<BinaryVariable> resolveBinaryVariables(long taskInstanceId);
	
	public abstract BinaryVariablesHandler getBinaryVariablesHandler();
}