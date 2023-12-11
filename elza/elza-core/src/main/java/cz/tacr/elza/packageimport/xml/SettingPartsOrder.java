package cz.tacr.elza.packageimport.xml;

import java.io.IOException;
import java.util.List;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "parts-order", namespace = "parts-order")
public class SettingPartsOrder extends Setting {

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @XmlElement(name = "part", type = Part.class, namespace = "parts-order")
    private List<Part> parts;


    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    public SettingPartsOrder() {
        super(UISettings.SettingsType.PARTS_ORDER.toString(),
                UISettings.EntityType.RULE);
    }

    @Override
    void store(UISettings uiSettings) {
        try {
            uiSettings.setValue(objectMapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    public static SettingPartsOrder newInstance(UISettings uis) {
        SettingPartsOrder spo = new SettingPartsOrder();
        try {
            spo.parts = objectMapper.readValue(uis.getValue(), SettingPartsOrder.class).getParts();
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
        return spo;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "part", namespace = "parts-order")
    public static class Part {

        @XmlAttribute(name = "code", required = true)
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
