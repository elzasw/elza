package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrDaoPackage;

/**
 * Value objekt {@link ArrDaoPackage}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public class ArrDaoPackageVO {
    private Integer id;
    private String code;
    private String batchInfoCode;
    private String batchInfoLabel;
    private long daoCount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBatchInfoCode() {
        return batchInfoCode;
    }

    public void setBatchInfoCode(String batchInfoCode) {
        this.batchInfoCode = batchInfoCode;
    }

    public String getBatchInfoLabel() {
        return batchInfoLabel;
    }

    public void setBatchInfoLabel(String batchInfoLabel) {
        this.batchInfoLabel = batchInfoLabel;
    }

    public long getDaoCount() {
        return daoCount;
    }

    public void setDaoCount(long daoCount) {
        this.daoCount = daoCount;
    }
}
