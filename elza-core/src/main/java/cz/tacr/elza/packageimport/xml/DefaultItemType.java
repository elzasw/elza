package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * VO odkazu na atribut.
 * @author Pavel Stánek
 * @since 10.06.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "default-item-type")
public class DefaultItemType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }
}
