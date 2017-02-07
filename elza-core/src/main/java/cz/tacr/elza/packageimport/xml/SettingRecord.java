package cz.tacr.elza.packageimport.xml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


/**
 * VO SettingFundViews.
 *
 * @author Martin Šlapa
 * @since 22.3.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "record")
public class SettingRecord extends Setting {

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @XmlElement(name = "scope-code", required = true)
    private List<ScopeCode> scopeCodes;

    public SettingRecord() {
        setSettingsType(UISettings.SettingsType.RECORD);
    }

    public List<ScopeCode> getScopeCodes() {
        return scopeCodes;
    }

    public void setScopeCodes(final List<ScopeCode> scopeCodes) {
        this.scopeCodes = scopeCodes;
    }

    @Override
    public String getValue() {
        try {
            return objectMapper.writeValueAsString(scopeCodes);
        } catch (JsonProcessingException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    @Override
    public void setValue(final String value) {
        try {
            scopeCodes = Arrays.asList(objectMapper.readValue(value, ScopeCode[].class));
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "scope-code")
    public static class ScopeCode {

        @XmlValue
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

}
