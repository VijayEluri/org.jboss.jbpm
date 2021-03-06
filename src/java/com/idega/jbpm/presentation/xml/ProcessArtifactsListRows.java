package com.idega.jbpm.presentation.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.xmlio.out.XMLOutputStreamWriter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.xml.XmlUtil;
import com.thoughtworks.xstream.XStream;

/**
 * see http://www.trirand.com/blog/?page_id=4
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.11 $
 *
 * Last modified: $Date: 2008/07/23 10:13:46 $ by $Author: valdas $
 */
public class ProcessArtifactsListRows {

	static final String alias = "rows";
	
	private String page;
	private String total;
	private String records;
	private List<ProcessArtifactsListRow> rows = new ArrayList<ProcessArtifactsListRow>();
	
	public Integer getPage() {

		return page == null ? null : Integer.parseInt(page);
	}
	public void setPage(int pageNumber) {

		Integer totalPages = getTotal();
		
		if(totalPages != null && pageNumber > totalPages)
			throw new IllegalArgumentException("Illegal argument provided: total pages are less than page number provided. Total: "+totalPages+", pageNumber: "+pageNumber);
		
		page = String.valueOf(pageNumber);
	}
	public Integer getTotal() {
		
		return total == null ? null : Integer.parseInt(total);
	}
	public void setTotal(int totalPages) {
		
		Integer pageNumber = getPage();
		
		if(pageNumber != null && pageNumber > totalPages)
			throw new IllegalArgumentException("Illegal argument provided: total pages are less than page number provided. Total: "+totalPages+", pageNumber: "+pageNumber);
		
		total = String.valueOf(totalPages);
		records = total;
	}
	
	private static final String stringZero = "0";
	public void clearValues() {
		page = stringZero;
		total = stringZero;
//		records - optional
		records = null;
		rows.clear();
	}
	public Integer getRecords() {
		return records == null ? null : Integer.parseInt(records);
	}
	public void setRecords(int recordsCount) {
		this.records = String.valueOf(recordsCount);
	}
	public List<ProcessArtifactsListRow> getRows() {
		return rows;
	}
	public void addRow(ProcessArtifactsListRow row) {
		getRows().add(row);
	}
	
	public Document getDocument() throws Exception {
		try {
			Collections.sort(getRows(), new ProcessArtifactsListRowComparator(IWTimestamp.SHORT, IWTimestamp.SHORT, CoreUtil.getIWContext().getCurrentLocale()));
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		XStream xstream = new XStream();
    	xstream.alias(ProcessArtifactsListRows.alias, ProcessArtifactsListRows.class);
    	xstream.alias(ProcessArtifactsListRow.alias, ProcessArtifactsListRow.class);
    	
    	xstream.useAttributeFor(ProcessArtifactsListRow.class, ProcessArtifactsListRow.attributeId);
    	xstream.useAttributeFor(ProcessArtifactsListRow.class, ProcessArtifactsListRow.attributeStyleClass);
    	xstream.useAttributeFor(ProcessArtifactsListRow.class, ProcessArtifactsListRow.attributeDisabledSelection);
    	
    	xstream.addImplicitCollection(ProcessArtifactsListRows.class, ProcessArtifactsListRows.alias);
    	xstream.addImplicitCollection(ProcessArtifactsListRow.class, ProcessArtifactsListRow.implicitCells, ProcessArtifactsListRow.implicitAkaCells, String.class);
    	
    	ByteArrayOutputStream output = new ByteArrayOutputStream();
    	XMLOutputStreamWriter writer = new XMLOutputStreamWriter(output, XMLOutputStreamWriter.ENCODING_UTF_8);
    	xstream.toXML(this, writer);
    	
    	Reader reader = null;
		try {
			reader = new InputStreamReader(new ByteArrayInputStream(output.toByteArray()), CoreConstants.ENCODING_UTF8);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    	
		return XmlUtil.getDocumentBuilder().parse(new InputSource(reader));
	}
	
	public static void main(String[] args) {
		
		ProcessArtifactsListRow row = new ProcessArtifactsListRow();
		row.setId("theid");
		row.addCell("cell content");
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		rows.setPage(23);
		rows.setRecords(10);
		rows.setTotal(34);

		rows.addRow(row);
		
		try {
			rows.getDocument();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}