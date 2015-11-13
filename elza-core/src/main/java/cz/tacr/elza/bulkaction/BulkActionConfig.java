package cz.tacr.elza.bulkaction;

import java.io.IOException;
import java.io.StringReader;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cz.tacr.elza.bulkaction.yaml.YamlProperties;


/**
 * Implementace konfigurace hromadné akce.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
public class BulkActionConfig implements cz.tacr.elza.api.vo.BulkActionConfig {

    private String code;

    @JsonIgnore
    private YamlProperties yaml = new YamlProperties();

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
            yaml.load(new StringReader(configuration));
        } catch (IOException e) {
            throw new IllegalArgumentException("Formát konfigurace není platný", e);
        }
    }

    public YamlProperties getYaml() {
        return yaml;
    }

    public void setYaml(YamlProperties yaml) {
        this.yaml = yaml;
    }

    public Object getProperty(String key) {
        return yaml.getProperty(key);
    }

    @Override
    public String toString() {
        return "BulkActionConfig{" +
                "code='" + code + '\'' +
                '}';
    }
}
