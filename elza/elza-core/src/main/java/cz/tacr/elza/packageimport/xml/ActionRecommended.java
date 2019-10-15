package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * VO ActionRecommended.
 *
 * @author Martin Šlapa
 * @since 28.06.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "action-recommended")
public class ActionRecommended {

    @XmlAttribute(name = "output-type", required = true)
    private String outputType;

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(final String outputType) {
        this.outputType = outputType;
    }
}
