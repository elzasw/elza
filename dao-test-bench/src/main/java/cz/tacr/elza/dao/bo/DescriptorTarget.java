package cz.tacr.elza.dao.bo;

import cz.tacr.elza.dao.descriptor.Descriptor;

public interface DescriptorTarget<T extends Descriptor> {

	T getDescriptor();

	void saveDescriptor();

	default boolean isValidDescriptor() {
		try {
			return getDescriptor() != null;
		} catch (Throwable t) {
			return false;
		}
	}
}
