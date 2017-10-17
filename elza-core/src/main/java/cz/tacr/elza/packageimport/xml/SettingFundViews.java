package cz.tacr.elza.packageimport.xml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlTransient;
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
@XmlType(name = "fund-views", namespace = "fund-views")
public class SettingFundViews extends Setting {

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @XmlElements({
            @XmlElement(name = "default", type = Default.class, namespace = "fund-views", required = true),
            @XmlElement(name = "fund", type = Fund.class, namespace = "fund-views"),
    })
    private List<Item> items;

    public SettingFundViews() {
        setSettingsType(UISettings.SettingsType.FUND_VIEW);
        setEntityType(UISettings.EntityType.RULE);
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(final List<Item> items) {
        this.items = items;
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
            items = objectMapper.readValue(value, SettingFundViews.class).getItems();
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    @XmlTransient
    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class Item {

        @XmlElement(name = "title", required = true)
        private String title;

        @XmlElement(name = "tree", required = true)
        private Tree tree;

        @XmlElement(name = "strict-mode", defaultValue = "false")
        private Boolean strictMode;

        @XmlElement(name = "accordion-left", required = true)
        private AccordionLeft accordionLeft;

        @XmlElement(name = "accordion-right", required = true)
        private AccordionRight accordionRight;

        @XmlElement(name = "hierarchy-level", required = true)
        private List<HierarchyLevel> hierarchyLevels;

        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title = title;
        }

        public Tree getTree() {
            return tree;
        }

        public void setTree(final Tree tree) {
            this.tree = tree;
        }

        public Boolean getStrictMode() {
            return strictMode;
        }

        public void setStrictMode(final Boolean strictMode) {
            this.strictMode = strictMode;
        }

        public AccordionLeft getAccordionLeft() {
            return accordionLeft;
        }

        public void setAccordionLeft(final AccordionLeft accordionLeft) {
            this.accordionLeft = accordionLeft;
        }

        public AccordionRight getAccordionRight() {
            return accordionRight;
        }

        public void setAccordionRight(final AccordionRight accordionRight) {
            this.accordionRight = accordionRight;
        }

        public List<HierarchyLevel> getHierarchyLevels() {
            return hierarchyLevels;
        }

        public void setHierarchyLevels(final List<HierarchyLevel> hierarchyLevels) {
            this.hierarchyLevels = hierarchyLevels;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "default", namespace = "fund-views")
    public static class Default extends Item {

    }

    @XmlAccessorType(XmlAccessType.FIELD)
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
    @XmlType(name = "hierarchy-level")
    public static class HierarchyLevel {

        @XmlAttribute(name = "code", required = true)
        private String code;

        @XmlElement(name = "hierarchy-item", required = true)
        private List<HierarchyItem> hierarchyItems;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public List<HierarchyItem> getHierarchyItems() {
            return hierarchyItems;
        }

        public void setHierarchyItems(final List<HierarchyItem> hierarchyItems) {
            this.hierarchyItems = hierarchyItems;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "hierarchy-item")
    public static class HierarchyItem {

        @XmlAttribute(name = "code", required = true)
        private String code;

        @XmlElement(name = "icon", required = true)
        private String icon;

        @XmlElement(name = "separator-first", required = true)
        private String separatorFirst;

        @XmlElement(name = "separator-other", required = true)
        private String separatorOther;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(final String icon) {
            this.icon = icon;
        }

        public String getSeparatorFirst() {
            return separatorFirst;
        }

        public void setSeparatorFirst(final String separatorFirst) {
            this.separatorFirst = separatorFirst;
        }

        public String getSeparatorOther() {
            return separatorOther;
        }

        public void setSeparatorOther(final String separatorOther) {
            this.separatorOther = separatorOther;
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "tree")
    public static class Tree {

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

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "accordion-left")
    public static class AccordionLeft {

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

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "accordion-right")
    public static class AccordionRight {

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
