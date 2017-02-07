package cz.tacr.elza.packageimport.xml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * VO skupina osoby.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "party-group")
public class PartyGroup {

    private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "party-type")
    private String partyType;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "view-order", required = true)
    private Integer viewOrder;

    @XmlElement(name = "type", required = true)
    private String type;

    @XmlJavaTypeAdapter(ContentDefinitionAdapter.class)
    @XmlElement(name = "content-definitions")
    private Map<String, ContentDefinition> contentDefinitions;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getPartyType() {
        return partyType;
    }

    public void setPartyType(final String partyType) {
        this.partyType = partyType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Map<String, ContentDefinition> getContentDefinitions() {
        return contentDefinitions;
    }

    public void setContentDefinitions(final Map<String, ContentDefinition> contentDefinitions) {
        this.contentDefinitions = contentDefinitions;
    }

    public String getContentDefinitionsString() {
        try {
            return objectMapper.writeValueAsString(contentDefinitions);
        } catch (JsonProcessingException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }

    public void setContentDefinitionsString(final String contentDefinitions) {
        try {
            TypeReference<LinkedHashMap<String,ContentDefinition>> typeRef = new TypeReference<LinkedHashMap<String,ContentDefinition>>() {};
            setContentDefinitions(objectMapper.readValue(contentDefinitions, typeRef));
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
    }
}
