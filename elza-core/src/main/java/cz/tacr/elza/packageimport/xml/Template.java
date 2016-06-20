package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;

/**
 * VO Template.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "template")
public class Template {

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

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
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

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
