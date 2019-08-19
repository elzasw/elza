
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for daoSyncRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="daoSyncRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="daoId" type="{http://elza.tacr.cz/ws/types/v1}identifier"/&gt;
 *         &lt;element name="didId" type="{http://elza.tacr.cz/ws/types/v1}identifier" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "daoSyncRequest", propOrder = {
    "daoId",
    "didId"
})
public class DaoSyncRequest {

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String daoId;
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String didId;

    /**
     * Gets the value of the daoId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDaoId() {
        return daoId;
    }

    /**
     * Sets the value of the daoId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDaoId(String value) {
        this.daoId = value;
    }

    /**
     * Gets the value of the didId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDidId() {
        return didId;
    }

    /**
     * Sets the value of the didId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDidId(String value) {
        this.didId = value;
    }

}
