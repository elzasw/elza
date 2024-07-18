package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Digitální archivní objekt (digitalizát).
 *
 */
@Table
@Entity(name = "arr_dao_link")
public class ArrDaoLink {

    public static final String TABLE_NAME = "arr_dao_link";

    public static final String FIELD_CREATE_CHANGE = "createChange";
    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";

    public static final String FIELD_DELETE_CHANGE = "deleteChange";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";

    public static final String FIELD_NODE = "node";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer daoLinkId;

	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @Column(name = "nodeId", updatable = false, insertable = false)
    private Integer nodeId;

	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrDao.class)
    @JoinColumn(name = "daoId")
    private ArrDao dao;

    @Column(name = "daoId", updatable = false, insertable = false)
    private Integer daoId;

	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_CREATE_CHANGE_ID, nullable = false)
    private ArrChange createChange;

    @Column(name = FIELD_CREATE_CHANGE_ID, updatable = false, insertable = false)
    private Integer createChangeId;

	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_DELETE_CHANGE_ID)
    private ArrChange deleteChange;

    @Column(name = FIELD_DELETE_CHANGE_ID, updatable = false, insertable = false)
    private Integer deleteChangeId;

    @Column(length = StringLength.LENGTH_250)
    private String scenario;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaAip.class)
    @JoinColumn(name = "aip_id", nullable = false)
    private DaAip aip;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaDao.class)
    @JoinColumn(name = "da_dao_id")
    private DaDao daDao;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", length = StringLength.LENGTH_ENUM, nullable = false)
    private LinkType linkType;

    public enum LinkType {
        AIP,
        PART_AIP,
        COMPONENT_AIP
    }

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

    public void setCreateChange(final ArrChange createChange,
                                final Integer createChangeId) {
        this.createChange = createChange;
        this.createChangeId = createChangeId;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public DaAip getAip() {
        return aip;
    }

    public void setAip(DaAip aip) {
        this.aip = aip;
    }

    public DaDao getDaDao() {
        return daDao;
    }

    public void setDaDao(DaDao daDao) {
        this.daDao = daDao;
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(LinkType linkType) {
        this.linkType = linkType;
    }
}
