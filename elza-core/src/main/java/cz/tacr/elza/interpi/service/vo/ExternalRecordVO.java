package cz.tacr.elza.interpi.service.vo;

import java.util.LinkedList;
import java.util.List;

import cz.tacr.elza.domain.RegRecord;

/**
 * Záznam z externího systému.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public class ExternalRecordVO {

    /** Identifikátor záznamu v externím systému. */
    private String recordId;

    /** Název záznamu. */
    private String name;

    /** Detail. */
    private String detail;

    /** Existující záznamy. */
    private List<PairedRecordVO> pairedRecords = new LinkedList<>();;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(final String detail) {
        this.detail = detail;
    }

    public List<PairedRecordVO> getPairedRecords() {
        return pairedRecords;
    }

    public void setPairedRecords(final List<PairedRecordVO> pairedRecords) {
        this.pairedRecords = pairedRecords;
    }

    public void addPairedRecord(final RegRecord regRecord) {
        PairedRecordVO pairedRecordVO = new PairedRecordVO(regRecord.getScope(), regRecord.getRecordId());
        pairedRecords.add(pairedRecordVO);
    }
}
