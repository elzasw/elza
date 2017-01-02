
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * A child element of <did> that provides a simple statement of the date(s) covered by the described materials.
 * 
 * <p>Java class for unitdatestructured complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="unitdatestructured"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice&gt;
 *         &lt;element name="datesingle" type="{http://elza.tacr.cz/ws/types/v1}datesingle"/&gt;
 *       &lt;/choice&gt;
 *       &lt;attribute name="calendar" type="{http://elza.tacr.cz/ws/types/v1}calendar" /&gt;
 *       &lt;attribute name="certainty" type="{http://elza.tacr.cz/ws/types/v1}certainity" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "unitdatestructured", propOrder = {
    "datesingle"
})
public class Unitdatestructured {

    protected Datesingle datesingle;
    @XmlAttribute(name = "calendar")
    protected Calendar calendar;
    @XmlAttribute(name = "certainty")
    protected Certainity certainty;

    /**
     * Gets the value of the datesingle property.
     * 
     * @return
     *     possible object is
     *     {@link Datesingle }
     *     
     */
    public Datesingle getDatesingle() {
        return datesingle;
    }

    /**
     * Sets the value of the datesingle property.
     * 
     * @param value
     *     allowed object is
     *     {@link Datesingle }
     *     
     */
    public void setDatesingle(Datesingle value) {
        this.datesingle = value;
    }

    /**
     * Gets the value of the calendar property.
     * 
     * @return
     *     possible object is
     *     {@link Calendar }
     *     
     */
    public Calendar getCalendar() {
        return calendar;
    }

    /**
     * Sets the value of the calendar property.
     * 
     * @param value
     *     allowed object is
     *     {@link Calendar }
     *     
     */
    public void setCalendar(Calendar value) {
        this.calendar = value;
    }

    /**
     * Gets the value of the certainty property.
     * 
     * @return
     *     possible object is
     *     {@link Certainity }
     *     
     */
    public Certainity getCertainty() {
        return certainty;
    }

    /**
     * Sets the value of the certainty property.
     * 
     * @param value
     *     allowed object is
     *     {@link Certainity }
     *     
     */
    public void setCertainty(Certainity value) {
        this.certainty = value;
    }

}
