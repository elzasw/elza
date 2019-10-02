package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


/**
 * VO PackageInfo - informace o importovaném package.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "package")
public class PackageInfo {

    @XmlElement(name = "code", required = true)
    private String code;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "version", required = true)
    private Integer version;

    @XmlElement(name = "description", nillable = true)
    private String description;

    @XmlElement(name = "dependency", required = true)
    @XmlElementWrapper(name = "dependencies")
    private List<PackageDependency> dependencies;

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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<PackageDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(final List<PackageDependency> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public String toString() {
        return "PackageInfo{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", description='" + description + '\'' +
                ", dependencies='" + dependencies + '\'' +
                '}';
    }
}
