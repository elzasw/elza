package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * VO OutputTypes.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
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
