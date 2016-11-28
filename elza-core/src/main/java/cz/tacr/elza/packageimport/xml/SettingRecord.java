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
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import java.io.IOException;
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
    private ScopeCode scopeCode;

    public SettingRecord() {
        setSettingsType(UISettings.SettingsType.RECORD);
    }

    public ScopeCode getScopeCode() {
        return scopeCode;
    }

    public void setScopeCode(final ScopeCode scopeCode) {
        this.scopeCode = scopeCode;
    }

    @Override
    public String getValue() {
        try {
            return objectMapper.writeValueAsString(scopeCode);
        } catch (JsonProcessingException e) {
            throw new SystemException(e, BaseCode.JSON_PARSE);
        }
    }

    @Override
    public void setValue(final String value) {
        try {
            scopeCode = objectMapper.readValue(value, ScopeCode.class);
        } catch (IOException e) {
            throw new SystemException(e, BaseCode.JSON_PARSE);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "scope-code")
    public static class ScopeCode {

        @XmlValue
        @XmlList
        private List<String> values;

        public List<String> getValues() {
            return values;
        }

        public void setValues(final List<String> values) {
            this.values = values;
        }
    }

}
