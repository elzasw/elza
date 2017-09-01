package cz.tacr.elza.controller.vo.nodes;

import cz.tacr.elza.domain.ArrNode;

/**
 * VO uzlu archivní pomůcky.
 */
public class ArrNodeVO {

    /**
     * identifikátor uzlu
     */
    private Integer id;

    /**
     * verze uzlu
     */
    private Integer version;

    public ArrNodeVO() {
    }

    public ArrNodeVO(final Integer id, final Integer version) {
        this.id = id;
        this.version = version;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public ArrNode createEntity() {
        ArrNode node = new ArrNode();
        node.setNodeId(id);
        node.setVersion(version);
        return node;
    }
}
