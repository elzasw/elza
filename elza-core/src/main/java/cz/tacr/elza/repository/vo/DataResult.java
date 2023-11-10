package cz.tacr.elza.repository.vo;


import cz.tacr.elza.domain.RulDataType;

public class DataResult {

    private Integer dataId;

    private RulDataType rulDataType;

    public DataResult(Integer dataId, RulDataType rulDataType) {
        this.dataId = dataId;
        this.rulDataType = rulDataType;
    }

    public Integer getDataId() {
        return dataId;
    }

    public void setDataId(Integer dataId) {
        this.dataId = dataId;
    }

    public RulDataType getRulDataType() {
        return rulDataType;
    }

    public void setRulDataType(RulDataType rulDataType) {
        this.rulDataType = rulDataType;
    }
}
