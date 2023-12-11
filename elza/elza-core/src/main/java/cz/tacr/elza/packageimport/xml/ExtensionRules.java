package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
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
