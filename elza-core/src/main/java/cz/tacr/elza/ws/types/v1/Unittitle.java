
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * A child element of <did> that specifies a title for the described materials.
 * 
 * <unittitle> is for recording the title statement, either formal or supplied, of the described materials. The title statement may consist of a word or phrase. <unittitle> is used at both the highest unit or <archdesc> level (e.g., collection, record group, or fonds) and at all the subordinate <c> levels (e.g., subseries, files, items, or other intervening stages within a hierarchical description).
 * 
 * Attribute usage:
 * 	- use @localtype if local use requires recording the type of <unittitle>.
 * 
 * <p>Java class for unittitle complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="unittitle"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="localtype" type="{http://elza.tacr.cz/ws/types/v1}localtype" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "unittitle")
public class Unittitle {

    @XmlAttribute(name = "localtype")
    protected String localtype;

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

}
