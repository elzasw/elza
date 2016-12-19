
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Structure for importing one or more packages with digital archival objects.
 * 
 * <p>Java class for daoImport complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="daoImport"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="daoPackages" type="{http://elza.tacr.cz/ws/types/v1}daoPackages"/&gt;
 *         &lt;element name="daoLinks" type="{http://elza.tacr.cz/ws/types/v1}daoLinks"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "daoImport", propOrder = {
    "daoPackages",
    "daoLinks"
})
public class DaoImport {

    @XmlElement(required = true)
    protected DaoPackages daoPackages;
    @XmlElement(required = true)
    protected DaoLinks daoLinks;

    /**
     * Gets the value of the daoPackages property.
     * 
     * @return
     *     possible object is
     *     {@link DaoPackages }
     *     
     */
    public DaoPackages getDaoPackages() {
        return daoPackages;
    }

    /**
     * Sets the value of the daoPackages property.
     * 
     * @param value
     *     allowed object is
     *     {@link DaoPackages }
     *     
     */
    public void setDaoPackages(DaoPackages value) {
        this.daoPackages = value;
    }

    /**
     * Gets the value of the daoLinks property.
     * 
     * @return
     *     possible object is
     *     {@link DaoLinks }
     *     
     */
    public DaoLinks getDaoLinks() {
        return daoLinks;
    }

    /**
     * Sets the value of the daoLinks property.
     * 
     * @param value
     *     allowed object is
     *     {@link DaoLinks }
     *     
     */
    public void setDaoLinks(DaoLinks value) {
        this.daoLinks = value;
    }

}
