package cz.tacr.elza.suzap.v1.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;

/**
 * Odkaz do rejstříku.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "desc-item-record-ref", namespace = NamespaceInfo.NAMESPACE)
public class DescItemRecordRef extends AbstractDescItem {

    /** Odkaz do seznamu rejstříkových hesel. */
    @XmlIDREF
    @XmlElement(required = true)
    private Record record;

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }
}
