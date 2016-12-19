
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Wrapper tag for message/notification about unlinking of digital archival object and did.
 * 
 * <p>Java class for onDaoUnlinked complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="onDaoUnlinked"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="daoIdentifier" type="{http://elza.tacr.cz/ws/types/v1}identifier"/&gt;
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
@XmlType(name = "onDaoUnlinked", propOrder = {
    "daoIdentifier"
})
public class OnDaoUnlinked {

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String daoIdentifier;
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
