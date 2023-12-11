package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * VO typ vztahu.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "relation-type")
public class RelationType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "relation-class-type", required = true)
    private String relatioClassType;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "class-type")
    private String classType;

    @XmlElement(name = "use-unitdate", required = true)
    private String useUnitdate;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getRelatioClassType() {
        return relatioClassType;
    }

    public void setRelatioClassType(final String relatioClassType) {
        this.relatioClassType = relatioClassType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(final String classType) {
        this.classType = classType;
    }

    public String getUseUnitdate() {
        return useUnitdate;
    }

    public void setUseUnitdate(final String useUnitdate) {
        this.useUnitdate = useUnitdate;
    }
}
