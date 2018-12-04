package cz.tacr.elza.packageimport.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * VO SettingFundIssues.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fund-issues")
public class SettingFundIssues extends Setting {

    public static final String NS_FUND_ISSUES = "fund-issues";

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    // --- fields ---

    /*
    @XmlElement(name = "issue-states", type = IssueStates.class, namespace = NS_FUND_ISSUES)
    private IssueStates issueStates;
    @XmlElement(name = "issue-types", type = IssueTypes.class, namespace = NS_FUND_ISSUES)
    private IssueTypes issueTypes;
    */

    @XmlJavaTypeAdapter(IssueStatesAdapter.class)
    @XmlElement(name = "issue-states", namespace = NS_FUND_ISSUES)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> issueStateIcons;

    @XmlJavaTypeAdapter(IssueTypesAdapter.class)
    @XmlElement(name = "issue-types", namespace = NS_FUND_ISSUES)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> issueTypeColors;

    // --- getters/setters ---

    public Map<String, String> getIssueStateIcons() {
        return issueStateIcons;
    }

    public void setIssueStateIcons(Map<String, String> issueStateIcons) {
        this.issueStateIcons = issueStateIcons;
    }

    public Map<String, String> getIssueTypeColors() {
        return issueTypeColors;
    }

    public void setIssueTypeColors(Map<String, String> issueTypeColors) {
        this.issueTypeColors = issueTypeColors;
    }

    // --- constructor ---

    public SettingFundIssues() {
        setSettingsType(UISettings.SettingsType.FUND_ISSUES);
        setEntityType(UISettings.EntityType.RULE);
    }

    // --- methods ---

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
            SettingFundIssues settingFundIssues = objectMapper.readValue(value, SettingFundIssues.class);
            this.issueStateIcons = settingFundIssues.getIssueStateIcons();
            this.issueTypeColors = settingFundIssues.getIssueTypeColors();
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    // --- classes ---

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "issue-states", namespace = NS_FUND_ISSUES)
    public static class IssueStates {

        // --- fields ---

        @XmlElement(name = "issue-state", namespace = NS_FUND_ISSUES)
        private List<IssueState> issueStates = new ArrayList<>();

        // --- getters/setters ---

        public List<IssueState> getIssueStates() {
            return issueStates;
        }

        public void setIssueStates(List<IssueState> issueStates) {
            this.issueStates = issueStates;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "issue-state", namespace = NS_FUND_ISSUES)
    public static class IssueState {

        // --- fields ---

        @XmlAttribute(name = "code", required = true)
        private String code;

        @XmlAttribute(name = "icon", required = true)
        private String icon;

        // --- getters/setters ---

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        // --- constructor ---

        private IssueState() {
            // required by JAXB
        }

        public IssueState(String code, String icon) {
            this.code = code;
            this.icon = icon;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "issue-types", namespace = NS_FUND_ISSUES)
    public static class IssueTypes {

        // --- fields ---

        @XmlElement(name = "issue-type", namespace = NS_FUND_ISSUES)
        private List<IssueType> issueTypes = new ArrayList<>();

        // --- getters/setters ---

        public List<IssueType> getIssueTypes() {
            return issueTypes;
        }

        public void setIssueTypes(List<IssueType> issueTypes) {
            this.issueTypes = issueTypes;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "issue-type", namespace = NS_FUND_ISSUES)
    public static class IssueType {

        // --- fields ---

        @XmlAttribute(name = "code", required = true)
        private String code;

        @XmlAttribute(name = "color", required = true)
        private String color;

        // --- getters/setters ---

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        // --- constructor ---

        private IssueType() {
            // required by JAXB
        }

        public IssueType(String code, String color) {
            this.code = code;
            this.color = color;
        }
    }

    public static class IssueStatesAdapter extends XmlAdapter<IssueStates, Map<String, String>> {

        @Override
        public Map<String, String> unmarshal(IssueStates issueStates) {
            if (issueStates == null || issueStates.getIssueStates() == null) {
                return null;
            }
            Map<String, String> issueStatesIcons = new LinkedHashMap();
            for (IssueState issueState : issueStates.getIssueStates()) {
                issueStatesIcons.put(issueState.getCode(), issueState.getIcon());
            }
            return issueStatesIcons;
        }

        @Override
        public IssueStates marshal(Map<String, String> issueStatesIcons) {
            if (issueStatesIcons == null) {
                return null;
            }
            IssueStates issueStates = new IssueStates();
            issueStates.setIssueStates(issueStatesIcons.entrySet().stream().map(entry -> new IssueState(entry.getKey(), entry.getValue())).collect(Collectors.toList()));
            return issueStates;
        }
    }

    public static class IssueTypesAdapter extends XmlAdapter<IssueTypes, Map<String, String>> {

        @Override
        public Map<String, String> unmarshal(IssueTypes issueTypes) {
            if (issueTypes == null || issueTypes.getIssueTypes() == null) {
                return null;
            }
            Map<String, String> issueTypesColors = new LinkedHashMap();
            for (IssueType issueType : issueTypes.getIssueTypes()) {
                issueTypesColors.put(issueType.getCode(), issueType.getColor());
            }
            return issueTypesColors;
        }

        @Override
        public IssueTypes marshal(Map<String, String> issueTypesColors) {
            if (issueTypesColors == null) {
                return null;
            }
            IssueTypes issueTypes = new IssueTypes();
            issueTypes.setIssueTypes(issueTypesColors.entrySet().stream().map(entry -> new IssueType(entry.getKey(), entry.getValue())).collect(Collectors.toList()));
            return issueTypes;
        }
    }
}
