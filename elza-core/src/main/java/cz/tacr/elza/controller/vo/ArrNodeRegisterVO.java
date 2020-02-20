package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;


/**
 * @author Martin Šlapa
 * @since 29.1.2016
 */
public class ArrNodeRegisterVO {


    private Integer id;

    private ArrNodeVO node;

    private Integer nodeId;

    private ApAccessPointVO record;

    private Integer value;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public ArrNodeVO getNode() {
        return node;
    }

    public void setNode(final ArrNodeVO node) {
        this.node = node;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public ApAccessPointVO getRecord() {
        return record;
    }

    public void setRecord(final ApAccessPointVO record) {
        this.record = record;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }
}