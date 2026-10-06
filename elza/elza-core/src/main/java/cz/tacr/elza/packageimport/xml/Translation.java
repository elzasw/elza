package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * One translated text: {@code <t type="ITEM_TYPE" code="SRD_TITLE" field="name">Title</t>}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "t")
public class Translation {

    /** Value of {@code TranslationEntityType}. */
    @XmlAttribute(name = "type", required = true)
    private String type;

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "field", required = true)
    private String field;

    @XmlValue
    private String value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
