package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Tabulka.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "desc-item-json-table", namespace = NamespaceInfo.NAMESPACE)
public class DescItemJsonTable extends AbstractDescItem {

    @XmlElement(name = "value", required = true)
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
