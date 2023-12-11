package cz.tacr.elza.packageimport.xml;

import java.io.IOException;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.ws.core.v1.daoservice.LevelImportSettings;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dao-import-level-settings", namespace = "dao-import-level")
public class SettingDaoImportLevel extends Setting {

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    final static public String NS_DAO_IMPORT_LEVEL = "dao-import-level";

    @XmlElement(name = "default", type = LevelImportSettings.class, namespace = NS_DAO_IMPORT_LEVEL, required = true)
    LevelImportSettings lis;

    public LevelImportSettings getLevelImportSettings() {
        return lis;
    }

    @Override
    void store(UISettings uiSettings) {
        try {
            uiSettings.setValue(objectMapper.writeValueAsString(lis));
        } catch (JsonProcessingException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    public static SettingDaoImportLevel newInstance(UISettings uis) {
        SettingDaoImportLevel sit = new SettingDaoImportLevel();
        try {
            sit.lis = objectMapper.readValue(uis.getValue(), LevelImportSettings.class);
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
        return sit;
    }
}
