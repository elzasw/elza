
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * An element for encoding an individual date related to the materials being described. 
 * 
 * <p>Java class for datesingle complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="datesingle"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="localtype" type="{http://elza.tacr.cz/ws/types/v1}localtype" /&gt;
 *       &lt;attribute name="standarddate" type="{http://elza.tacr.cz/ws/types/v1}standarddate" /&gt;
 *       &lt;attribute name="notafter" type="{http://elza.tacr.cz/ws/types/v1}standarddate" /&gt;
 *       &lt;attribute name="notbefore" type="{http://elza.tacr.cz/ws/types/v1}standarddate" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "datesingle")
public class Datesingle {

    @XmlAttribute(name = "localtype")
    protected String localtype;
    @XmlAttribute(name = "standarddate")
    protected String standarddate;
    @XmlAttribute(name = "notafter")
    protected String notafter;
    @XmlAttribute(name = "notbefore")
    protected String notbefore;

    /**
     * Gets the value of the localtype property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLocaltype() {
        return localtype;
    }

    /**
     * Sets the value of the localtype property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLocaltype(String value) {
        this.localtype = value;
    }

    /**
     * Gets the value of the standarddate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStandarddate() {
        return standarddate;
    }

    /**
     * Sets the value of the standarddate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStandarddate(String value) {
        this.standarddate = value;
    }

    /**
     * Gets the value of the notafter property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNotafter() {
        return notafter;
    }

    /**
     * Sets the value of the notafter property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNotafter(String value) {
        this.notafter = value;
    }

    /**
     * Gets the value of the notbefore property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNotbefore() {
        return notbefore;
    }

    /**
     * Sets the value of the notbefore property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNotbefore(String value) {
        this.notbefore = value;
    }

}
