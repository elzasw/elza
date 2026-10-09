package cz.tacr.elza.packageimport.xml;

import java.io.IOException;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Defaults of the new output dialog for a rule set.
 *
 * <pre>
 * &lt;output-defaults settings-type="OUTPUT_DEFAULTS" entity-type="RULE"&gt;
 *     &lt;ns:output-filter code="ZP_ACCESS_RESTRICT"/&gt;
 * &lt;/output-defaults&gt;
 * </pre>
 *
 * The setting is layered: a package deeper in dependency order replaces it, and a setting without
 * {@code output-filter} removes the default stated by the customized package.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "output-defaults", namespace = "output-defaults")
public class SettingOutputDefaults extends Setting {

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    /**
     * Reference to an output filter of the rule set by its code.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "output-filter-ref", namespace = "output-defaults")
    public static class OutputFilterRef {

        @XmlAttribute(name = "code", required = true)
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    @XmlElement(name = "output-filter", namespace = "output-defaults")
    private OutputFilterRef outputFilter;

    public SettingOutputDefaults() {
        super(UISettings.SettingsType.OUTPUT_DEFAULTS.toString(), UISettings.EntityType.RULE);
    }

    public OutputFilterRef getOutputFilter() {
        return outputFilter;
    }

    public void setOutputFilter(OutputFilterRef outputFilter) {
        this.outputFilter = outputFilter;
    }

    /**
     * @return code of the default output filter, null when the setting states none
     */
    public String outputFilterCode() {
        return outputFilter != null ? outputFilter.getCode() : null;
    }

    @Override
    void store(UISettings uiSettings) {
        try {
            uiSettings.setValue(objectMapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    public static SettingOutputDefaults newInstance(UISettings uis) {
        SettingOutputDefaults sod = new SettingOutputDefaults();
        try {
            sod.outputFilter = objectMapper.readValue(uis.getValue(), SettingOutputDefaults.class).getOutputFilter();
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
        return sod;
    }
}
