package cz.tacr.elza.dao.bo.resource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.yaml.snakeyaml.Yaml;

public abstract class YamlResource<T> extends AbstractStorageResource<T> {

	protected final static Yaml YAML_INSTANCE = new Yaml();

	private final Class<T> type;

	protected YamlResource(Class<T> type) {
		this.type = type;
	}

	public void save() throws IOException {
		if (!isInitialized()) {
			throw new IllegalStateException("resource not initialized");
		}
		try (BufferedWriter bw = Files.newBufferedWriter(getResourcePath(), StandardOpenOption.WRITE,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			YAML_INSTANCE.dump(getResource(), bw);
		}
	}

	@Override
	protected T loadResource() throws Exception {
		try (InputStream is = Files.newInputStream(getResourcePath())) {
			T resource = YAML_INSTANCE.loadAs(is, type);
			if (resource == null) {
				// empty file
				resource = type.newInstance();
			}
			return resource;
		}
	}

	protected abstract Path getResourcePath();
}