//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.24 at 04:42:06 PM CET 
//


package cz.tacr.elza.interpi.ws.wo;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Některé typy popisu (zařazení, hierarchická struktura a pod.) je možné zapsat formou odkazu na entitu. V tomto případě je související entita zapsaná v menším rozsahu.
 * 
 * <p>Java class for souvisejici_minTyp complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="souvisejici_minTyp"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="identifikator" type="{http://www.interpi.cz}identifikator_souvTyp" maxOccurs="unbounded"/&gt;
 *         &lt;element name="preferovane_oznaceni" type="{http://www.interpi.cz}oznaceniTyp"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "souvisejici_minTyp", propOrder = {
    "identifikator",
    "preferovaneOznaceni"
})
public class SouvisejiciMinTyp {

    @XmlElement(required = true)
    protected List<IdentifikatorSouvTyp> identifikator;
    @XmlElement(name = "preferovane_oznaceni", required = true)
    protected OznaceniTyp preferovaneOznaceni;

    /**
     * Gets the value of the identifikator property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the identifikator property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIdentifikator().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link IdentifikatorSouvTyp }
     * 
     * 
     */
    public List<IdentifikatorSouvTyp> getIdentifikator() {
        if (identifikator == null) {
            identifikator = new ArrayList<IdentifikatorSouvTyp>();
        }
        return this.identifikator;
    }

    /**
     * Gets the value of the preferovaneOznaceni property.
     * 
     * @return
     *     possible object is
     *     {@link OznaceniTyp }
     *     
     */
    public OznaceniTyp getPreferovaneOznaceni() {
        return preferovaneOznaceni;
    }

    /**
     * Sets the value of the preferovaneOznaceni property.
     * 
     * @param value
     *     allowed object is
     *     {@link OznaceniTyp }
     *     
     */
    public void setPreferovaneOznaceni(OznaceniTyp value) {
        this.preferovaneOznaceni = value;
    }

}
