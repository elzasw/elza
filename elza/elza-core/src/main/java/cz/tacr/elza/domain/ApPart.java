package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import org.hibernate.Length;

import jakarta.persistence.*;
import java.util.List;

/**
 * Part je popisem části přístupového bodu nebo jména. Part je tvořen prvky popisu. Part má svůj vnitřní stav,
 * který je uložen v atributu state. Part je možné reprezentovat textovou hodnotou. Ta je uložena v hodnotě value.
 *
 * @since 17.07.2018
 */
@Entity
@Table(name = "ap_part")
public class ApPart implements AccessPointPart {

    public static final String PART_ID = "partId";
    public static final String ACCESS_POINT_ID = "accessPointId";
    public static final String DELETE_CHANGE_ID = "deleteChangeId";
    public static final String PARENT_PART = "parentPart";
    public static final String ITEMS = "items";
    public static final String INDICES = "indices";
    public static final String ACCESS_POINT = "accessPoint";
    public static final String PART_TYPE = "partType";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer partId;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM)
    private ApStateEnum state;

    @Column(length = Length.LONG) // Hibernate long text field
    private String errorDescription;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPartType.class)
    @JoinColumn(name = "part_type_id", nullable = false)
    private RulPartType partType;

    @Column(name = "part_type_id", updatable = false, insertable = false)
    private Integer partTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "parent_part_id")
    private ApPart parentPart;

    @Column(name = "parent_part_id", updatable = false, insertable = false)
    private Integer parentPartId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    private ApChange createChange;

    @Column(name = "create_change_id", nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "last_change_id", nullable = false)
    private ApChange lastChange;

    @Column(name = "last_change_id", nullable = false, updatable = false, insertable = false)
    private Integer lastChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "delete_change_id")
    private ApChange deleteChange;

    @Column(name = "delete_change_id", updatable = false, insertable = false)
    private Integer deleteChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "accessPointId", nullable = false)
    private ApAccessPoint accessPoint;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer accessPointId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApKeyValue.class)
    @JoinColumn(name = "key_value_id")
    private ApKeyValue keyValue;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "part")
    private List<ApItem> items;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "part")
    private List<ApIndex> indices;

    public Integer getPartId() {
        return partId;
    }

    public void setPartId(final Integer partId) {
        this.partId = partId;
    }

    public ApStateEnum getState() {
        return state;
    }

    public void setState(final ApStateEnum state) {
        this.state = state;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(final String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public RulPartType getPartType() {
        return partType;
    }

    public void setPartType(final RulPartType partType) {
        this.partType = partType;
        this.partTypeId = partType != null ? partType.getPartTypeId() : null;
    }

    public ApPart getParentPart() {
        return parentPart;
    }

    public Integer getParentPartId() {
        return parentPartId;
    }

    public void setParentPart(ApPart parentPart) {
        this.parentPart = parentPart;
        this.parentPartId = parentPart != null ? parentPart.getPartId() : null;
    }

    public ApChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(ApChange createChange) {
        this.createChange = createChange;
        this.createChangeId = createChange != null ? createChange.getChangeId() : null;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public ApChange getLastChange() {
        return lastChange;
    }

    public void setLastChange(ApChange lastChange) {
        this.lastChange = lastChange;
        this.lastChangeId = lastChange != null ? lastChange.getChangeId() : null;
    }

    public Integer getLastChangeId() {
        return lastChangeId;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
        this.deleteChangeId = deleteChange != null ? deleteChange.getChangeId() : null;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
        this.accessPointId = accessPoint != null ? accessPoint.getAccessPointId() : null;
    }

    public Integer getAccessPointId() {
        if (accessPointId != null) {
            return accessPointId;
        } else if (accessPoint != null) {
            return accessPoint.getAccessPointId();
        } else {
            return null;
        }
    }

    public Integer getPartTypeId() {
        return partTypeId;
    }

    public ApKeyValue getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(ApKeyValue keyValue) {
        this.keyValue = keyValue;
    }
}
