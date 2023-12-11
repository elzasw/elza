package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * VO PackageActions.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "package-actions")
@XmlType(name = "package-actions")
public class ActionsXml {

    @XmlElement(name = "package-action", required = true)
    private List<ActionXml> packageActions;

    public List<ActionXml> getPackageActions() {
        return packageActions;
    }

    public void setPackageActions(final List<ActionXml> packageActions) {
        this.packageActions = packageActions;
    }
}
