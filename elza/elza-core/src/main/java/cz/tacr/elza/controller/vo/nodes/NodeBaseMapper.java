package cz.tacr.elza.controller.vo.nodes;

import cz.tacr.elza.controller.vo.NodeBase;
import cz.tacr.elza.domain.ArrNode;

public class NodeBaseMapper {

	public static NodeBase valueOf(ArrNode node) {
        return new NodeBase(node.getNodeId(), node.getVersion(), node.getUuid());
    }

    public static ArrNode createEntity(NodeBase nb) {
        ArrNode node = new ArrNode();
        node.setNodeId(nb.getId());
        node.setVersion(nb.getVersion());
        node.setUuid(nb.getUuid());
        return node;
    }

    @Deprecated
    public static ArrNodeVO toArrNodeVO(NodeBase nb) {
    	ArrNodeVO node = new ArrNodeVO();
    	node.setId(nb.getId());
    	node.setUuid(nb.getUuid());
    	node.setVersion(nb.getVersion());
    	return node;
    }

}