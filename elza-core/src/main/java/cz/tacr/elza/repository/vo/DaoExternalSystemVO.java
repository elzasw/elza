package cz.tacr.elza.repository.vo;

/**
 * VO pro hromadnou synchronizaci DAO - grupováno přes ID uložiště digitalizátů
 */
public class DaoExternalSystemVO {

    // --- fields ---

    private final Integer daoId;
    private final Integer externalSystemId;

    // --- getters/setters ---

    public Integer getDaoId() {
        return daoId;
    }

    public Integer getExternalSystemId() {
        return externalSystemId;
    }

    // --- constructor ---

    public DaoExternalSystemVO(Integer daoId, Integer externalSystemId) {
        this.daoId = daoId;
        this.externalSystemId = externalSystemId;
    }
}
