package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO typ vztahu.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "register-type")
public class RegisterType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "party-type")
    private String partyType;

    @XmlAttribute(name = "parent-register-type")
    private String parentRegisterType;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "add-record")
    private Boolean addRecord;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getPartyType() {
        return partyType;
    }

    public void setPartyType(final String partyType) {
        this.partyType = partyType;
    }

    public String getParentRegisterType() {
        return parentRegisterType;
    }

    public void setParentRegisterType(final String parentRegisterType) {
        this.parentRegisterType = parentRegisterType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Boolean getAddRecord() {
        return addRecord;
    }

    public void setAddRecord(final Boolean addRecord) {
        this.addRecord = addRecord;
    }
}
