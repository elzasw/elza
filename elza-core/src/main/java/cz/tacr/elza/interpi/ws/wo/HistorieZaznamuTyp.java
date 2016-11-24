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
 * Historie záznamu - přehled datací změn záznamu a zpracovatelů.
 * 
 * <p>Java class for historie_zaznamuTyp complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="historie_zaznamuTyp"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="souvisejici_entita" type="{http://www.interpi.cz}souvisejiciZpracTyp" maxOccurs="unbounded"/&gt;
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
@XmlType(name = "historie_zaznamuTyp", propOrder = {
    "souvisejiciEntita",
    "datace"
})
public class HistorieZaznamuTyp {

    @XmlElement(name = "souvisejici_entita", required = true)
    protected List<SouvisejiciZpracTyp> souvisejiciEntita;
    @XmlElement(required = true)
    protected String datace;

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
     * {@link SouvisejiciZpracTyp }
     * 
     * 
     */
    public List<SouvisejiciZpracTyp> getSouvisejiciEntita() {
        if (souvisejiciEntita == null) {
            souvisejiciEntita = new ArrayList<SouvisejiciZpracTyp>();
        }
        return this.souvisejiciEntita;
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
