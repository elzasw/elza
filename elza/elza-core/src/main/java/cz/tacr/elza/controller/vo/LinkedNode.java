package cz.tacr.elza.controller.vo;

public class LinkedNode {

    private Integer id;
    private Integer nodeId;
    private String name;

    public LinkedNode(Integer id, Integer nodeId, String name) {
        this.id = id;
        this.nodeId = nodeId;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
