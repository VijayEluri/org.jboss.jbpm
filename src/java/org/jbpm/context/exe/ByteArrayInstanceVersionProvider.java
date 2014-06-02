package org.jbpm.context.exe;

import org.jbpm.context.exe.variableinstance.ByteArrayInstance;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.version.BPMInstanceVersionProvider;

@Service("jbpmByteArrayInstanceVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ByteArrayInstanceVersionProvider implements BPMInstanceVersionProvider<ByteArrayInstance> {
	
	@Override
	public Number getVersion(ByteArrayInstance byteArrayVar) {
		if (byteArrayVar instanceof VariableInstance) {
			return ((VariableInstance) byteArrayVar).version;
		}
		return null;
	}
}