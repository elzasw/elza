
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Request to transfer set of digital archival objects to other fund.
 * 
 * <p>Java class for transferRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="transferRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="daoIdentifiers" type="{http://elza.tacr.cz/ws/types/v1}daoIdentifiers"/&gt;
 *         &lt;element name="targetFund" type="{http://elza.tacr.cz/ws/types/v1}identifier" minOccurs="0"/&gt;
 *         &lt;element name="description" type="{http://elza.tacr.cz/ws/types/v1}description" minOccurs="0"/&gt;
 *         &lt;element name="username" type="{http://elza.tacr.cz/ws/types/v1}username" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="identifier" use="required" type="{http://elza.tacr.cz/ws/types/v1}identifier" /&gt;
 *       &lt;attribute name="systemIdentifier" type="{http://elza.tacr.cz/ws/types/v1}systemIdentifier" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "transferRequest", propOrder = {
    "daoIdentifiers",
    "targetFund",
    "description",
    "username"
})
public class TransferRequest {

    @XmlElement(required = true)
    protected DaoIdentifiers daoIdentifiers;
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String targetFund;
    protected String description;
    protected String username;
    @XmlAttribute(name = "identifier", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String identifier;
    @XmlAttribute(name = "systemIdentifier")
    protected String systemIdentifier;

    /**
     * Gets the value of the daoIdentifiers property.
     * 
     * @return
     *     possible object is
     *     {@link DaoIdentifiers }
     *     
     */
    public DaoIdentifiers getDaoIdentifiers() {
        return daoIdentifiers;
    }

    /**
     * Sets the value of the daoIdentifiers property.
     * 
     * @param value
     *     allowed object is
     *     {@link DaoIdentifiers }
     *     
     */
    public void setDaoIdentifiers(DaoIdentifiers value) {
        this.daoIdentifiers = value;
    }

    /**
     * Gets the value of the targetFund property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTargetFund() {
        return targetFund;
    }

    /**
     * Sets the value of the targetFund property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTargetFund(String value) {
        this.targetFund = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the username property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the value of the username property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUsername(String value) {
        this.username = value;
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

    /**
     * Gets the value of the systemIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSystemIdentifier() {
        return systemIdentifier;
    }

    /**
     * Sets the value of the systemIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSystemIdentifier(String value) {
        this.systemIdentifier = value;
    }

}
