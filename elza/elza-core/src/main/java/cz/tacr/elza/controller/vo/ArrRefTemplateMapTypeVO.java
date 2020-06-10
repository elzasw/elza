package cz.tacr.elza.controller.vo;

import java.util.List;

public class ArrRefTemplateMapTypeVO {

    private Integer id;

    private Integer fromItemTypeId;

    private Integer toItemTypeId;

    private Boolean fromParentLevel;

    private Boolean mapAllSpec;

    private List<ArrRefTemplateMapSpecVO> refTemplateMapSpecVOList;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFromItemTypeId() {
        return fromItemTypeId;
    }

    public void setFromItemTypeId(Integer fromItemTypeId) {
        this.fromItemTypeId = fromItemTypeId;
    }

    public Integer getToItemTypeId() {
        return toItemTypeId;
    }

    public void setToItemTypeId(Integer toItemTypeId) {
        this.toItemTypeId = toItemTypeId;
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

    public List<ArrRefTemplateMapSpecVO> getRefTemplateMapSpecVOList() {
        return refTemplateMapSpecVOList;
    }

    public void setRefTemplateMapSpecVOList(List<ArrRefTemplateMapSpecVO> refTemplateMapSpecVOList) {
        this.refTemplateMapSpecVOList = refTemplateMapSpecVOList;
    }
}
