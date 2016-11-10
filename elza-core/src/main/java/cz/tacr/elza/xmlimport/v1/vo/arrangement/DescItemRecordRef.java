package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;
import cz.tacr.elza.xmlimport.v1.vo.record.Record;

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
    @XmlAttribute(name = "record-id", required = true)
    private String recordId;

    @XmlTransient
    private Record record;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(final Record record) {
        this.record = record;
    }
}
