package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;

/**
 * VO OutputType.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "output-type")
public class OutputType {

    @XmlAttribute(name = "code", required = true)
    private String code;

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
