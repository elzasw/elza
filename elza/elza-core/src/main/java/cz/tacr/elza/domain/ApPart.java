package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * Part je popisem části přístupového bodu nebo jména. Part je tvořen prvky popisu. Part má svůj vnitřní stav,
 * který je uložen v atributu state. Part je možné reprezentovat textovou hodnotou. Ta je uložena v hodnotě value.
 *
 * @since 17.07.2018
 */
@Entity
@Table(name = "ap_part")
public class ApPart {

    public static final String PART_ID = "partId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer partId;

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM)
    private ApStateEnum state;

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String errorDescription;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPartType.class)
    @JoinColumn(name = "part_type_id", nullable = false)
    private RulPartType partType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "parent_part_id")
    private ApPart parentPart;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    private ApChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "delete_change_id")
    private ApChange deleteChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "access_point_id", nullable = false)
    private ApAccessPoint accessPoint;

    public Integer getPartId() {
        return partId;
    }

    public void setPartId(final Integer partId) {
        this.partId = partId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
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
    }

    public ApPart getParentPart() {
        return parentPart;
    }

    public void setParentPart(ApPart parentPart) {
        this.parentPart = parentPart;
    }

    public ApChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(ApChange createChange) {
        this.createChange = createChange;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
    }
}
