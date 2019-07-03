
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Response on daosSyncRequest. Each dao has separate answer.
 * 
 * <p>Java class for daosSyncResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="daosSyncResponse"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="daoset" type="{http://elza.tacr.cz/ws/types/v1}daoset" minOccurs="0"/&gt;
 *         &lt;element name="nonexistingDaos" type="{http://elza.tacr.cz/ws/types/v1}nonexistingDaos"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "daosSyncResponse", propOrder = {
    "daoset",
    "nonexistingDaos"
})
public class DaosSyncResponse {

    protected Daoset daoset;
    @XmlElement(required = true)
    protected NonexistingDaos nonexistingDaos;

    /**
     * Gets the value of the daoset property.
     * 
     * @return
     *     possible object is
     *     {@link Daoset }
     *     
     */
    public Daoset getDaoset() {
        return daoset;
    }

    /**
     * Sets the value of the daoset property.
     * 
     * @param value
     *     allowed object is
     *     {@link Daoset }
     *     
     */
    public void setDaoset(Daoset value) {
        this.daoset = value;
    }

    /**
     * Gets the value of the nonexistingDaos property.
     * 
     * @return
     *     possible object is
     *     {@link NonexistingDaos }
     *     
     */
    public NonexistingDaos getNonexistingDaos() {
        return nonexistingDaos;
    }

    /**
     * Sets the value of the nonexistingDaos property.
     * 
     * @param value
     *     allowed object is
     *     {@link NonexistingDaos }
     *     
     */
    public void setNonexistingDaos(NonexistingDaos value) {
        this.nonexistingDaos = value;
    }

}
