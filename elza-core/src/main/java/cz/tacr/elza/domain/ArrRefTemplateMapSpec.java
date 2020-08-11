package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "arr_ref_template_map_spec")
public class ArrRefTemplateMapSpec {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer refTemplateMapSpecId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrRefTemplateMapType.class)
    @JoinColumn(name = "refTemplateMapTypeId", nullable = false)
    private ArrRefTemplateMapType refTemplateMapType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "fromItemSpecId")
    private RulItemSpec fromItemSpec;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "toItemSpecId")
    private RulItemSpec toItemSpec;

    public Integer getRefTemplateMapSpecId() {
        return refTemplateMapSpecId;
    }

    public void setRefTemplateMapSpecId(Integer refTemplateMapSpecId) {
        this.refTemplateMapSpecId = refTemplateMapSpecId;
    }

    public ArrRefTemplateMapType getRefTemplateMapType() {
        return refTemplateMapType;
    }

    public void setRefTemplateMapType(ArrRefTemplateMapType refTemplateMapType) {
        this.refTemplateMapType = refTemplateMapType;
    }

    public RulItemSpec getFromItemSpec() {
        return fromItemSpec;
    }

    public void setFromItemSpec(RulItemSpec fromItemSpec) {
        this.fromItemSpec = fromItemSpec;
    }

    public RulItemSpec getToItemSpec() {
        return toItemSpec;
    }

    public void setToItemSpec(RulItemSpec toItemSpec) {
        this.toItemSpec = toItemSpec;
    }
}
