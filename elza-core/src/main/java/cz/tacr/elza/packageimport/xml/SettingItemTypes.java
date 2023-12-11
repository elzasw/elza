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
@XmlType(name = "item-types", namespace = "item-types")
public class SettingItemTypes extends Setting{

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @XmlElement(name = "item-type", type = ItemType.class, namespace = "item-types")
    private List<ItemType> itemTypes;

    public List<ItemType> getItemTypes() {
        return itemTypes;
    }

    public void setItemTypes(List<ItemType> itemTypes) {
        this.itemTypes = itemTypes;
    }

    public SettingItemTypes() {
        super(UISettings.SettingsType.ITEM_TYPES.toString(),
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

    public static SettingItemTypes newInstance(UISettings uis) {
        SettingItemTypes sit = new SettingItemTypes();
        try {
            sit.itemTypes = objectMapper.readValue(uis.getValue(), SettingItemTypes.class).getItemTypes();
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
        return sit;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "item-type", namespace = "item-types")
    public static class ItemType {

        @XmlAttribute(name = "code", required = true)
        private String code;

        @XmlElement(name = "position")
        private Integer position;

        @XmlElement(name = "width")
        private Integer width;

        @XmlElement(name = "part-type")
        private String partType;

        @XmlElement(name = "geo-search-item-type")
        private String geoSearchItemType;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Integer getPosition() {
            return position;
        }

        public void setPosition(Integer position) {
            this.position = position;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public String getPartType() {
            return partType;
        }

        public void setPartType(String partType) {
            this.partType = partType;
        }

        public String getGeoSearchItemType() {
            return geoSearchItemType;
        }

        public void setGeoSearchItemType(String geoSearchItemType) {
            this.geoSearchItemType = geoSearchItemType;
        }
    }
}
