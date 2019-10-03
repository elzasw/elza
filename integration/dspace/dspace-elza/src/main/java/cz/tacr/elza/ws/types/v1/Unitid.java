
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * A child element of <did> that provides an identifier for the materials being described, such as an accession number.
 * 
 * <unitid> may contain any alpha-numeric text string that serves as a unique reference point or control number for the described material, such as a lot number, an accession number, a classification number, or an entry number in a bibliography or catalog. <unitid> is primarily a logical designation, which sometimes indirectly provides location information, as in the case of a classification number.
 * 
 * Attribute localtype can be used to declare type of unitid.
 * 
 * <p>Java class for unitid complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="unitid"&gt;
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
@XmlType(name = "unitid")
public class Unitid {

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
