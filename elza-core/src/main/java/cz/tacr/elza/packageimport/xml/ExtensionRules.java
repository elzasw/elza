package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO ExtensionRules.
 *
 * @since 17.10.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "extension-rules")
@XmlType(name = "extension-rules")
public class ExtensionRules {

    @XmlElement(name = "extension-rule", required = true)
    private List<ExtensionRule> extensionRules;

    public List<ExtensionRule> getExtensionRules() {
        return extensionRules;
    }

    public void setExtensionRules(final List<ExtensionRule> extensionRules) {
        this.extensionRules = extensionRules;
    }
}
