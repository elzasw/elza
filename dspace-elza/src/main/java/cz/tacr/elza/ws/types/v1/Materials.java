
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper element for set of archival materials for digitization.
 * 
 * <p>Java class for materials complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="materials"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="did" type="{http://elza.tacr.cz/ws/types/v1}did" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "materials", propOrder = {
    "did"
})
public class Materials {

    @XmlElement(required = true)
    protected List<Did> did;

    /**
     * Gets the value of the did property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the did property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDid().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Did }
     * 
     * 
     */
    public List<Did> getDid() {
        if (did == null) {
            did = new ArrayList<Did>();
        }
        return this.did;
    }

}
