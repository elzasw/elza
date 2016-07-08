package cz.tacr.elza.packageimport.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO PackageRules.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "package-rules")
@XmlType(name = "package-rules")
public class PackageRules {

    @XmlElement(name = "package-rule", required = true)
    private List<PackageRule> packageRules;

    public List<PackageRule> getPackageRules() {
        return packageRules;
    }

    public void setPackageRules(final List<PackageRule> packageRules) {
        this.packageRules = packageRules;
    }
}
