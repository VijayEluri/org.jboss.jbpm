package com.idega.jbpm.bundle;

import java.io.IOException;

import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.xml.XPathUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 *          Last modified: $Date: 2009/01/25 15:36:31 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class ProcessBundleFactory {

	public ProcessBundle createProcessBundle(ProcessBundleResources resources,
			Integer version) {

		Document bundleConfigXML = resources.getConfiguration();

		XPathUtil xput = new XPathUtil("//processBundle");
		Element processBundleE = xput.getNode(bundleConfigXML);

		String beanIdentifierAttribute = "beanIdentifier";
		String processManagerTypeAttribute = "processManagerType";
		String bundleBeanIdentifier = processBundleE
				.getAttribute(beanIdentifierAttribute); // this can be changed
		// to bundle type
		String processManagerType = processBundleE
				.getAttribute(processManagerTypeAttribute);

		if (StringUtil.isEmpty(bundleBeanIdentifier))
			throw new IllegalArgumentException("Wrong configuration file: no "
					+ beanIdentifierAttribute
					+ " attribute provided at the processbundle element");

		ProcessBundle procBundle = (ProcessBundle) ELUtil.getInstance()
				.getBean(bundleBeanIdentifier);
		procBundle.setBundleResources(resources);

		if (!StringUtil.isEmpty(processManagerType)) {
			procBundle.setProcessManagerType(processManagerType);
		}

		if (version != null) {

			try {
				ProcessDefinition pd = procBundle.getProcessDefinition();
				pd.setVersion(version);

			} catch (IOException e) {
				throw new RuntimeException(
						"Failed to resolve process definition from process bundle, bundleBeanIdentifier="
								+ bundleBeanIdentifier, e);
			}
		}

		return procBundle;
	}

	public ProcessBundle createProcessBundle(ProcessBundleResources resources) {

		return createProcessBundle(resources, null);
	}
}