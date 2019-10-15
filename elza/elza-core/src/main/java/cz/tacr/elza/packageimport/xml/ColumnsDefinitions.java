package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO ColumnsDefinitions.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "columns-definitions")
@XmlType(name = "columns-definitions")
public class ColumnsDefinitions {

    @XmlElement(name = "column", required = true)
    private List<Column> columns;

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(final List<Column> columns) {
        this.columns = columns;
    }
}
