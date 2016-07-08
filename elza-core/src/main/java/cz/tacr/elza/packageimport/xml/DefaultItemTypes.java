package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * Vo kontejner na seznam implicitních atributů pro zobrazení u RuleSet.
 *
 * @author Pavel Stánek
 * @since 10.06.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "default-item-types")
public class DefaultItemTypes {
    @XmlElement(name = "item-type", required = true)
    private List<DefaultItemType> defaultItemTypes;

    public List<DefaultItemType> getDefaultItemTypes() {
        return defaultItemTypes;
    }

    public void setDefaultItemTypes(final List<DefaultItemType> defaultItemTypes) {
        this.defaultItemTypes = defaultItemTypes;
    }
}
