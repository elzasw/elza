package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * VO ArrangementExtension - {@link cz.tacr.elza.domain.RulArrangementExtension}
 *
 * @since 17.10.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "arrangement-extension")
public class ArrangementExtension {

    /**
     * Unikátní kód.
     */
    @XmlAttribute(name = "code", required = true)
    private String code;

    /**
     * Název souboru.
     */
    @XmlElement(name = "name", required = true)
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
