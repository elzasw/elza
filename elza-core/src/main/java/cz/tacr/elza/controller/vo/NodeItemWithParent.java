package cz.tacr.elza.controller.vo;

/**
 * Objekt reprezentující název JP v seznamu s rodičem.
 *
 * @author Martin Šlapa
 * @since 15.04.2016
 */
public class NodeItemWithParent {

    /**
     * identifikátor nodu
     */
    private Integer id;

    /**
     * zobrazovaný název
     */
    private String name;

    /**
     * rodič uzlu
     */
    private TreeNodeClient parentNode;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public TreeNodeClient getParentNode() {
        return parentNode;
    }

    public void setParentNode(final TreeNodeClient parentNode) {
        this.parentNode = parentNode;
    }
}
