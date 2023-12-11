package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * VO ExternalIdTypes.
 *
 * @since 17.07.2018
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "external-id-types")
@XmlType(name = "external-id-types")
public class ExternalIdTypes {

    @XmlElement(name = "external-id-type", required = true)
    private List<ExternalIdType> externalIdTypes;

    public List<ExternalIdType> getExternalIdTypes() {
        return externalIdTypes;
    }

    public void setExternalIdTypes(final List<ExternalIdType> externalIdTypes) {
        this.externalIdTypes = externalIdTypes;
    }
}
