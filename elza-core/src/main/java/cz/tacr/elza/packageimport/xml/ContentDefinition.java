package cz.tacr.elza.packageimport.xml;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * ContentDefinition.
 *
 * @author Martin Šlapa
 * @since 01.12.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "content-definition")
public class ContentDefinition {

    @JsonIgnore
    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "desc")
    private String desc;

    @XmlElement(name = "type")
    private String type;

    @XmlElement(name = "definition")
    private String definition;

    @XmlElement(name = "width")
    private Integer width;

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

    public String getDesc() {
        return desc;
    }

    public void setDesc(final String desc) {
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(final String definition) {
        this.definition = definition;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(final Integer width) {
        this.width = width;
    }
}
