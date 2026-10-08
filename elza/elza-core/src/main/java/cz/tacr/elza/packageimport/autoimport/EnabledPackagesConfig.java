package cz.tacr.elza.packageimport.autoimport;

import java.util.List;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

/**
 * The configuration key naming the packages of the {@code dpkg} directory to import at startup
 * although they are not installed yet (see {@link AutoImportSelection}): a list in YAML or a
 * comma-separated value. Read through the {@link Environment}, because the index configuration
 * needs it before the configuration beans exist.
 */
public final class EnabledPackagesConfig {

    public static final String KEY = "elza.packages.enabled";

    private EnabledPackagesConfig() {
    }

    /**
     * @return the listed codes, or {@code null} when the key is not set
     */
    public static List<String> read(final Environment environment) {
        return Binder.get(environment).bind(KEY, Bindable.listOf(String.class)).orElse(null);
    }
}
