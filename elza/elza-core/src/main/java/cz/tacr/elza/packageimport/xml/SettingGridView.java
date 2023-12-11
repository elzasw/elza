package cz.tacr.elza.packageimport.xml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.IOException;
import java.util.List;


/**
 * VO SettingGridView.
 *
 * @since 19.01.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "grid-view", namespace = "grid-view")
public class SettingGridView extends Setting {

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @XmlElement(name = "item-type", type = ItemType.class, namespace = "grid-view")
    private List<ItemType> itemTypes;

    @XmlAttribute(name = "code", required = true)
    @JsonIgnore
    private String code;

    public List<ItemType> getItemTypes() {
        return itemTypes;
    }

    public void setItemTypes(final List<ItemType> itemTypes) {
        this.itemTypes = itemTypes;
    }

    public SettingGridView() {
        super(UISettings.SettingsType.GRID_VIEW.toString(),
              // bez vazby na konkretni typ entity
              null);
    }

    @Override
    void store(UISettings uiSettings) {
        try {
            uiSettings.setValue(objectMapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "item-type", namespace = "grid-view")
    public static class ItemType {

        @XmlAttribute(name = "code", required = true)
        private String code;

        @XmlElement(name = "show-default")
        private Boolean showDefault;

        @XmlElement(name = "width")
        private Integer width;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public Boolean getShowDefault() {
            return showDefault;
        }

        public void setShowDefault(final Boolean showDefault) {
            this.showDefault = showDefault;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(final Integer width) {
            this.width = width;
        }
    }

	public static SettingGridView newInstance(UISettings uis) {
		SettingGridView sgv = new SettingGridView();
        try {
            sgv.itemTypes = objectMapper.readValue(uis.getValue(), SettingGridView.class).getItemTypes();
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
		return sgv;
	}

}
