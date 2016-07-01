package cz.tacr.elza.bulkaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.tacr.elza.utils.Yaml;


/**
 * Implementace konfigurace hromadné akce.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
public class BulkActionConfig implements cz.tacr.elza.api.vo.BulkActionConfig {

    private String code;

    @JsonIgnore
    private Yaml yaml = new Yaml();

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public String getConfiguration() {
        return yaml.toString();
    }

    @Override
    public void setConfiguration(final String configuration) {
        try {
            yaml.load(configuration);
        } catch (Yaml.YAMLInvalidContentException e) {
            throw new IllegalArgumentException("Formát konfigurace není platný", e);
        }
    }

    public Yaml getYaml() {
        return yaml;
    }

    public void setYaml(Yaml yaml) {
        this.yaml = yaml;
    }

    public String getString(final String key) {
        return yaml.getString(key, null);
    }

    @Override
    public String toString() {
        return "BulkActionConfig{" +
                "code='" + code + '\'' +
                '}';
    }
}
