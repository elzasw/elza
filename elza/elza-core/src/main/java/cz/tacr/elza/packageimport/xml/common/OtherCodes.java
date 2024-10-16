package cz.tacr.elza.packageimport.xml.common;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * List of other codes
 * 
 * Typically used to list deleted or compatible codes
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "other-codes")
public class OtherCodes {

    @XmlElement(name = "other-code", required = true)
    List<OtherCode> otherCodes;

    public List<OtherCode> getOtherCodes() {
        return otherCodes;
    }

    public void setOtherCodes(List<OtherCode> otherCodes) {
        this.otherCodes = otherCodes;
    }

}
