package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO vztah typu třídy - seznam.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "register-types")
@XmlType(name = "register-types")
public class RegisterTypes {

    @XmlElement(name = "register-type", required = true)
    private List<RegisterType> registerTypes;

    public List<RegisterType> getRegisterTypes() {
        return registerTypes;
    }

    public void setRegisterTypes(final List<RegisterType> registerTypes) {
        this.registerTypes = registerTypes;
    }
}
