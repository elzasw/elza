
package cz.tacr.elza.interpi.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="sT001" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="sData" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="sUser" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="sPwd" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "st001",
    "sData",
    "sUser",
    "sPwd"
})
@XmlRootElement(name = "writeOneRecord")
public class WriteOneRecord {

    @XmlElement(name = "sT001")
    protected String st001;
    protected String sData;
    protected String sUser;
    protected String sPwd;

    /**
     * Gets the value of the st001 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getST001() {
        return st001;
    }

    /**
     * Sets the value of the st001 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setST001(String value) {
        this.st001 = value;
    }

    /**
     * Gets the value of the sData property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSData() {
        return sData;
    }

    /**
     * Sets the value of the sData property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSData(String value) {
        this.sData = value;
    }

    /**
     * Gets the value of the sUser property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSUser() {
        return sUser;
    }

    /**
     * Sets the value of the sUser property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSUser(String value) {
        this.sUser = value;
    }

    /**
     * Gets the value of the sPwd property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSPwd() {
        return sPwd;
    }

    /**
     * Sets the value of the sPwd property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSPwd(String value) {
        this.sPwd = value;
    }

}
