package cz.tacr.elza.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Digitální archivní objekt (digitalizát).
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
@Table
@Entity(name = "arr_dao_link")
public class ArrDaoLink implements Serializable {

    @Id
    @GeneratedValue
    private Integer daoLinkId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @Column(name = "nodeId", updatable = false, insertable = false)
    private Integer nodeId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrDao.class)
    @JoinColumn(name = "daoId", nullable = false)
    private ArrDao dao;

    @Column(name = "daoId", updatable = false, insertable = false)
    private Integer daoId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @Column(name = "createChangeId", updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId")
    private ArrChange deleteChange;

    @Column(name = "deleteChangeId", updatable = false, insertable = false)
    private Integer deleteChangeId;

    public Integer getDaoLinkId() {
        return daoLinkId;
    }

    public void setDaoLinkId(final Integer daoLinkId) {
        this.daoLinkId = daoLinkId;
    }

    public ArrNode getNode() {
        return node;
    }

    public void setNode(final ArrNode node) {
        this.node = node;
        this.nodeId = node == null ? null : node.getNodeId();
    }

    public ArrDao getDao() {
        return dao;
    }

    public void setDao(final ArrDao dao) {
        this.dao = dao;
        this.daoId = dao == null ? null : dao.getDaoId();
    }

    public ArrChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
        this.createChangeId = createChange == null ? null : createChange.getChangeId();
    }

    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(final ArrChange deleteChange) {
        this.deleteChange = deleteChange;
        this.deleteChangeId = deleteChange == null ? null : deleteChange.getChangeId();
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getDaoId() {
        return daoId;
    }

    public void setDaoId(final Integer daoId) {
        this.daoId = daoId;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public void setCreateChangeId(final Integer createChangeId) {
        this.createChangeId = createChangeId;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public void setDeleteChangeId(final Integer deleteChangeId) {
        this.deleteChangeId = deleteChangeId;
    }
}
