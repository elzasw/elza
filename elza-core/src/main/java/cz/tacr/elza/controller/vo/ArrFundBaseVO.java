package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrFund;

/**
 * Základní VO pro archivní pomůcku.
 *
 */
public class ArrFundBaseVO {

    private Integer id;

    private String name;

    public ArrFundBaseVO() {

    }

    public ArrFundBaseVO(ArrFund fund) {
        this.id = fund.getFundId();
        this.name = fund.getName();
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

    public ArrFund createEntity() {
        ArrFund entity = new ArrFund();
        entity.setFundId(id);
        entity.setName(name);
        return entity;
    }

    public static ArrFundBaseVO newInstance(ArrFund fund) {
        ArrFundBaseVO fundBaseVO = new ArrFundBaseVO(fund);
        return fundBaseVO;
    }
}
