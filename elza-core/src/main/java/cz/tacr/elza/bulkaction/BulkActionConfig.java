package cz.tacr.elza.bulkaction;

import cz.tacr.elza.utils.Yaml;


/**
 * Konfigurace hromadné akce.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
public class BulkActionConfig {

    private String code;

    private Yaml yaml = new Yaml();

    /**
     * Vrací kód hromadné akce.
     *
     * @return kód hromadné akce
     */
    public String getCode() {
        return code;
    }

    /**
     * Nastavuje kód hromadné akce.
     *
     * @param code kód hromadné akce
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * Vrací konfiguraci formou textu - předpoklad je formát YAML.
     *
     * @return konfigurace
     */
    public String getConfiguration() {
        return yaml.toString();
    }

    /**
     * Nastavuje konfiguraci.
     *
     * @param configuration konfigurace - text ve formátu YAML
     */
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

    public void setYaml(final Yaml yaml) {
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
