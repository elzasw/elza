package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.DaDao;

public class DaDaoVO {
    private Integer daoId;
    private Integer aipId;
    private DaDao.DaoType type;
    private String code;
    private String label;

    public Integer getDaoId() {
        return daoId;
    }

    public void setDaoId(Integer daoId) {
        this.daoId = daoId;
    }

    public Integer getAipId() {
        return aipId;
    }

    public void setAipId(Integer aipId) {
        this.aipId = aipId;
    }

    public DaDao.DaoType getType() {
        return type;
    }

    public void setType(DaDao.DaoType type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
