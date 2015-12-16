package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO DescItemConstraint.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "desc-item-constraint")
public class DescItemConstraint {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "desc-item-type", required = true)
    private String descItemType;

    @XmlAttribute(name = "desc-item-spec")
    private String descItemSpec;

    @XmlElement(name = "repeatable")
    private Boolean repeatable;

    @XmlElement(name = "regexp")
    private String regexp;

    @XmlElement(name = "text-lenght-limit")
    private Integer textLenghtLimit;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getDescItemType() {
        return descItemType;
    }

    public void setDescItemType(final String descItemType) {
        this.descItemType = descItemType;
    }

    public String getDescItemSpec() {
        return descItemSpec;
    }

    public void setDescItemSpec(final String descItemSpec) {
        this.descItemSpec = descItemSpec;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(final String regexp) {
        this.regexp = regexp;
    }

    public Integer getTextLenghtLimit() {
        return textLenghtLimit;
    }

    public void setTextLenghtLimit(final Integer textLenghtLimit) {
        this.textLenghtLimit = textLenghtLimit;
    }
}
