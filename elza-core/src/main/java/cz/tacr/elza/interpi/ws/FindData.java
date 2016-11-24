
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
 *         &lt;element name="sQuery" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="sQuerySort" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="sFrom" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="sTo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
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
    "sQuery",
    "sQuerySort",
    "sFrom",
    "sTo",
    "sUser",
    "sPwd"
})
@XmlRootElement(name = "findData")
public class FindData {

    protected String sQuery;
    protected String sQuerySort;
    protected String sFrom;
    protected String sTo;
    protected String sUser;
    protected String sPwd;

    /**
     * Gets the value of the sQuery property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSQuery() {
        return sQuery;
    }

    /**
     * Sets the value of the sQuery property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSQuery(String value) {
        this.sQuery = value;
    }

    /**
     * Gets the value of the sQuerySort property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSQuerySort() {
        return sQuerySort;
    }

    /**
     * Sets the value of the sQuerySort property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSQuerySort(String value) {
        this.sQuerySort = value;
    }

    /**
     * Gets the value of the sFrom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSFrom() {
        return sFrom;
    }

    /**
     * Sets the value of the sFrom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSFrom(String value) {
        this.sFrom = value;
    }

    /**
     * Gets the value of the sTo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSTo() {
        return sTo;
    }

    /**
     * Sets the value of the sTo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSTo(String value) {
        this.sTo = value;
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
