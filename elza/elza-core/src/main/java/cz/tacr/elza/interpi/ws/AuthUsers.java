
package cz.tacr.elza.interpi.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element name="sAuthUser" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="sAuthUserPwd" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
    "sAuthUser",
    "sAuthUserPwd",
    "sUser",
    "sPwd"
})
@XmlRootElement(name = "authUsers")
public class AuthUsers {

    protected String sAuthUser;
    protected String sAuthUserPwd;
    protected String sUser;
    protected String sPwd;

    /**
     * Gets the value of the sAuthUser property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSAuthUser() {
        return sAuthUser;
    }

    /**
     * Sets the value of the sAuthUser property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSAuthUser(String value) {
        this.sAuthUser = value;
    }

    /**
     * Gets the value of the sAuthUserPwd property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSAuthUserPwd() {
        return sAuthUserPwd;
    }

    /**
     * Sets the value of the sAuthUserPwd property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSAuthUserPwd(String value) {
        this.sAuthUserPwd = value;
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
