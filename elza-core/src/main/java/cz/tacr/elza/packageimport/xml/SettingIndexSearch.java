package cz.tacr.elza.packageimport.xml;

import static cz.tacr.elza.domain.ApCachedAccessPoint.FIELD_ACCESSPOINT_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.INDEX;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.PREFIX_PREF;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.PREF_INDEX;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.SEPARATOR;

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
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Fields every entity has whatever rules describe it: the entity id and the names produced by
     * the index rules of its parts (any part and the preferred one). A package states the fields of
     * its own item and part types and may override these.
     */
    public static SettingIndexSearch coreDefaults() {
        SettingIndexSearch sis = new SettingIndexSearch();
        sis.fields = List.of(
                Field.of(FIELD_ACCESSPOINT_ID, null, 100.0f, null, null, false),
                Field.of(PREF_INDEX, 10.0f, 50.0f, 40.0f, 800.0f, true),
                Field.of(INDEX, 1.0f, 30.0f, 20.0f, 50.0f, true));
        return sis;
    }

    /**
     * Merges the layers; a field of a later layer replaces the field of the same name as a whole.
     *
     * @param layers configurations, lowest precedence first
     */
    public static SettingIndexSearch merge(List<SettingIndexSearch> layers) {
        Map<String, Field> fields = new LinkedHashMap<>();
        for (SettingIndexSearch layer : layers) {
            if (layer.fields != null) {
                layer.fields.forEach(field -> fields.put(field.getName(), field));
            }
        }
        SettingIndexSearch sis = new SettingIndexSearch();
        sis.fields = new ArrayList<>(fields.values());
        return sis;
    }

    /**
     * @return configuration of the field, null when not configured
     */
    @Nullable
    public Field findField(String name) {
        if (fields != null) {
            for (Field field : fields) {
                if (field.getName().equals(name)) {
                    return field;
                }
            }
        }
        return null;
    }

    /**
     * Item fields searched by the text search in the area - the configured fields other than the
     * entity id, the index fields ({@code index}, {@code pref_index}, {@code <part type>_index}) and
     * the fields of an index type ({@link #indexTypeFields(String)}).
     *
     * @param area null - any part: all item fields; {@code pref} - the preferred part: the fields
     *             {@code pref_<item type>}; a part type code in lower case - names of the part type:
     *             the fields {@code pref_<item type>} and the fields of the part type
     *             ({@code part-type})
     * @return fields in the order of the configuration
     */
    public List<Field> searchedItemFields(@Nullable String area) {
        List<Field> result = new ArrayList<>();
        if (fields == null) {
            return result;
        }
        for (Field field : fields) {
            String name = field.getName();
            if (name.equals(FIELD_ACCESSPOINT_ID) || name.equals(INDEX) || name.endsWith(SEPARATOR + INDEX)
                    || field.getIndexType() != null) {
                continue;
            }
            boolean pref = field.preferredPartOnly();
            boolean searched;
            if (area == null) {
                searched = true;
            } else if (area.equals(PREFIX_PREF)) {
                searched = pref;
            } else {
                searched = pref || area.equalsIgnoreCase(field.getPartType());
            }
            if (searched) {
                result.add(field);
            }
        }
        return result;
    }

    /**
     * Fields of an index type (attribute {@code index-type}): an index of the index rules other than
     * the display name, e.g. {@code REAL_NAME}, matched as a whole against the whole search text.
     * They take no part in the search by words.
     *
     * @param area null or a part type code - every field; {@code pref} - the preferred part: the
     *             fields prefixed by {@code pref_}
     * @return fields in the order of the configuration
     */
    public List<Field> indexTypeFields(@Nullable String area) {
        List<Field> result = new ArrayList<>();
        if (fields == null) {
            return result;
        }
        boolean preferredOnly = PREFIX_PREF.equals(area);
        for (Field field : fields) {
            if (field.getIndexType() == null || (preferredOnly && !field.preferredPartOnly())) {
                continue;
            }
            result.add(field);
        }
        return result;
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

        @XmlAttribute(name = "boost-fulltext")
        private Float boostFulltext;

        @XmlAttribute(name = "transliterate")
        private Boolean transliterate;

        /** Part type holding the item, for the search in names of a part type. */
        @XmlAttribute(name = "part-type")
        private String partType;

        /**
         * Index type of the index rules the field holds (e.g. {@code REAL_NAME}); null for an item
         * field. A field prefixed by {@code pref_} holds the index of the preferred part only.
         */
        @XmlAttribute(name = "index-type")
        private String indexType;

        static Field of(String name, Float boost, Float boostExact, Float boostTransExact,
                        Float boostFulltext, Boolean transliterate) {
            Field field = new Field();
            field.name = name;
            field.boost = boost;
            field.boostExact = boostExact;
            field.boostTransExact = boostTransExact;
            field.boostFulltext = boostFulltext;
            field.transliterate = transliterate;
            return field;
        }

        /** True for a field of the preferred part only (name prefixed by {@code pref_}). */
        public boolean preferredPartOnly() {
            return name != null && name.startsWith(PREFIX_PREF + SEPARATOR);
        }

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

        public Float getBoostFulltext() {
            return boostFulltext;
        }

        public void setBoostFulltext(Float boostFulltext) {
            this.boostFulltext = boostFulltext;
        }

        public Boolean getTransliterate() {
            return transliterate;
        }

        public void setTransliterate(Boolean transliterate) {
            this.transliterate = transliterate;
        }

        public String getPartType() {
            return partType;
        }

        public void setPartType(String partType) {
            this.partType = partType;
        }

        public String getIndexType() {
            return indexType;
        }

        public void setIndexType(String indexType) {
            this.indexType = indexType;
        }
    }
}
