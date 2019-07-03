
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * Set of links between digital archival objects and material being described with <did> element.
 * 
 * <p>Java class for daoLinks complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="daoLinks"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="daoLink" type="{http://elza.tacr.cz/ws/types/v1}daoLink" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "daoLinks", propOrder = {
    "daoLink"
})
public class DaoLinks {

    protected List<DaoLink> daoLink;

    /**
     * Gets the value of the daoLink property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the daoLink property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDaoLink().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DaoLink }
     * 
     * 
     */
    public List<DaoLink> getDaoLink() {
        if (daoLink == null) {
            daoLink = new ArrayList<DaoLink>();
        }
        return this.daoLink;
    }

}
