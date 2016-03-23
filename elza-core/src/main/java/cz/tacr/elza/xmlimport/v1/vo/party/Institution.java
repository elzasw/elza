package cz.tacr.elza.xmlimport.v1.vo.party;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Instituce.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 3. 2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "institution", namespace = NamespaceInfo.NAMESPACE)
public class Institution {

    /** Kód instituce. Délka 50. */
    @XmlAttribute(name = "code", required = true)
    private String code;

    /** Kód typu instituce. */
    @XmlAttribute(name = "type-code", required = true)
    private String typeCode;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
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
