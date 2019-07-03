
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Package of one or more digital archival objects. One package have to contain DAOs exactly for one fund.
 * 
 * <p>Java class for daoPackage complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="daoPackage"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="daoBatchInfo" type="{http://elza.tacr.cz/ws/types/v1}daoBatchInfo" minOccurs="0"/&gt;
 *         &lt;element name="daoset" type="{http://elza.tacr.cz/ws/types/v1}daoset"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="fundIdentifier" type="{http://elza.tacr.cz/ws/types/v1}identifier" /&gt;
 *       &lt;attribute name="identifier" type="{http://elza.tacr.cz/ws/types/v1}identifier" /&gt;
 *       &lt;attribute name="repositoryIdentifier" type="{http://elza.tacr.cz/ws/types/v1}identifier" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "daoPackage", propOrder = {
    "daoBatchInfo",
    "daoset"
})
public class DaoPackage {

    protected DaoBatchInfo daoBatchInfo;
    @XmlElement(required = true)
    protected Daoset daoset;
    @XmlAttribute(name = "fundIdentifier")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String fundIdentifier;
    @XmlAttribute(name = "identifier")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String identifier;
    @XmlAttribute(name = "repositoryIdentifier")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String repositoryIdentifier;

    /**
     * Gets the value of the daoBatchInfo property.
     * 
     * @return
     *     possible object is
     *     {@link DaoBatchInfo }
     *     
     */
    public DaoBatchInfo getDaoBatchInfo() {
        return daoBatchInfo;
    }

    /**
     * Sets the value of the daoBatchInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link DaoBatchInfo }
     *     
     */
    public void setDaoBatchInfo(DaoBatchInfo value) {
        this.daoBatchInfo = value;
    }

    /**
     * Gets the value of the daoset property.
     * 
     * @return
     *     possible object is
     *     {@link Daoset }
     *     
     */
    public Daoset getDaoset() {
        return daoset;
    }

    /**
     * Sets the value of the daoset property.
     * 
     * @param value
     *     allowed object is
     *     {@link Daoset }
     *     
     */
    public void setDaoset(Daoset value) {
        this.daoset = value;
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

    /**
     * Gets the value of the identifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the value of the identifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdentifier(String value) {
        this.identifier = value;
    }

    /**
     * Gets the value of the repositoryIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRepositoryIdentifier() {
        return repositoryIdentifier;
    }

    /**
     * Sets the value of the repositoryIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRepositoryIdentifier(String value) {
        this.repositoryIdentifier = value;
    }

}
