package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.domain.RulItemAptype;

/**
 * VO ItemAptype.
 *
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item-aptype")
public class ItemAptype {

    // --- fields ---

    @XmlAttribute(name = "register-type", required = true)
    private String registerType;

    // --- getters/setters ---

    public String getRegisterType() {
        return registerType;
    }

    public void setRegisterType(final String registerType) {
        this.registerType = registerType;
    }

    // --- methods ---

    public static ItemAptype fromEntity(RulItemAptype rulItemAptype) {
        ItemAptype result = new ItemAptype();
        result.setRegisterType(rulItemAptype.getApType().getCode());
        return result;
    }
}
