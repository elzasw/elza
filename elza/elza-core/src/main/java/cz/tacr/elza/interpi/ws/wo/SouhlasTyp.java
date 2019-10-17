//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.24 at 04:42:06 PM CET 
//


package cz.tacr.elza.interpi.ws.wo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Do záznamu se ukládá informace o datu a formě poskytnutí souhlasu fyzické osoby se zpracováním osobních údajů v případě, že se souhlas vyžaduje.
 * 
 * <p>Java class for souhlasTyp complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="souhlasTyp"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="forma_poskytnuti" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="datace" type="{http://www.interpi.cz}presne_datumTyp"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "souhlasTyp", propOrder = {
    "formaPoskytnuti",
    "datace"
})
public class SouhlasTyp {

    @XmlElement(name = "forma_poskytnuti", required = true)
    protected String formaPoskytnuti;
    @XmlElement(required = true)
    protected String datace;

    /**
     * Gets the value of the formaPoskytnuti property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFormaPoskytnuti() {
        return formaPoskytnuti;
    }

    /**
     * Sets the value of the formaPoskytnuti property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFormaPoskytnuti(String value) {
        this.formaPoskytnuti = value;
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

}