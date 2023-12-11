package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * List of available access point types
 *
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ap-types")
@XmlType(name = "ap-types")
public class APTypes {

    @XmlElement(name = "ap-type", required = true)
    private List<APTypeXml> registerTypes;

    public List<APTypeXml> getRegisterTypes() {
        return registerTypes;
    }

    public void setRegisterTypes(final List<APTypeXml> registerTypes) {
        this.registerTypes = registerTypes;
    }
}
