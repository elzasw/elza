package cz.tacr.elza.controller.vo;

/**
 * Import osoby z externího systému.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 1. 12. 2016
 */
public class RecordImportVO {

    private Integer scopeId;

    private String interpiRecordId;

    private Integer systemId;

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    public String getInterpiRecordId() {
        return interpiRecordId;
    }

    public void setInterpiRecordId(final String interpiRecordId) {
        this.interpiRecordId = interpiRecordId;
    }

    public Integer getSystemId() {
        return systemId;
    }

    public void setSystemId(final Integer systemId) {
        this.systemId = systemId;
    }
}
