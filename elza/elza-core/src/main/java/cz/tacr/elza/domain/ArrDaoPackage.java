package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Informační balíček digitalizátů (digitálních archivních objektů DAO).
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao_package")
public class ArrDaoPackage {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer daoPackageId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDigitalRepository.class)
    @JoinColumn(name = "digitalRepositoryId", nullable = false)
    private ArrDigitalRepository digitalRepository;

    @Column(name = "digitalRepositoryId", nullable = false, insertable = false, updatable = false)
    private Integer digitalRepositoryId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDaoBatchInfo.class)
    @JoinColumn(name = "daoBatchInfoId")
    private ArrDaoBatchInfo daoBatchInfo;

    @Column(length = StringLength.LENGTH_1000, unique = true)
    private String code;

    public Integer getDaoPackageId() {
        return daoPackageId;
    }

    public void setDaoPackageId(final Integer daoPackageId) {
        this.daoPackageId = daoPackageId;
    }

    public ArrFund getFund() {
        return fund;
    }

    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    public ArrDigitalRepository getDigitalRepository() {
        return digitalRepository;
    }

    public void setDigitalRepository(final ArrDigitalRepository digitalRepository) {
        this.digitalRepository = digitalRepository;
        this.digitalRepositoryId = digitalRepository == null ? null : digitalRepository.getExternalSystemId();
    }

    public Integer getDigitalRepositoryId() {
        return digitalRepositoryId;
    }

    public ArrDaoBatchInfo getDaoBatchInfo() {
        return daoBatchInfo;
    }

    public void setDaoBatchInfo(final ArrDaoBatchInfo daoBatchInfo) {
        this.daoBatchInfo = daoBatchInfo;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }
}
