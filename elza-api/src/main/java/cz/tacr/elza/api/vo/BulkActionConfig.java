package cz.tacr.elza.api.vo;

import java.io.Serializable;


/**
 * Konfigurace hromadné akce.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
public interface BulkActionConfig extends Serializable {

    /**
     * Vrací kód hromadné akce.
     *
     * @return kód hromadné akce
     */
    String getCode();


    /**
     * Nastavuje kód hromadné akce.
     *
     * @param code kód hromadné akce
     */
    void setCode(String code);


    /**
     * Vrací konfiguraci formou textu - předpoklad je formát YAML.
     *
     * @return konfigurace
     */
    String getConfiguration();


    /**
     * Nastavuje konfiguraci.
     *
     * @param configuration konfigurace - text ve formátu YAML
     */
    void setConfiguration(String configuration);

}
