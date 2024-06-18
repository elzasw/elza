package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "da_dao_relation")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class DaDaoRelation {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer daoRelationId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    private DaChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "delete_change_id")
    private DaChange deleteChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaDao.class)
    @JoinColumn(name = "dao_id", nullable = false)
    private DaDao dao;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaDao.class)
    @JoinColumn(name = "parent_dao_id", nullable = false)
    private DaDao parentDao;

    public Integer getDaoRelationId() {
        return daoRelationId;
    }

    public void setDaoRelationId(Integer daoRelationId) {
        this.daoRelationId = daoRelationId;
    }

    public DaChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(DaChange createChange) {
        this.createChange = createChange;
    }

    public DaChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(DaChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public DaDao getDao() {
        return dao;
    }

    public void setDao(DaDao dao) {
        this.dao = dao;
    }

    public DaDao getParentDao() {
        return parentDao;
    }

    public void setParentDao(DaDao parentDao) {
        this.parentDao = parentDao;
    }
}
