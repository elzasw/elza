package cz.tacr.elza.packageimport.xml;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Menu specific settings
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "menu", namespace = "menu")
public class SettingMenu extends Setting {

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    /**
     * Single menu option
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "menu-option", namespace = "menu")
    public static class MenuOption {

        @XmlAttribute(name = "name", required = true)
        String name;

        @XmlValue
        String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    @XmlElement(name = "menu-option", type = MenuOption.class, namespace = "menu")
    List<MenuOption> options;

    public SettingMenu() {
        super(UISettings.SettingsType.MENU.toString(),
                // bez vazby na konkretni typ entity
                null);
    }

    public List<MenuOption> getOptions() {
        return options;
    }

    public void setOptions(List<MenuOption> options) {
        this.options = options;
    }

    @Override
    void store(UISettings uiSettings) {
        try {
            uiSettings.setValue(objectMapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    public static SettingMenu newInstance(UISettings uis) {
        SettingMenu settsm = new SettingMenu();
        try {
            settsm.options = objectMapper.readValue(uis.getValue(), SettingMenu.class).getOptions();
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
        return settsm;
    }
}
