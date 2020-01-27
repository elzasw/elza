
package cz.tacr.elza.ws.types.v1;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * The file element provides access to the content files for the digital object being described.
 * 
 * <p>Java class for file complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="file"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *       &lt;/sequence&gt;
 *       &lt;attGroup ref="{http://elza.tacr.cz/ws/types/v1}mixCore"/&gt;
 *       &lt;attGroup ref="{http://elza.tacr.cz/ws/types/v1}videoMD"/&gt;
 *       &lt;attGroup ref="{http://elza.tacr.cz/ws/types/v1}fileCore"/&gt;
 *       &lt;attribute name="identifier" use="required" type="{http://elza.tacr.cz/ws/types/v1}identifier" /&gt;
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="fileName" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "file")
public class File {

    @XmlAttribute(name = "identifier", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String identifier;
    @XmlAttribute(name = "description")
    protected String description;
    @XmlAttribute(name = "fileName")
    protected String fileName;
    @XmlAttribute(name = "imageHeight")
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger imageHeight;
    @XmlAttribute(name = "imageWidth")
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger imageWidth;
    @XmlAttribute(name = "sourceXDimensionUnit")
    protected UnitOfMeasure sourceXDimensionUnit;
    @XmlAttribute(name = "sourceXDimensionValue")
    protected Float sourceXDimensionValue;
    @XmlAttribute(name = "sourceYDimensionUnit")
    protected UnitOfMeasure sourceYDimensionUnit;
    @XmlAttribute(name = "sourceYDimensionValue")
    protected Float sourceYDimensionValue;
    @XmlAttribute(name = "duration")
    protected String duration;
    @XmlAttribute(name = "checksum")
    protected String checksum;
    @XmlAttribute(name = "checksumType")
    protected ChecksumType checksumType;
    @XmlAttribute(name = "created")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar created;
    @XmlAttribute(name = "mimetype")
    protected String mimetype;
    @XmlAttribute(name = "size")
    protected Long size;

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
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the fileName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the value of the fileName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFileName(String value) {
        this.fileName = value;
    }

    /**
     * Gets the value of the imageHeight property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getImageHeight() {
        return imageHeight;
    }

    /**
     * Sets the value of the imageHeight property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setImageHeight(BigInteger value) {
        this.imageHeight = value;
    }

    /**
     * Gets the value of the imageWidth property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getImageWidth() {
        return imageWidth;
    }

    /**
     * Sets the value of the imageWidth property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setImageWidth(BigInteger value) {
        this.imageWidth = value;
    }

    /**
     * Gets the value of the sourceXDimensionUnit property.
     * 
     * @return
     *     possible object is
     *     {@link UnitOfMeasure }
     *     
     */
    public UnitOfMeasure getSourceXDimensionUnit() {
        return sourceXDimensionUnit;
    }

    /**
     * Sets the value of the sourceXDimensionUnit property.
     * 
     * @param value
     *     allowed object is
     *     {@link UnitOfMeasure }
     *     
     */
    public void setSourceXDimensionUnit(UnitOfMeasure value) {
        this.sourceXDimensionUnit = value;
    }

    /**
     * Gets the value of the sourceXDimensionValue property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getSourceXDimensionValue() {
        return sourceXDimensionValue;
    }

    /**
     * Sets the value of the sourceXDimensionValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setSourceXDimensionValue(Float value) {
        this.sourceXDimensionValue = value;
    }

    /**
     * Gets the value of the sourceYDimensionUnit property.
     * 
     * @return
     *     possible object is
     *     {@link UnitOfMeasure }
     *     
     */
    public UnitOfMeasure getSourceYDimensionUnit() {
        return sourceYDimensionUnit;
    }

    /**
     * Sets the value of the sourceYDimensionUnit property.
     * 
     * @param value
     *     allowed object is
     *     {@link UnitOfMeasure }
     *     
     */
    public void setSourceYDimensionUnit(UnitOfMeasure value) {
        this.sourceYDimensionUnit = value;
    }

    /**
     * Gets the value of the sourceYDimensionValue property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getSourceYDimensionValue() {
        return sourceYDimensionValue;
    }

    /**
     * Sets the value of the sourceYDimensionValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setSourceYDimensionValue(Float value) {
        this.sourceYDimensionValue = value;
    }

    /**
     * Gets the value of the duration property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDuration() {
        return duration;
    }

    /**
     * Sets the value of the duration property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDuration(String value) {
        this.duration = value;
    }

    /**
     * Gets the value of the checksum property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Sets the value of the checksum property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setChecksum(String value) {
        this.checksum = value;
    }

    /**
     * Gets the value of the checksumType property.
     * 
     * @return
     *     possible object is
     *     {@link ChecksumType }
     *     
     */
    public ChecksumType getChecksumType() {
        return checksumType;
    }

    /**
     * Sets the value of the checksumType property.
     * 
     * @param value
     *     allowed object is
     *     {@link ChecksumType }
     *     
     */
    public void setChecksumType(ChecksumType value) {
        this.checksumType = value;
    }

    /**
     * Gets the value of the created property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getCreated() {
        return created;
    }

    /**
     * Sets the value of the created property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setCreated(XMLGregorianCalendar value) {
        this.created = value;
    }

    /**
     * Gets the value of the mimetype property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMimetype() {
        return mimetype;
    }

    /**
     * Sets the value of the mimetype property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMimetype(String value) {
        this.mimetype = value;
    }

    /**
     * Gets the value of the size property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getSize() {
        return size;
    }

    /**
     * Sets the value of the size property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setSize(Long value) {
        this.size = value;
    }

}
