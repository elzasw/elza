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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * Záznam entity může obsahovat narativně tvořený popis entity různého typu a rozsahu. Na existující popisy entit je možno odkázat s uvedením zdroje nebo vložením propojením na webovou stránku. U popisu je možné uvést autora popisu.
 * 
 * <p>Java class for popisTyp complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="popisTyp"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="souvisejici_entita" type="{http://www.interpi.cz}souvisejiciPracTyp" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="text_popisu" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="cislo_zdroje" type="{http://www.interpi.cz}cislo_zdrojeTyp" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="typ" use="required" type="{http://www.interpi.cz}popisTypA" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "popisTyp", propOrder = {
    "souvisejiciEntita",
    "textPopisu",
    "cisloZdroje"
})
public class PopisTyp {

    @XmlElement(name = "souvisejici_entita")
    protected List<SouvisejiciPracTyp> souvisejiciEntita;
    @XmlElement(name = "text_popisu", required = true)
    protected String textPopisu;
    @XmlElement(name = "cislo_zdroje", type = Integer.class)
    @XmlSchemaType(name = "integer")
    protected List<Integer> cisloZdroje;
    @XmlAttribute(name = "typ", required = true)
    protected PopisTypA typ;

    /**
     * Gets the value of the souvisejiciEntita property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the souvisejiciEntita property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSouvisejiciEntita().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SouvisejiciPracTyp }
     * 
     * 
     */
    public List<SouvisejiciPracTyp> getSouvisejiciEntita() {
        if (souvisejiciEntita == null) {
            souvisejiciEntita = new ArrayList<SouvisejiciPracTyp>();
        }
        return this.souvisejiciEntita;
    }

    /**
     * Gets the value of the textPopisu property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTextPopisu() {
        return textPopisu;
    }

    /**
     * Sets the value of the textPopisu property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTextPopisu(String value) {
        this.textPopisu = value;
    }

    /**
     * Gets the value of the cisloZdroje property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cisloZdroje property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCisloZdroje().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Integer }
     * 
     * 
     */
    public List<Integer> getCisloZdroje() {
        if (cisloZdroje == null) {
            cisloZdroje = new ArrayList<Integer>();
        }
        return this.cisloZdroje;
    }

    /**
     * Gets the value of the typ property.
     * 
     * @return
     *     possible object is
     *     {@link PopisTypA }
     *     
     */
    public PopisTypA getTyp() {
        return typ;
    }

    /**
     * Sets the value of the typ property.
     * 
     * @param value
     *     allowed object is
     *     {@link PopisTypA }
     *     
     */
    public void setTyp(PopisTypA value) {
        this.typ = value;
    }

}