package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

import javax.persistence.EntityManager;
import java.util.Objects;

public class ApItemUriRefVO extends ApItemVO {

    /*
     ** Node
     */
    private String schema;

    private String value;

    private String description;

    private ArrNodeVO node;

    private Integer nodeId;

    public ApItemUriRefVO() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public ApItemUriRefVO(final ApItem item) {
        super(item);
        ArrDataUriRef data = (ArrDataUriRef) item.getData();
        value = data == null ? null : data.getUriRefValue();
        schema = data == null ? null : data.getSchema();
        description  = data == null ? null : data.getDescription();
        nodeId = data == null ? null : data.getNodeId();
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
       ArrDataUriRef data = new ArrDataUriRef();
       // pokud by nebylo schéma nastavené, je potřeba ho dopočítat
       if (schema == null) {
           schema = ArrDataUriRef.createSchema(value);
       }
       data.setSchema(schema);
       data.setUriRefValue(value);
       data.setDescription(description);

       if(node != null) {
           if(!Objects.equals(node.getId(), nodeId)) {
               throw new BusinessException("Inconsistent data, node is not null", BaseCode.PROPERTY_IS_INVALID)
                       .set("nodeId", nodeId).set("node.id", node.getId());
           }
       }
       ArrNode node = null;
       if(this.nodeId != null) {
           node = em.getReference(ArrNode.class, nodeId);
       }
       data.setArrNode(node);
       data.setDataType(DataType.URI_REF.getEntity());
       return data;
    }

    public String getValue() {
        return value;
    }
}
