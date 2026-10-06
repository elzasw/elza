package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Translation file {@code translations/<tag>.xml}: package-provided texts in one language.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "translations")
@XmlType(name = "translations")
public class Translations {

    /** BCP 47 tag of the language, same as the file name. */
    @XmlAttribute(name = "lang", required = true)
    private String lang;

    @XmlElement(name = "t")
    private List<Translation> translations;

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public List<Translation> getTranslations() {
        return translations;
    }

    public void setTranslations(List<Translation> translations) {
        this.translations = translations;
    }
}
