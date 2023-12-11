package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO číselník typů doplňků jmen osob - seznam.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "complement-types")
@XmlType(name = "complement-types")
public class ComplementTypes {

    @XmlElement(name = "complement-type", required = true)
    private List<ComplementType> complementTypes;

    public List<ComplementType> getComplementTypes() {
        return complementTypes;
    }

    public void setComplementTypes(final List<ComplementType> complementTypes) {
        this.complementTypes = complementTypes;
    }
}
