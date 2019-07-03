
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Digitization request.
 * 
 * <p>Java class for digitizationRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="digitizationRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="description" type="{http://elza.tacr.cz/ws/types/v1}description" minOccurs="0"/&gt;
 *         &lt;element name="materials" type="{http://elza.tacr.cz/ws/types/v1}materials"/&gt;
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
@XmlType(name = "digitizationRequest", propOrder = {
    "description",
    "materials"
})
public class DigitizationRequest {

    protected String description;
    @XmlElement(required = true)
    protected Materials materials;
    @XmlAttribute(name = "identifier", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String identifier;
    @XmlAttribute(name = "systemIdentifier")
    protected String systemIdentifier;

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
     * Gets the value of the materials property.
     * 
     * @return
     *     possible object is
     *     {@link Materials }
     *     
     */
    public Materials getMaterials() {
        return materials;
    }

    /**
     * Sets the value of the materials property.
     * 
     * @param value
     *     allowed object is
     *     {@link Materials }
     *     
     */
    public void setMaterials(Materials value) {
        this.materials = value;
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
