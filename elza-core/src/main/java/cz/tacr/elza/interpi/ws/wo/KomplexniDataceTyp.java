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
 * Komplexní datace obsahuje úplné informace o dataci, které umožňují zobrazení poznámky pro uživatele i zabezpečení parametrizovaného vyhledávání podle data. Datace je tvořena dvěma částmi: začátek, konec. Každá část se zapisuje jednak jako text, který se zobrazí uživateli, kromě toho je doporučeno uvádět dataci i ve tvaru vhodném pro vyhledávání ve vymezení od-do.
 * 
 * <p>Java class for komplexni_dataceTyp complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="komplexni_dataceTyp"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="text_datace" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="datum_od" type="{http://www.interpi.cz}datumTyp" minOccurs="0"/&gt;
 *         &lt;element name="datum_do" type="{http://www.interpi.cz}datumTyp" minOccurs="0"/&gt;
 *         &lt;element name="poznamka" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="typ" type="{http://www.interpi.cz}komplexni_dataceTypA" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "komplexni_dataceTyp", propOrder = {
    "textDatace",
    "datumOd",
    "datumDo",
    "poznamka"
})
public class KomplexniDataceTyp {

    @XmlElement(name = "text_datace", required = true)
    protected String textDatace;
    @XmlElement(name = "datum_od")
    protected String datumOd;
    @XmlElement(name = "datum_do")
    protected String datumDo;
    protected String poznamka;
    @XmlAttribute(name = "typ")
    protected KomplexniDataceTypA typ;

    /**
     * Gets the value of the textDatace property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTextDatace() {
        return textDatace;
    }

    /**
     * Sets the value of the textDatace property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTextDatace(String value) {
        this.textDatace = value;
    }

    /**
     * Gets the value of the datumOd property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatumOd() {
        return datumOd;
    }

    /**
     * Sets the value of the datumOd property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatumOd(String value) {
        this.datumOd = value;
    }

    /**
     * Gets the value of the datumDo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatumDo() {
        return datumDo;
    }

    /**
     * Sets the value of the datumDo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatumDo(String value) {
        this.datumDo = value;
    }

    /**
     * Gets the value of the poznamka property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPoznamka() {
        return poznamka;
    }

    /**
     * Sets the value of the poznamka property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPoznamka(String value) {
        this.poznamka = value;
    }

    /**
     * Gets the value of the typ property.
     * 
     * @return
     *     possible object is
     *     {@link KomplexniDataceTypA }
     *     
     */
    public KomplexniDataceTypA getTyp() {
        return typ;
    }

    /**
     * Sets the value of the typ property.
     * 
     * @param value
     *     allowed object is
     *     {@link KomplexniDataceTypA }
     *     
     */
    public void setTyp(KomplexniDataceTypA value) {
        this.typ = value;
    }

}