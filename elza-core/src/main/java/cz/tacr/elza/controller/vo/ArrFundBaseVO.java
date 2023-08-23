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

    private Integer fundNumber;

    private String mark;

    public ArrFundBaseVO() {
    }

    protected ArrFundBaseVO(final Integer id,
                            final String name,
                            final String internalCode,
                            final Integer fundNumber,
                            final String mark) {
        this.id = id;
        this.name = name;
        this.internalCode = internalCode;
        this.fundNumber = fundNumber;
        this.mark = mark;
    }

    public ArrFundBaseVO(ArrFund fund) {
        this.id = fund.getFundId();
        this.name = fund.getName();
        this.internalCode = fund.getInternalCode();
        this.fundNumber = fund.getFundNumber();
        this.mark = fund.getMark();
    }

    public ArrFundBaseVO(Fund fund) {
        this.id = fund.getId();
        this.name = fund.getName();
        this.internalCode = fund.getInternalCode();
        this.fundNumber = fund.getFundNumber();
        this.mark = fund.getMark();
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

    public Integer getFundNumber() {
        return fundNumber;
    }

    public void setFundNumber(Integer fundNumber) {
        this.fundNumber = fundNumber;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public ArrFund createEntity() {
        ArrFund entity = new ArrFund();
        entity.setFundId(id);
        entity.setName(name);
        entity.setInternalCode(internalCode);
        entity.setFundNumber(fundNumber);
        entity.setMark(mark);
        return entity;
    }

    public static ArrFundBaseVO newInstance(Integer id, String name, String internalCode,
                                            Integer fundNumber, String mark) {
        return new ArrFundBaseVO(id, name, internalCode, fundNumber, mark);
    }

    public static ArrFundBaseVO newInstance(ArrFund fund) {
        return new ArrFundBaseVO(fund);
    }

    public static ArrFundBaseVO newInstance(Fund fund) {
        return new ArrFundBaseVO(fund);
    }
}
