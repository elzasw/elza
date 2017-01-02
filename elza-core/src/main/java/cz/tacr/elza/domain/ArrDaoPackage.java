package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Implementace {@link cz.tacr.elza.api.ArrDaoPackage}
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao_package")
public class ArrDaoPackage implements cz.tacr.elza.api.ArrDaoPackage<ArrFund, ArrDigitalRepository, ArrDaoBatchInfo> {

    @Id
    @GeneratedValue
    private Integer daoPackageId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitalRepository.class)
    @JoinColumn(name = "digitalRepositoryId", nullable = false)
    private ArrDigitalRepository digitalRepository;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDaoBatchInfo.class)
    @JoinColumn(name = "daoBatchInfoId")
    private ArrDaoBatchInfo daoBatchInfo;

    @Column(length = StringLength.LENGTH_50, unique = true)
    private String code;

    @Override
    public Integer getDaoPackageId() {
        return daoPackageId;
    }

    @Override
    public void setDaoPackageId(final Integer daoPackageId) {
        this.daoPackageId = daoPackageId;
    }

    @Override
    public ArrFund getFund() {
        return fund;
    }

    @Override
    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    @Override
    public ArrDigitalRepository getDigitalRepository() {
        return digitalRepository;
    }

    @Override
    public void setDigitalRepository(final ArrDigitalRepository digitalRepository) {
        this.digitalRepository = digitalRepository;
    }

    @Override
    public ArrDaoBatchInfo getDaoBatchInfo() {
        return daoBatchInfo;
    }

    @Override
    public void setDaoBatchInfo(final ArrDaoBatchInfo daoBatchInfo) {
        this.daoBatchInfo = daoBatchInfo;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }
}
