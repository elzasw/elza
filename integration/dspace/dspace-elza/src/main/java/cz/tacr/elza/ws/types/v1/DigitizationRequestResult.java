
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Result of digitization. Attribute identifier is used to pair result with the request.
 * 
 * <p>Java class for digitizationRequestResult complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="digitizationRequestResult"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="daoImport" type="{http://elza.tacr.cz/ws/types/v1}daoImport"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="identifier" use="required" type="{http://elza.tacr.cz/ws/types/v1}identifier" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "digitizationRequestResult", propOrder = {
    "daoImport"
})
public class DigitizationRequestResult {

    @XmlElement(required = true)
    protected DaoImport daoImport;
    @XmlAttribute(name = "identifier", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String identifier;

    /**
     * Gets the value of the daoImport property.
     * 
     * @return
     *     possible object is
     *     {@link DaoImport }
     *     
     */
    public DaoImport getDaoImport() {
        return daoImport;
    }

    /**
     * Sets the value of the daoImport property.
     * 
     * @param value
     *     allowed object is
     *     {@link DaoImport }
     *     
     */
    public void setDaoImport(DaoImport value) {
        this.daoImport = value;
    }

    /**
     * Gets the value of the identifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the value of the identifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdentifier(String value) {
        this.identifier = value;
    }

}
