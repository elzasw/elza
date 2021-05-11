package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "package-output-filters")
@XmlType(name = "package-output-filters")
public class OutputFiltersXml {

    @XmlElement(name = "package-output-filter", required = true)
    private List<OutputFilterXml> packageOutputFilters;

    public List<OutputFilterXml> getPackageOutputFilters() {
        return packageOutputFilters;
    }

    public void setPackageOutputFilters(List<OutputFilterXml> packageOutputFilters) {
        this.packageOutputFilters = packageOutputFilters;
    }
}
