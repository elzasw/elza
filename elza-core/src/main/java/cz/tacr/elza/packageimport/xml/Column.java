package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * ColumnsDefinition.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "column")
public class Column {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "data-type", required = true)
    private String dataType;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "width", required = true)
    private Integer width;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(final String dataType) {
        this.dataType = dataType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(final Integer width) {
        this.width = width;
    }
}
