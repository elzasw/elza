
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Wrapper tag for message/notification about linking together of digital archival object and did.
 * 
 * <p>Java class for onDaoLinked complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="onDaoLinked"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="daoIdentifier" type="{http://elza.tacr.cz/ws/types/v1}identifier"/&gt;
 *         &lt;element name="did" type="{http://elza.tacr.cz/ws/types/v1}did"/&gt;
 *         &lt;element name="username" type="{http://elza.tacr.cz/ws/types/v1}username" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="systemIdentifier" type="{http://elza.tacr.cz/ws/types/v1}systemIdentifier" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "onDaoLinked", propOrder = {
    "daoIdentifier",
    "did",
    "username"
})
public class OnDaoLinked {

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String daoIdentifier;
    @XmlElement(required = true)
    protected Did did;
    protected String username;
    @XmlAttribute(name = "systemIdentifier")
    protected String systemIdentifier;

    /**
     * Gets the value of the daoIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDaoIdentifier() {
        return daoIdentifier;
    }

    /**
     * Sets the value of the daoIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDaoIdentifier(String value) {
        this.daoIdentifier = value;
    }

    /**
     * Gets the value of the did property.
     * 
     * @return
     *     possible object is
     *     {@link Did }
     *     
     */
    public Did getDid() {
        return did;
    }

    /**
     * Sets the value of the did property.
     * 
     * @param value
     *     allowed object is
     *     {@link Did }
     *     
     */
    public void setDid(Did value) {
        this.did = value;
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
