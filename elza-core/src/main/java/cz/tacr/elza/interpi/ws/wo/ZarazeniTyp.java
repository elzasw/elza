//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.24 at 04:42:06 PM CET 
//


package cz.tacr.elza.interpi.ws.wo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Zařazení je vyjádření charakteristiky entity pomocí jednoduchých slov nebo slovních spojení - v zásadě propojením na obecný pojem. Typy zařazení jsou vymezeny pro jednotlivé třídy.
 * 
 * <p>Java class for zarazeniTyp complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="zarazeniTyp"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="souvisejici_entita" type="{http://www.interpi.cz}souvisejici_minTyp" minOccurs="0"/&gt;
 *         &lt;element name="text_zarazeni" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="datace" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="typ" use="required" type="{http://www.interpi.cz}zarazeniTypA" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "zarazeniTyp", propOrder = {
    "souvisejiciEntita",
    "textZarazeni",
    "datace"
})
public class ZarazeniTyp {

    @XmlElement(name = "souvisejici_entita")
    protected SouvisejiciMinTyp souvisejiciEntita;
    @XmlElement(name = "text_zarazeni")
    protected String textZarazeni;
    protected String datace;
    @XmlAttribute(name = "typ", required = true)
    protected ZarazeniTypA typ;

    /**
     * Gets the value of the souvisejiciEntita property.
     * 
     * @return
     *     possible object is
     *     {@link SouvisejiciMinTyp }
     *     
     */
    public SouvisejiciMinTyp getSouvisejiciEntita() {
        return souvisejiciEntita;
    }

    /**
     * Sets the value of the souvisejiciEntita property.
     * 
     * @param value
     *     allowed object is
     *     {@link SouvisejiciMinTyp }
     *     
     */
    public void setSouvisejiciEntita(SouvisejiciMinTyp value) {
        this.souvisejiciEntita = value;
    }

    /**
     * Gets the value of the textZarazeni property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTextZarazeni() {
        return textZarazeni;
    }

    /**
     * Sets the value of the textZarazeni property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTextZarazeni(String value) {
        this.textZarazeni = value;
    }

    /**
     * Gets the value of the datace property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatace() {
        return datace;
    }

    /**
     * Sets the value of the datace property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatace(String value) {
        this.datace = value;
    }

    /**
     * Gets the value of the typ property.
     * 
     * @return
     *     possible object is
     *     {@link ZarazeniTypA }
     *     
     */
    public ZarazeniTypA getTyp() {
        return typ;
    }

    /**
     * Sets the value of the typ property.
     * 
     * @param value
     *     allowed object is
     *     {@link ZarazeniTypA }
     *     
     */
    public void setTyp(ZarazeniTypA value) {
        this.typ = value;
    }

}