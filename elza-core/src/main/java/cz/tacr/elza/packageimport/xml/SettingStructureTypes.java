package cz.tacr.elza.packageimport.xml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.xml.bind.annotation.*;
import java.io.IOException;
import java.util.List;


/**
 * VO SettingStructureTypes.
 *
 * @since 10.08.2018
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "allow-structure-types", namespace = "allow-structure-types")
public class SettingStructureTypes extends Setting {

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @XmlElements({
            @XmlElement(name = "type", type = SettingStructureTypes.Type.class, namespace = "allow-structure-types"),
    })
    private List<Type> items;

    public List<Type> getItems() {
        return items;
    }

    public void setItems(final List<Type> items) {
        this.items = items;
    }

    public SettingStructureTypes() {
        super(UISettings.SettingsType.STRUCTURE_TYPES.toString(),
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

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "type", namespace = "allow-structure-types")
    public static class Type {

        @XmlAttribute(name = "code", required = true)
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

    }

	public static SettingStructureTypes newInstance(UISettings uis) {
        try {
            return objectMapper.readValue(uis.getValue(), SettingStructureTypes.class);
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
	}
}
