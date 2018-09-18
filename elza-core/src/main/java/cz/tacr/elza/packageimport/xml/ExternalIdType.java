package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * VO ExternalIdType.
 *
 * @since 17.07.2018
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "external-id-type")
public class ExternalIdType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "name", required = true)
    private String name;

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
}
