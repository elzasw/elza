package cz.tacr.elza.dao.bo.resource;

public abstract class AbstractStorageResource<T> {

	private T resource;

	public final boolean isInitialized() {
		return resource != null;
	}

	public void init() throws Exception {
		if (isInitialized()) {
			return;
		}
		resource = loadResource();
	}

	public T getResource() {
		return resource;
	}

	protected final void clearCached() {
		resource = null;
	}

	protected abstract T loadResource() throws Exception;
}