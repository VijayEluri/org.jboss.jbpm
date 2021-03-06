package com.idega.jbpm;

import java.util.Collections;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.job.executor.JobExecutor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.test.base.IdegaBaseTransactionalTest;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 *          Last modified: $Date: 2009/02/19 13:06:40 $ by $Author: civilis $
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public final class AsyncTest extends IdegaBaseTransactionalTest {
	
	private static final Logger log = Logger.getLogger(AsyncTest.class.getName());
	protected JobExecutor jobExecutor;
	static long maxWaitTime = 20000;

	@Autowired
	private BPMContext bpmContext;
	
	void deployProcessDefinitions() throws Exception {

		JbpmContext jctx = bpmContext.createJbpmContext();

		try {
			ProcessDefinition simpleAsyncProcess = ProcessDefinition
					.parseXmlString("<process-definition name='bulk messages'>"
							+ "  <start-state>"
							+ "    <transition to='b' />"
							+ "  </start-state>"
							// + "  <node name='a' async='true'>"
							// + "    <action class='"
							// + Automatica.class.getName()
							// + "' />"
							// + "    <transition to='b' />"
							// + "  </node>"
							+ "  <node name='b' >"
							+ "    <event type='node-enter'>"
							+ "      <action name='X' async='true' class='"
							+ AsyncAction.class.getName()
							+ "' />"
							+ "    </event>"
							// + "    <action class='"
							// + AutomaticActivity.class.getName()
							// + "' />"
							+ "    <transition to='end' />"
							+ "  </node>"
							// + "  <node name='c' async='true'>"
							// + "    <action class='"
							// + AutomaticActivity.class.getName()
							// + "' />"
							// + "    <transition to='d'>"
							// + "      <action name='Y' async='true' class='"
							// + AsyncAction.class.getName()
							// + "' />"
							// + "    </transition>"
							// + "  </node>"
							// + "  <node name='d' async='true'>"
							// + "    <action class='"
							// + AutomaticActivity.class.getName()
							// + "' />"
							// + "    <transition to='e' />"
							// + "    <event type='node-leave'>"
							// + "      <action name='Z' async='true' class='"
							// + AsyncAction.class.getName()
							// + "' />"
							// + "    </event>"
							// + "  </node>"
							// + "  <node name='e' async='true'>"
							// + "    <action class='"
							// + AutomaticActivity.class.getName()
							// + "' />"
							// + "    <transition to='end' />"
							// + "  </node>"
							+ "  <end-state name='end'/>"
							+ "</process-definition>");
			jctx.deployProcessDefinition(simpleAsyncProcess);
			
			/*
			ProcessDefinition asyncBetweenForksProcess = ProcessDefinition
			.parseXmlString("<process-definition name='asyncBetweenForks'>"
					+ "  <start-state>"
					+ "    <transition to='b' />"
					+ "  </start-state>"
//							+"<fork name='fork1'>"
//							+"<transition name='toB' to='b'></transition>"
//							+"<transition name='toNotimportant' to='notimportant'></transition>"
//							+ "</fork>"
					+ "  <node name='b'>"
					+ "    <event type='node-leave'>"
					+ "      <action name='asyncAction' async='true' class='"
							+ AsyncAction.class.getName()
							+ "' />"
					+ "    </event>"
					+ "    <transition to='fork2' />"
					+ "  </node>"
					+ "  <node name='notimportant'>"
					+ "    <transition to='end' />"
					+ "  </node>"
						+"<fork name='fork2'>"
						+"<transition name='toSomeTask2' to='sometask2'></transition>"
						+"<transition name='toSomeTask3' to='sometask3'></transition>"
						+ "</fork>"
						+"<task-node name='sometask2'>"
						+"<task name='st2'>"
						+"<controller>"
						+"<variable name='string_documentKey' access='read,write,required'></variable>"
						+"</controller>"
						+"</task>" +
								"<transition to='end' name='toEnd'></transition>"
						+"</task-node>"
						+"<task-node name='sometask3'>"
						+"<task name='st3'>"
						+"<controller>"
						+"<variable name='string_documentKey' access='read,write,required'></variable>"
						+"</controller>"
						+"</task>"
						+"<transition to='end' name='toEnd'></transition>"
						+"</task-node>"
					+ "  <end-state name='end'/>"
					+ "</process-definition>");
			jctx.deployProcessDefinition(asyncBetweenForksProcess);
			*/

		} finally {
			bpmContext.closeAndCommit(jctx);
		}
	}

	@Test
	public void testAsync() throws Exception {
		
		if(true)
			return;

		/*deployProcessDefinitions();
		
		JbpmContext jbpmContext = bpmContext.createJbpmContext();

		try {
			jobExecutor = jbpmContext.getJbpmConfiguration().getJobExecutor();
			
			ProcessInstance pi = jbpmContext.newProcessInstance("bulk messages");
			pi.signal();
			
//			jbpmContext.getJobSession().

		} finally {
			bpmContext.closeAndCommit(jbpmContext);
		}
		
		processJobs(maxWaitTime);*/
	}
	
	/*
	@Test
	public void testAsyncBetweenForks() throws Exception {
		
		if(true)
			return;

		deployProcessDefinitions();
		
		JbpmContext jbpmContext = bpmContext.createJbpmContext();

		try {
			jobExecutor = jbpmContext.getJbpmConfiguration().getJobExecutor();
			
			ProcessInstance pi = jbpmContext.newProcessInstance("asyncBetweenForks");
			pi.signal();
			
//			jbpmContext.getJobSession().

		} finally {
			bpmContext.closeAndCommit(jbpmContext);
		}
		
		processJobs(maxWaitTime);
	}
	*/

	public static class AsyncAction implements ActionHandler {
		private static final long serialVersionUID = 1L;

		public void execute(ExecutionContext executionContext) throws Exception {
			
			Thread.sleep(8000);
			
			String id = (String) Long.toString(executionContext
					.getProcessInstance().getId());
			Action action = executionContext.getAction();
			String actionName = action.getName();
			
			collectedResults.add(id + actionName);
		}
	}
	
	private void processAllJobs(final long maxWait) {
	    boolean jobsAvailable = true;

	    // install a timer that will interrupt if it takes too long
	    // if that happens, it will lead to an interrupted exception and the test will fail
	    TimerTask interruptTask = new TimerTask() {
	      Thread testThread = Thread.currentThread();
	      public void run() {
	        log.log(Level.WARNING, "test "+getName()+" took too long. going to interrupt...");
	        testThread.interrupt();
	      }
	    };
	    Timer timer = new Timer();
	    timer.schedule(interruptTask, maxWait);
	    
	    try {
	      while (jobsAvailable) {
	    	  log.log(Level.WARNING, "going to sleep for 200 millis, waiting for the job executor to process more jobs");
	        Thread.sleep(200);
	        jobsAvailable = areJobsAvailable();
	      }
	      jobExecutor.stopAndJoin();
	      
	    } catch (InterruptedException e) {
	      fail("test execution exceeded treshold of "+maxWait+" milliseconds");
	    } finally {
	      timer.cancel();
	    }
	  }
	
	protected void processJobs(long maxWait) {
	    try {
	      Thread.sleep(300);
	    } catch (InterruptedException e) {
	      e.printStackTrace();
	    }
	    startJobExecutor();
	    try {
	      processAllJobs(maxWait);
	    } catch (Exception e) {
	      e.printStackTrace();
	      throw new RuntimeException(e);
	    } finally {
	      stopJobExecutor();
	    }
	  }

	static Set collectedResults = Collections.synchronizedSet(new TreeSet());
	
	protected boolean areJobsAvailable() {
	    return (getNbrOfJobsAvailable()>0);
	  }
	
	protected void startJobExecutor() {
	    //jobExecutor.start();
	  }
	
	private int getNbrOfJobsAvailable() {
	    int nbrOfJobsAvailable = 0;
	    JbpmContext jbpmContext = bpmContext.createJbpmContext();
	    try {
	      Session session = jbpmContext.getSession();
	      Number jobs = (Number) session.createQuery("select count(*) from org.jbpm.job.Job").uniqueResult();
	      log.log(Level.WARNING, "there are '"+jobs+"' jobs currently in the job table");
	      if (jobs!=null) {
	        nbrOfJobsAvailable = jobs.intValue();
	      }
	    } finally {
	    	bpmContext.closeAndCommit(jbpmContext);
	    }
	    return nbrOfJobsAvailable;
	  }
	
	protected void stopJobExecutor() {
	    if (jobExecutor!=null) {
	      try {
	        jobExecutor.stopAndJoin();
	      } catch (InterruptedException e) {
	        throw new RuntimeException("waiting for job executor to stop and join got interrupted", e); 
	      }
	    }
	  }
}