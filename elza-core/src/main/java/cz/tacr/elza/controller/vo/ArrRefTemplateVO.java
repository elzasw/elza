package cz.tacr.elza.controller.vo;

import java.util.List;

public class ArrRefTemplateVO {

    private Integer id;

    private String name;

    private Integer itemTypeId;

    private List<ArrRefTemplateMapTypeVO> refTemplateMapTypeVOList;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getItemTypeId() {
        return itemTypeId;
    }

    public void setItemTypeId(Integer itemTypeId) {
        this.itemTypeId = itemTypeId;
    }

    public List<ArrRefTemplateMapTypeVO> getRefTemplateMapTypeVOList() {
        return refTemplateMapTypeVOList;
    }

    public void setRefTemplateMapTypeVOList(List<ArrRefTemplateMapTypeVO> refTemplateMapTypeVOList) {
        this.refTemplateMapTypeVOList = refTemplateMapTypeVOList;
    }
}
