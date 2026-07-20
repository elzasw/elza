package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * List of available institution types
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "institution-types")
@XmlType(name = "institution-types")
public class InstitutionTypes {

    @XmlElement(name = "institution-type", required = true)
    private List<InstitutionType> institutionTypes;

    public List<InstitutionType> getInstitutionTypes() {
        return institutionTypes;
    }

    public void setInstitutionTypes(List<InstitutionType> institutionTypes) {
        this.institutionTypes = institutionTypes;
    }
}
