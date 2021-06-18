package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrFund;

/**
 * Základní VO pro archivní pomůcku.
 *
 */
public class ArrFundBaseVO {

    private Integer id;

    private String name;

    private String internalCode;

    public ArrFundBaseVO() {
    }

    public ArrFundBaseVO(Integer id, String name, String internalCode) {
        this.id = id;
        this.name = name;
        this.internalCode = internalCode;
    }

    public ArrFundBaseVO(ArrFund fund) {
        this.id = fund.getFundId();
        this.name = fund.getName();
        this.internalCode = fund.getInternalCode();
    }

    public ArrFundBaseVO(Fund fund) {
        this.id = fund.getId();
        this.name = fund.getName();
        this.internalCode = fund.getInternalCode();
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(String internalCode) {
        this.internalCode = internalCode;
    }

    public ArrFund createEntity() {
        ArrFund entity = new ArrFund();
        entity.setFundId(id);
        entity.setName(name);
        entity.setInternalCode(internalCode);
        return entity;
    }

    public static ArrFundBaseVO newInstance(Integer id, String name, String internalCode) {
        return new ArrFundBaseVO(id, name, internalCode);
    }

    public static ArrFundBaseVO newInstance(ArrFund fund) {
        return new ArrFundBaseVO(fund);
    }

    public static ArrFundBaseVO newInstance(Fund fund) {
        return new ArrFundBaseVO(fund);
    }
}
