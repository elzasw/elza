package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * VO OutputType.
 *
 * @since 17.6.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "output-type")
public class OutputType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "filename", required = false)
    private String filename;

    @XmlElement(name = "name", required = true)
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
