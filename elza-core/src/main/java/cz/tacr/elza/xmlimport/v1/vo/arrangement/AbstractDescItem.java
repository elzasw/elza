package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Abstraktní předek hodnot.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "abstract-desc-item", namespace = NamespaceInfo.NAMESPACE)
public abstract class AbstractDescItem {

    @XmlAttribute(name = "desc-item-type-code", required = true)
    private String descItemTypeCode;

    @XmlAttribute(name = "desc-item-spec-code")
    private String descItemSpecCode;

    @XmlElement(name = "position", required = true)
    private Integer position;

    public String getDescItemTypeCode() {
        return descItemTypeCode;
    }

    public void setDescItemTypeCode(String descItemTypeCode) {
        this.descItemTypeCode = descItemTypeCode;
    }

    public String getDescItemSpecCode() {
        return descItemSpecCode;
    }

    public void setDescItemSpecCode(String descItemSpecCode) {
        this.descItemSpecCode = descItemSpecCode;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
