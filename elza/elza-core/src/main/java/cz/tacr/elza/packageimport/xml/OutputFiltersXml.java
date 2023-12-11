package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
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
