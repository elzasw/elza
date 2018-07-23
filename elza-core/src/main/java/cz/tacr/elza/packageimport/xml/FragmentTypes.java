package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;
import java.util.List;


/**
 * VO FragmentTypes.
 *
 * @since 01.11.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "fragment-types")
@XmlType(name = "fragment-types")
public class FragmentTypes {

    @XmlElement(name = "fragment-type", required = true)
    private List<FragmentType> fragmentTypes;

    public List<FragmentType> getFragmentTypes() {
        return fragmentTypes;
    }

    public void setFragmentTypes(final List<FragmentType> fragmentTypes) {
        this.fragmentTypes = fragmentTypes;
    }
}
