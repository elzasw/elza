package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * VO OutputTypes.
 *
 * @since 17.6.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "output-types")
@XmlType(name = "output-types")
public class OutputTypes {

    @XmlElement(name = "output-type", required = true)
    private List<OutputType> outputTypes;

    public List<OutputType> getOutputTypes() {
        return outputTypes;
    }

    public void setOutputTypes(final List<OutputType> outputTypes) {
        this.outputTypes = outputTypes;
    }
}
