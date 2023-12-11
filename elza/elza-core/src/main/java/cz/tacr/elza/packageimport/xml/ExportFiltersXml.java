package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "package-export-filters")
@XmlType(name = "package-export-filters")
public class ExportFiltersXml {

    @XmlElement(name = "package-export-filter", required = true)
    private List<ExportFilterXml> packageExportFilters;

    public List<ExportFilterXml> getPackageExportFilters() {
        return packageExportFilters;
    }

    public void setPackageExportFilters(List<ExportFilterXml> packageExportFilters) {
        this.packageExportFilters = packageExportFilters;
    }
}
