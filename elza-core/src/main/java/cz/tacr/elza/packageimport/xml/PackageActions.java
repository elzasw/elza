package cz.tacr.elza.packageimport.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO PackageActions.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "package-actions")
@XmlType(name = "package-actions")
public class PackageActions {

    @XmlElement(name = "package-action", required = true)
    private List<PackageAction> packageActions;

    public List<PackageAction> getPackageActions() {
        return packageActions;
    }

    public void setPackageActions(final List<PackageAction> packageActions) {
        this.packageActions = packageActions;
    }
}
