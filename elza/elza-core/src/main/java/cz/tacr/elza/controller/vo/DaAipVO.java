package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.DaAip;

public class DaAipVO {

    private Integer aipId;

    private String code;

    private Integer digitalRepositoryId;

    public Integer getAipId() {
        return aipId;
    }

    public void setAipId(Integer aipId) {
        this.aipId = aipId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getDigitalRepositoryId() {
        return digitalRepositoryId;
    }

    public void setDigitalRepositoryId(Integer digitalRepositoryId) {
        this.digitalRepositoryId = digitalRepositoryId;
    }


    public static DaAipVO newInstance(DaAip src) {
        DaAipVO vo = new DaAipVO();
        vo.setAipId(src.getAipId());
        vo.setCode(src.getCode());
        vo.setDigitalRepositoryId(src.getDigitalRepository().getExternalSystemId());
        return vo;
    }
}
