package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * Informační balíček digitalizátů (digitálních archivních objektů DAO).
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
public interface ArrDaoPackage<F extends ArrFund, DR extends ArrDigitalRepository, DBI extends ArrDaoBatchInfo> extends Serializable {

    Integer getDaoPackageId();

    void setDaoPackageId(Integer daoPackageId);

    F getFund();

    void setFund(F fund);

    DBI getDaoBatchInfo();

    void setDaoBatchInfo(DBI daoBatchInfo);

    DR getDigitalRepository();

    void setDigitalRepository(DR digitalRepository);

    String getCode();

    void setCode(String code);
}
