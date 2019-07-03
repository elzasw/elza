
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Tag represents link between digital archival object and did.
 * 
 * <p>Java class for daoLink complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="daoLink"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="daoIdentifier" type="{http://elza.tacr.cz/ws/types/v1}identifier"/&gt;
 *         &lt;element name="didIdentifier" type="{http://elza.tacr.cz/ws/types/v1}identifier"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="repositoryIdentifier" use="required" type="{http://elza.tacr.cz/ws/types/v1}identifier" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "daoLink", propOrder = {
    "daoIdentifier",
    "didIdentifier"
})
public class DaoLink {

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String daoIdentifier;
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String didIdentifier;
    @XmlAttribute(name = "repositoryIdentifier", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String repositoryIdentifier;

    /**
     * Gets the value of the daoIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDaoIdentifier() {
        return daoIdentifier;
    }

    /**
     * Sets the value of the daoIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDaoIdentifier(String value) {
        this.daoIdentifier = value;
    }

    /**
     * Gets the value of the didIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDidIdentifier() {
        return didIdentifier;
    }

    /**
     * Sets the value of the didIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDidIdentifier(String value) {
        this.didIdentifier = value;
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
