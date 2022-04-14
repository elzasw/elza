package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

import javax.persistence.EntityManager;
import java.util.Objects;

public class ApItemUriRefVO extends ApItemVO {

    /**
     * Node
     */
    private String schema;

    private String value;

    private String description;

    private ArrNodeVO node;

    private Integer nodeId;

    public ApItemUriRefVO() {
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public ApItemUriRefVO(final AccessPointItem item) {
        super(item);
        ArrDataUriRef data = (ArrDataUriRef) item.getData();
        if (data != null) {
            value = data.getUriRefValue();
            schema = data.getSchema();
            description = data.getDescription();
            nodeId = data.getNodeId();
        }
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

    @Override
    public boolean equalsValue(AccessPointItem item) {
        String value = null;
        String schema = null;
        String description = null;
        Integer nodeId = null;
        ArrDataUriRef data = (ArrDataUriRef) item.getData();
        if (data != null) {
            value = data.getUriRefValue();
            schema = data.getSchema();
            description = data.getDescription();
            nodeId = data.getNodeId();
        }
        if(!equalsBase(item)) {
        	return false;
        }
        if(!Objects.equals(value, this.value)||
        		!Objects.equals(description, this.description)||
        		!Objects.equals(nodeId, this.nodeId)
        	) {
        	return false;
        }
        // compare schema if not null
        // if schema is null it will be calculated from URL
        if(schema!=null&&this.schema!=null) {
        	if(!Objects.equals(schema, this.schema)) {
        		return false;
        	}
        }
        return true;
    }
}
