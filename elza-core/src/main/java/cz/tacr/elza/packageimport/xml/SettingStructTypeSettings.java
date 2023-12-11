package cz.tacr.elza.packageimport.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "structure-type-settings", namespace = "structure-type-settings")
public class SettingStructTypeSettings extends Setting {
    
    public static final String NS_STRUCT_TYPE_SETTINGS = "structure-type-settings"; 
    
    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "property", namespace = NS_STRUCT_TYPE_SETTINGS)
    public static class Property {

        /**
         * Code of the property
         * 
         * e.g. prefixSeparator
         */
        @XmlAttribute(name = "code", required = true)
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }
        
        @XmlValue()
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }        
    }
    
    /**
     * Code of struct type
     * 
     * Code includes prefix STRUCT_TYPE_
     */
    @XmlAttribute(name="code")
    @JsonIgnore
    private String code;
    
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    //@XmlElementWrapper(name="properties", namespace = NS_STRUCT_TYPE_SETTINGS)
    @XmlElement(name="property", namespace = NS_STRUCT_TYPE_SETTINGS)
    private List<Property> properties = new ArrayList<>();

    public SettingStructTypeSettings() {
        super(UISettings.SettingsType.STRUCT_TYPE_.toString(),
                UISettings.EntityType.FUND);
    }    

    public List<Property> getProperties() {
        return properties;
    }
    
    public String getPropertyValue(String code) {
        if(properties!=null) {
            for(Property p:properties) {
                if(code.equals(p.getCode())) {
                    return p.getValue();
                }
            }
        }
        return null;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    @Override
    void store(UISettings uiSettings) {
        // code has to be valid and exists
        Validate.isTrue(StringUtils.isNotEmpty(code));
        // extend type with code
        String settType = this.code;
        uiSettings.setSettingsType(settType);
        try {
            String value = objectMapper.writeValueAsString(this);
            uiSettings.setValue(value);
        } catch (JsonProcessingException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    public static SettingStructTypeSettings newInstance(UISettings uis, String code) {        
        try {
            SettingStructTypeSettings ssts = objectMapper.readValue(uis.getValue(), SettingStructTypeSettings.class);
            ssts.setCode(code);
            return ssts;
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }        
    }
}
