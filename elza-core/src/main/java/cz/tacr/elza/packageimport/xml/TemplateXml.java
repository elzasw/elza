package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.packageimport.xml.common.OtherCodes;

/**
 * VO Template.
 *
 * @since 20.6.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "template")
public class TemplateXml {

    @XmlAttribute(name = "output-type", required = true)
    private String outputType;

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "engine", required = true)
    private String engine;

    @XmlElement(name = "directory", required = true)
    private String directory;

    @XmlAttribute(name = "mime-type", required = true)
    private String mimeType;

    @XmlAttribute(name = "extension", required = true)
    private String extension;

    /**
     * List of invalidated output types which are handled
     * by this output
     */
    @XmlElement(name = "other-codes")
    private OtherCodes otherCodes;

    public OtherCodes getOtherCodes() {
        return otherCodes;
    }

    public void setOtherCodes(OtherCodes otherCodes) {
        this.otherCodes = otherCodes;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(final String outputType) {
        this.outputType = outputType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(final String engine) {
        this.engine = engine;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(final String directory) {
        this.directory = directory;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
    }
}
