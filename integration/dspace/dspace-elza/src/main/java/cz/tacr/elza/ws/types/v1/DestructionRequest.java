
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Request to destruct one or more digital archival objects.
 * 
 * At a minimum, destruction may be no more complicated than placing materials in the trash for transfer to a landfill. In other instances, the manner of destruction is appropriate to the sensitivity of the information contained in the materials and may involve shredding, maceration, or incineration. For electronic records, simple destruction may be accomplished by deleting the record, which merely removes the pointer from an index without overwriting the data. For sensitive information in electronic format, the media may be overwritten numerous time or physically destroyed to make it impossible to recover the data
 * 
 * <p>Java class for destructionRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="destructionRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="daoIdentifiers" type="{http://elza.tacr.cz/ws/types/v1}daoIdentifiers"/&gt;
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
@XmlType(name = "destructionRequest", propOrder = {
    "daoIdentifiers",
    "description",
    "username"
})
public class DestructionRequest {

    @XmlElement(required = true)
    protected DaoIdentifiers daoIdentifiers;
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
