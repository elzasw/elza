package cz.tacr.elza.controller.vo.nodes.descitems;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.*;

import javax.persistence.EntityManager;

public class ArrItemUriRefVO extends ArrItemVO {

    private Integer nodeId;

    private String value;

    private String description;

    private Integer refTemplateId;

    public ArrItemUriRefVO() {

    }

    public ArrItemUriRefVO(ArrItem item, String value, String description) {
        super(item);
        this.value = value;
        this.description = description;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRefTemplateId() {
        return refTemplateId;
    }

    public void setRefTemplateId(Integer refTemplateId) {
        this.refTemplateId = refTemplateId;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataUriRef data = new ArrDataUriRef();
        data.setUriRefValue(value);
        data.setDescription(description);
        data.setDataType(DataType.URI_REF.getEntity());
        data.setSchema(ArrDataUriRef.createSchema(value));
        return data;
    }

}
