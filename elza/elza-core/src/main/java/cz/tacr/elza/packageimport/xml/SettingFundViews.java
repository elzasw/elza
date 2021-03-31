package cz.tacr.elza.packageimport.xml;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * VO SettingFundViews.
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fund-views")
public class SettingFundViews extends Setting {

    final static String NS_FUND_VIEWS = "fund-views";

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @XmlElement(name = "default", type = FundView.class, namespace = NS_FUND_VIEWS, required = true)
    private FundView fundView;

    public SettingFundViews() {
        super(UISettings.SettingsType.FUND_VIEW.toString(),
        		UISettings.EntityType.RULE);
    }

    public FundView getFundView() {
        return fundView;
    }

    public void setFundView(FundView fundView) {
        this.fundView = fundView;
    }

    @Override
    void store(UISettings uiSettings) {
        try {
            uiSettings.setValue(objectMapper.writeValueAsString(fundView));
        } catch (JsonProcessingException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "fund-view", namespace = NS_FUND_VIEWS)
    public static class FundView {

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

        @XmlElement(name = "hierarchy", required = true)
        private HierarchyXml hierarchy;

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

        public HierarchyXml getHierarchy() {
            return hierarchy;
        }

        public void setHierarchy(final HierarchyXml hierarchyLevels) {
            this.hierarchy = hierarchyLevels;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "hierarchy")
    public static class HierarchyXml {

        @XmlAttribute(name = "type-code", required = true)
        private String code;

        @XmlAttribute(name = "default-separator", required = true)
        private String defaultSeparator;

        @XmlElement(name = "level", required = true)
        private List<HierarchyItem> levels;

        public String getTypeCode() {
            return code;
        }

        public void setTypeCode(final String code) {
            this.code = code;
        }

        public List<HierarchyItem> getLevels() {
            return levels;
        }

        public void setLevels(final List<HierarchyItem> levels) {
            this.levels = levels;
        }

        public String getDefaultSeparator() {
            return defaultSeparator;
        }

        public void setDefaultSeparator(String defaultSeparator) {
            this.defaultSeparator = defaultSeparator;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "separator", namespace = NS_FUND_VIEWS)
    public static class Separator {
        @XmlAttribute(name = "parent", required = true)
        private String parent;

        @XmlValue
        private String value;

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "level", namespace = NS_FUND_VIEWS)
    public static class HierarchyItem {

        @XmlAttribute(name = "spec-code", required = true)
        private String code;

        @XmlElement(name = "icon", required = true)
        private String icon;

        @XmlElementWrapper(name = "separators")
        @XmlElement(name = "separator", required = false)
        private List<Separator> separators;

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

        public List<Separator> getSeparators() {
            return separators;
        }

        public void setSeparators(final List<Separator> separators) {
            this.separators = separators;
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "title-config")
    public static class TitleConfig {

        @XmlAttribute(name = "separator", required = false)
        private String separator;

        @XmlElement(name = "item")
        private List<TitleConfigItem> items;

        public String getSeparator() {
            return separator;
        }

        public void setSeparator(String separator) {
            this.separator = separator;
        }

        public List<TitleConfigItem> getItems() {
            return items;
        }

        public void setItems(final List<TitleConfigItem> values) {
            this.items = values;
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "tree", namespace = NS_FUND_VIEWS)
    public static class Tree extends TitleConfig {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "accordion-left", namespace = NS_FUND_VIEWS)
    public static class AccordionLeft extends TitleConfig {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "accordion-right", namespace = NS_FUND_VIEWS)
    public static class AccordionRight extends TitleConfig {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "item")
    public static class TitleConfigItem {

        @XmlAttribute(name = "max-count", required = false)
        private Integer maxCount;

        @XmlAttribute(name = "type", required = true)
        private String type;

        @XmlElement(name = "spec", required = false)
        private List<ItemSpec> specs;

        public Integer getMaxCount() {
            return maxCount;
        }

        public void setMaxCount(Integer maxCount) {
            this.maxCount = maxCount;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<ItemSpec> getSpecs() {
            return specs;
        }

        public void setSpecs(List<ItemSpec> specs) {
            this.specs = specs;
        }

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "spec")
    public static class ItemSpec {

        @XmlAttribute(name = "type", required = true)
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

    }

	public static SettingFundViews newInstance(UISettings uis) {
		SettingFundViews sfv = new SettingFundViews();
        try {
            sfv.fundView = objectMapper.readValue(uis.getValue(), FundView.class);
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
		return sfv;
	}
}
