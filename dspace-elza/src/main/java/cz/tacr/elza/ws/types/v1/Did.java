
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;


/**
 * Descriptive Identification, a wrapper element that encloses information essential for identifying the material being described. 
 * Source: EAD3
 * 
 * <p>Java class for did complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="did"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="abstract" type="{http://elza.tacr.cz/ws/types/v1}abstract" minOccurs="0"/&gt;
 *         &lt;element name="unitdatestructured" type="{http://elza.tacr.cz/ws/types/v1}unitdatestructured" minOccurs="0"/&gt;
 *         &lt;element name="unitid" type="{http://elza.tacr.cz/ws/types/v1}unitid" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="unittitle" type="{http://elza.tacr.cz/ws/types/v1}unittitle" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="identifier" type="{http://elza.tacr.cz/ws/types/v1}identifier" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "did", propOrder = {
    "_abstract",
    "unitdatestructured",
    "unitid",
    "unittitle"
})
public class Did {

    @XmlElement(name = "abstract")
    protected String _abstract;
    protected Unitdatestructured unitdatestructured;
    protected List<Unitid> unitid;
    protected List<Unittitle> unittitle;
    @XmlAttribute(name = "identifier")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String identifier;

    /**
     * Gets the value of the abstract property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAbstract() {
        return _abstract;
    }

    /**
     * Sets the value of the abstract property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAbstract(String value) {
        this._abstract = value;
    }

    /**
     * Gets the value of the unitdatestructured property.
     * 
     * @return
     *     possible object is
     *     {@link Unitdatestructured }
     *     
     */
    public Unitdatestructured getUnitdatestructured() {
        return unitdatestructured;
    }

    /**
     * Sets the value of the unitdatestructured property.
     * 
     * @param value
     *     allowed object is
     *     {@link Unitdatestructured }
     *     
     */
    public void setUnitdatestructured(Unitdatestructured value) {
        this.unitdatestructured = value;
    }

    /**
     * Gets the value of the unitid property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the unitid property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUnitid().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Unitid }
     * 
     * 
     */
    public List<Unitid> getUnitid() {
        if (unitid == null) {
            unitid = new ArrayList<Unitid>();
        }
        return this.unitid;
    }

    /**
     * Gets the value of the unittitle property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the unittitle property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUnittitle().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Unittitle }
     * 
     * 
     */
    public List<Unittitle> getUnittitle() {
        if (unittitle == null) {
            unittitle = new ArrayList<Unittitle>();
        }
        return this.unittitle;
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
