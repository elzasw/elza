package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.domain.RulItemSpecRegister;

/**
 * VO ItemSpecRegister.
 *
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item-spec-register")
public class ItemSpecRegister {

    @XmlAttribute(name = "register-type", required = true)
    private String registerType;

    public String getRegisterType() {
        return registerType;
    }

    public void setRegisterType(final String registerType) {
        this.registerType = registerType;
    }

    public static ItemSpecRegister fromEntity(RulItemSpecRegister rulItemSpecRegister) {
        ItemSpecRegister result = new ItemSpecRegister();
        result.setRegisterType(rulItemSpecRegister.getApType().getCode());
        return result;
    }
}
