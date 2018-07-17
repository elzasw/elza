package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;
import java.util.List;


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
