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

@Entity(name = "arr_ref_template_map_type")
public class ArrRefTemplateMapType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer refTemplateMapTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrRefTemplate.class)
    @JoinColumn(name = "refTemplateId", nullable = false)
    private ArrRefTemplate refTemplate;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "fromItemTypeId", nullable = false)
    private RulItemType formItemType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "toItemTypeId", nullable = false)
    private RulItemType toItemType;

    @Column(name = "fromParentLevel")
    private Boolean fromParentLevel;

    @Column(name = "mapAllSpec")
    private Boolean mapAllSpec;

    public Integer getRefTemplateMapTypeId() {
        return refTemplateMapTypeId;
    }

    public void setRefTemplateMapTypeId(Integer refTemplateMapTypeId) {
        this.refTemplateMapTypeId = refTemplateMapTypeId;
    }

    public ArrRefTemplate getRefTemplate() {
        return refTemplate;
    }

    public void setRefTemplate(ArrRefTemplate refTemplate) {
        this.refTemplate = refTemplate;
    }

    public RulItemType getFormItemType() {
        return formItemType;
    }

    public void setFormItemType(RulItemType formItemType) {
        this.formItemType = formItemType;
    }

    public RulItemType getToItemType() {
        return toItemType;
    }

    public void setToItemType(RulItemType toItemType) {
        this.toItemType = toItemType;
    }

    public Boolean getFromParentLevel() {
        return fromParentLevel;
    }

    public void setFromParentLevel(Boolean fromParentLevel) {
        this.fromParentLevel = fromParentLevel;
    }

    public Boolean getMapAllSpec() {
        return mapAllSpec;
    }

    public void setMapAllSpec(Boolean mapAllSpec) {
        this.mapAllSpec = mapAllSpec;
    }
}
