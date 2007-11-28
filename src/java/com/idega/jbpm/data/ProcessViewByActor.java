package com.idega.jbpm.data;


import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/11/28 12:45:19 $ by $Author: alexis $
 */
@Entity
@Table(name="PROCESS_VIEW_BIND")
@NamedQueries({
		@NamedQuery(name="processViewByActor.getByViewerType", query="from ProcessViewByActor PVA where PVA.viewerType = :viewerType and viewType = :viewType and processDefinitionId = :processDefinitionId"),
		@NamedQuery(name="processViewByActor.getByViewType", query="from ProcessViewByActor PVA where PVA.viewType = :viewType and processDefinitionId = :processDefinitionId")
})
public class ProcessViewByActor implements Serializable {
	
	private static final long serialVersionUID = -4151166970366065468L;
	
	public static final String GET_BY_VIEWER_TYPE_QUERY_NAME = "processViewByActor.getByViewerType";
	public static final String GET_BY_VIEW_TYPE_QUERY_NAME = "processViewByActor.getByViewType";
	public static final String viewerTypeParam = "viewerType";
	public static final String viewTypeParam = "viewType";
	public static final String processDefinitionIdParam = "processDefinitionId";
	
	public static final String VIEWER_TYPE_OWNER = "OWNER";
	public static final String VIEWER_TYPE_CASE_HANDLERS = "HANDLERS";
	public static final String VIEWER_TYPE_OTHERS = "OTHERS";
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="bind_id", nullable=false)
    private Long bindId;
	
	@Column(name="process_definition_id", nullable=false)
	private Long processDefinitionId;
	
	@Column(name="view_identifier", nullable=false)
	private String viewIdentifier;
	
	@Column(name="view_type", nullable=false)
	private String viewType;
	
	@Column(name="actor_id")
	private String actorId;
	
	@Column(name="actor_type")
	private String actorType;
	
	@Column(name="viewer_type", nullable=false)
	private String viewerType;
	
	public ProcessViewByActor() { }

	public Long getBindId() {
		return bindId;
	}

	@SuppressWarnings("unused")
	private void setBindId(Long bindId) {
		this.bindId = bindId;
	}

	public Long getProcessDefinitionId() {
		return processDefinitionId;
	}

	public void setProcessDefinitionId(Long processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
	}

	public String getViewIdentifier() {
		return viewIdentifier;
	}

	public void setViewIdentifier(String viewIdentifier) {
		this.viewIdentifier = viewIdentifier;
	}

	public String getViewType() {
		return viewType;
	}

	public void setViewType(String viewType) {
		this.viewType = viewType;
	}

	public String getActorId() {
		return actorId;
	}

	public void setActorId(String actorId) {
		this.actorId = actorId;
	}

	public String getActorType() {
		return actorType;
	}

	public void setActorType(String actorType) {
		this.actorType = actorType;
	}

	public String getViewerType() {
		return viewerType;
	}

	public void setViewerType(String viewerType) {
		this.viewerType = viewerType;
	}
	
	public static ProcessViewByActor getByViewerType(Session session, String viewerType, String viewType, Long processDefinition) {
		
		Transaction transaction = session.getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		try {
			return (ProcessViewByActor) session.getNamedQuery(GET_BY_VIEWER_TYPE_QUERY_NAME)
			.setString(viewerTypeParam, viewerType)
			.setString(viewTypeParam, viewType)
			.setLong(processDefinitionIdParam, processDefinition)
			.uniqueResult();
			
		} finally {
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public static List getByViewType(Session session, String viewType, Long processDefinition) {
		
		Transaction transaction = session.getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		try {
			return session.getNamedQuery(GET_BY_VIEW_TYPE_QUERY_NAME)
			.setString(viewTypeParam, viewType)
			.setLong(processDefinitionIdParam, processDefinition)
			.list();
			
		} finally {
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
}