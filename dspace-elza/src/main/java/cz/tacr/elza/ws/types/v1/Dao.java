
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;


/**
 * Child element of <did> used for linking to born digital records or a digital representation of the materials being described. 
 * 
 * Digital archival object might contain link to several files in repository. Digital representations may include graphic images, audio or video clips, images of text pages, and electronic transcriptions of text.
 * 
 * Original source: EAD3
 * 
 * Note: Some types of DAO does not have to have link to any file.
 * 
 * <p>Java class for dao complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="dao"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="fileGroup" type="{http://elza.tacr.cz/ws/types/v1}fileGroup" minOccurs="0"/&gt;
 *         &lt;element name="relatedFileGroup" type="{http://elza.tacr.cz/ws/types/v1}relatedFileGroup" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="identifier" use="required" type="{http://elza.tacr.cz/ws/types/v1}identifier" /&gt;
 *       &lt;attribute name="label" type="{http://elza.tacr.cz/ws/types/v1}label" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dao", propOrder = {
    "fileGroup",
    "relatedFileGroup"
})
public class Dao {

    protected FileGroup fileGroup;
    protected List<RelatedFileGroup> relatedFileGroup;
    @XmlAttribute(name = "identifier", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String identifier;
    @XmlAttribute(name = "label")
    protected String label;

    /**
     * Gets the value of the fileGroup property.
     * 
     * @return
     *     possible object is
     *     {@link FileGroup }
     *     
     */
    public FileGroup getFileGroup() {
        return fileGroup;
    }

    /**
     * Sets the value of the fileGroup property.
     * 
     * @param value
     *     allowed object is
     *     {@link FileGroup }
     *     
     */
    public void setFileGroup(FileGroup value) {
        this.fileGroup = value;
    }

    /**
     * Gets the value of the relatedFileGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the relatedFileGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRelatedFileGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RelatedFileGroup }
     * 
     * 
     */
    public List<RelatedFileGroup> getRelatedFileGroup() {
        if (relatedFileGroup == null) {
            relatedFileGroup = new ArrayList<RelatedFileGroup>();
        }
        return this.relatedFileGroup;
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
     * Gets the value of the label property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLabel(String value) {
        this.label = value;
    }

}
