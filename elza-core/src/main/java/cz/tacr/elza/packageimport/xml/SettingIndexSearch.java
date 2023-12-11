package cz.tacr.elza.packageimport.xml;

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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "index-search", namespace = "index-search")
public class SettingIndexSearch extends Setting {

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @XmlElement(name = "field", type = Field.class, namespace = "index-search")
    private List<Field> fields;

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public SettingIndexSearch() {
        super(UISettings.SettingsType.INDEX_SEARCH.toString(),
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

    public static SettingIndexSearch newInstance(UISettings uis) {
        SettingIndexSearch sis = new SettingIndexSearch();
        try {
            sis.fields = objectMapper.readValue(uis.getValue(), SettingIndexSearch.class).getFields();
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
        return sis;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "field", namespace = "index-search")
    public static class Field {

        @XmlAttribute(name = "name", required = true)
        private String name;

        @XmlAttribute(name = "boost")
        private Float boost;

        @XmlAttribute(name = "boost-exact")
        private Float boostExact;

        @XmlAttribute(name = "boost-trans-exact")
        private Float boostTransExact;

        @XmlAttribute(name = "transliterate")
        private Boolean transliterate;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Float getBoost() {
            return boost;
        }

        public void setBoost(Float boost) {
            this.boost = boost;
        }

        public Float getBoostExact() {
            return boostExact;
        }

        public void setBoostExact(Float boostExact) {
            this.boostExact = boostExact;
        }

        public Float getBoostTransExact() {
            return boostTransExact;
        }

        public void setBoostTransExact(Float boostTransExact) {
            this.boostTransExact = boostTransExact;
        }

        public Boolean getTransliterate() {
            return transliterate;
        }

        public void setTransliterate(Boolean transliterate) {
            this.transliterate = transliterate;
        }
    }
}
