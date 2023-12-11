package cz.tacr.elza.packageimport.xml.common;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * List of other code
 * 
 * Typically used to list deleted or compatible codes
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "other-code")
public class OtherCode {

    @XmlAttribute(name = "code", required = true)
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
