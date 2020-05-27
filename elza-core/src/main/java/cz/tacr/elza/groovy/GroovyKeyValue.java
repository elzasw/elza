package cz.tacr.elza.groovy;

import javax.validation.constraints.NotNull;

public class GroovyKeyValue {

    private String key;
    private String value;

    public GroovyKeyValue(@NotNull final String key, @NotNull final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
