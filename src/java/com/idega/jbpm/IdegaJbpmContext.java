package com.idega.jbpm;

import java.util.Stack;

import javax.persistence.EntityManagerFactory;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 *
 * Last modified: $Date: 2008/09/15 15:44:57 $ by $Author: civilis $
 */
public class IdegaJbpmContext implements BPMContext {
	
	public static final String beanIdentifier = "idegaJbpmContext";
	private EntityManagerFactory entityManagerFactory;
	private ThreadLocal<Stack<Boolean>> doCommitStackLocal = new ThreadLocal<Stack<Boolean>>();

	public JbpmConfiguration getJbpmConfiguration() {
		return JbpmConfiguration.getInstance();
	}

	protected Stack<Boolean> getDoCommitStack() {
		
		Stack<Boolean> stack = doCommitStackLocal.get();
		
	    if (stack == null) {
	    	
	      stack = new Stack<Boolean>();
	      doCommitStackLocal.set(stack);
	    }
	    
	    return stack;
	}
	
	public JbpmContext createJbpmContext() {
		
		return JbpmConfiguration.getInstance().createJbpmContext();
	}
	
	public void closeAndCommit(JbpmContext ctx) {
		
		ctx.close();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}
	
	public void saveProcessEntity(Object entity) {

		JbpmContext current = getJbpmConfiguration().getCurrentJbpmContext();
		
		if(current != null) {
		
			current.getSession().save(entity);
		} else
			throw new IllegalStateException("No current JbpmContext resolved. Create JbpmContext around calling this method");
	}
}