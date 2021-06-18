package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
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
