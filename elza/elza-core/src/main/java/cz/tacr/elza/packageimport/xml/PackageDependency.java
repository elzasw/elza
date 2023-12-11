package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * VO PackageDependency.
 *
 * @since 15.09.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dependency")
public class PackageDependency {

    /**
     * Kód závislého balíčku.
     */
    @XmlAttribute(name = "code", required = true)
    private String code;

    /**
     * Minimální požadovaná verze.
     */
    @XmlAttribute(name = "min-version", required = true)
    private Integer minVersion;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public Integer getMinVersion() {
        return minVersion;
    }

    public void setMinVersion(final Integer minVersion) {
        this.minVersion = minVersion;
    }

    @Override
    public String toString() {
        return "PackageDependency{" +
                "code='" + code + '\'' +
                ", minVersion=" + minVersion +
                '}';
    }
}
