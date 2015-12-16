package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO DescItemSpecRegister.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "desc-item-spec-register")
public class DescItemSpecRegister {

    @XmlAttribute(name = "register-type", required = true)
    private String registerType;

    public String getRegisterType() {
        return registerType;
    }

    public void setRegisterType(final String registerType) {
        this.registerType = registerType;
    }
}
