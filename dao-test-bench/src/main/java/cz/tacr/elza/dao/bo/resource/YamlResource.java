package cz.tacr.elza.dao.bo.resource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public abstract class YamlResource<T> extends AbstractStorageResource<T> {

    protected final static Yaml YAML_INSTANCE;

    static {
        NotNullRepresenter representer = new NotNullRepresenter();
        YAML_INSTANCE = new Yaml(representer);
    }

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
			YAML_INSTANCE.dump(get(), bw);
		}
	}

	@Override
	protected T loadResource() throws Exception {
		try (InputStream is = Files.newInputStream(getResourcePath())) {
			T resource = YAML_INSTANCE.loadAs(is, type);
			if (resource == null) {
				resource = createEmptyResource();
			}
			return resource;
		}
	}

	public abstract Path getResourcePath();

	protected abstract T createEmptyResource() throws Exception;

	private static class NotNullRepresenter extends Representer {
        public NotNullRepresenter() {
            super(new DumperOptions());
        }

        @Override
		protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
			if (propertyValue == null) {
				return null;
			} else {
				return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
			}
		}
	}
}