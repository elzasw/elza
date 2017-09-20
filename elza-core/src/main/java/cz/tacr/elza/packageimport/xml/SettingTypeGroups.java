package cz.tacr.elza.packageimport.xml;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * VO SettingTypeGroups.
 *
 * @author Martin Šlapa
 * @since 22.3.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "type-groups", namespace = "type-groups")
public class SettingTypeGroups extends Setting {

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @XmlElements({
            @XmlElement(name = "default", type = Default.class, namespace = "type-groups", required = true),
            @XmlElement(name = "fund", type = Fund.class, namespace = "type-groups"),
    })
    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(final List<Item> items) {
        this.items = items;
    }

    public SettingTypeGroups() {
        setSettingsType(UISettings.SettingsType.TYPE_GROUPS);
        setEntityType(UISettings.EntityType.RULE);
    }

    @Override
    public String getValue() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    @Override
    public void setValue(final String value) {
        try {
            items = objectMapper.readValue(value, SettingTypeGroups.class).getItems();
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    @XmlTransient
    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class Item {

        @XmlElement(name = "group", required = true)
        private List<Group> groups;

        public List<Group> getGroups() {
            return groups;
        }

        public void setGroups(final List<Group> groups) {
            this.groups = groups;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "default", namespace = "type-groups")
    public static class Default extends Item {

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "fund", namespace = "type-groups")
    public static class Fund extends Item {

        @XmlAttribute(name = "fund-id", required = true)
        private Integer fundId;

        public Integer getFundId() {
            return fundId;
        }

        public void setFundId(final Integer fundId) {
            this.fundId = fundId;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "group")
    public static class Group {

        @XmlAttribute(name = "code", required = true)
        private String code;

        @XmlElement(name = "name", required = true)
        private String name;

        @XmlElement(name = "type", required = true)
        private List<Type> types;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public List<Type> getTypes() {
            return types;
        }

        public void setTypes(final List<Type> types) {
            this.types = types;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "type")
    public static class Type {

        @XmlAttribute(name = "code", required = true)
        private String code;

        @XmlAttribute(name = "width", required = true)
        private Integer width;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(final Integer width) {
            this.width = width;
        }
    }
}
