
package cz.tacr.elza.ws.types.v1;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Digital archival object might have several related files. Several related files are connected to digital archival object wrapped in this named container.
 * 
 * <p>Java class for relatedFileGroup complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="relatedFileGroup"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="fileGroup" type="{http://elza.tacr.cz/ws/types/v1}fileGroup"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="identifier" type="{http://elza.tacr.cz/ws/types/v1}identifier" /&gt;
 *       &lt;attribute name="label" type="{http://elza.tacr.cz/ws/types/v1}label" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "relatedFileGroup", propOrder = {
    "fileGroup"
})
public class RelatedFileGroup {

    @XmlElement(required = true)
    protected FileGroup fileGroup;
    @XmlAttribute(name = "identifier")
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
