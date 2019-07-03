
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;


/**
 * Request to synchronize daos
 * 
 * <p>Java class for daosSyncRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="daosSyncRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="username" type="{http://elza.tacr.cz/ws/types/v1}username" minOccurs="0"/&gt;
 *         &lt;element name="dids" type="{http://elza.tacr.cz/ws/types/v1}dids"/&gt;
 *         &lt;element name="daoSyncRequest" type="{http://elza.tacr.cz/ws/types/v1}daoSyncRequest" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="fundIdentifier" type="{http://elza.tacr.cz/ws/types/v1}identifier" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "daosSyncRequest", propOrder = {
    "username",
    "dids",
    "daoSyncRequest"
})
public class DaosSyncRequest {

    protected String username;
    @XmlElement(required = true)
    protected Dids dids;
    @XmlElement(required = true)
    protected List<DaoSyncRequest> daoSyncRequest;
    @XmlAttribute(name = "fundIdentifier")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String fundIdentifier;

    /**
     * Gets the value of the username property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the value of the username property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUsername(String value) {
        this.username = value;
    }

    /**
     * Gets the value of the dids property.
     * 
     * @return
     *     possible object is
     *     {@link Dids }
     *     
     */
    public Dids getDids() {
        return dids;
    }

    /**
     * Sets the value of the dids property.
     * 
     * @param value
     *     allowed object is
     *     {@link Dids }
     *     
     */
    public void setDids(Dids value) {
        this.dids = value;
    }

    /**
     * Gets the value of the daoSyncRequest property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the daoSyncRequest property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDaoSyncRequest().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DaoSyncRequest }
     * 
     * 
     */
    public List<DaoSyncRequest> getDaoSyncRequest() {
        if (daoSyncRequest == null) {
            daoSyncRequest = new ArrayList<DaoSyncRequest>();
        }
        return this.daoSyncRequest;
    }

    /**
     * Gets the value of the fundIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFundIdentifier() {
        return fundIdentifier;
    }

    /**
     * Sets the value of the fundIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFundIdentifier(String value) {
        this.fundIdentifier = value;
    }

}
