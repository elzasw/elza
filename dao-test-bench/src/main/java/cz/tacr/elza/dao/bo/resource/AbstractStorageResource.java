package cz.tacr.elza.dao.bo.resource;

public abstract class AbstractStorageResource<T> {

	private T resource;

	public boolean isInitialized() {
		return resource != null;
	}

	public T get() {
		return resource;
	}

	public void init() throws Exception {
		resource = loadResource();
	}

	public T getOrInit() throws Exception {
		if (!isInitialized()) {
			init();
		}
		return get();
	}

	protected void clearCached() {
		resource = null;
	}

	protected abstract T loadResource() throws Exception;
}