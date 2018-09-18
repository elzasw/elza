package cz.tacr.elza.controller.vo.nodes;

/**
 * Parametry pro získání informací JP a jejím "okolí".
 *
 * 1) request ze stromu:
 * 2) request při změně stránkování
 * 3) request při přejití na index v úrovni
 * 4) request na filtrování v úrovni
 */
public class NodeDataParam {

    /**
     * Verze AS.
     */
    private Integer fundVersionId; // vždy

    /**
     * Identifikátor JP.
     */
    private Integer nodeId;

    /**
     * Index JP v úrovni (použité v kombinaci s {@link NodeDataParam#parentNodeId}).
     */
    private Integer nodeIndex;

    /**
     * Načítat i data formuláře JP?
     */
    private Boolean formData;

    /**
     * Načítat (tzn. není null) sourozence JP?
     */
    private Integer siblingsFrom; // null - nepožadujeme

    /**
     * Maximální počet vrácených sourozence.
     */
    private Integer siblingsMaxCount; // maximální počet vrácených

    /**
     * Fulltextové filtrování v sourozencích.
     */
    private String siblingsFilter;

    /**
     * Načítat JP předky ke kořenu?
     */
    private Boolean parents;

    /**
     * Načítat přímé potomky?
     */
    private Boolean children;

    /**
     * Identifikátor rodiče (použité v kombinaci s {@link NodeDataParam#nodeIndex}).
     */
    private Integer parentNodeId;

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public void setFundVersionId(final Integer fundVersionId) {
        this.fundVersionId = fundVersionId;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(final Integer nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public Boolean getFormData() {
        return formData;
    }

    public void setFormData(final Boolean formData) {
        this.formData = formData;
    }

    public Integer getSiblingsFrom() {
        return siblingsFrom;
    }

    public void setSiblingsFrom(final Integer siblingsFrom) {
        this.siblingsFrom = siblingsFrom;
    }

    public Integer getSiblingsMaxCount() {
        return siblingsMaxCount;
    }

    public void setSiblingsMaxCount(final Integer siblingsMaxCount) {
        this.siblingsMaxCount = siblingsMaxCount;
    }

    public String getSiblingsFilter() {
        return siblingsFilter;
    }

    public void setSiblingsFilter(final String siblingsFilter) {
        this.siblingsFilter = siblingsFilter;
    }

    public Boolean getParents() {
        return parents;
    }

    public void setParents(final Boolean parents) {
        this.parents = parents;
    }

    public Boolean getChildren() {
        return children;
    }

    public void setChildren(final Boolean children) {
        this.children = children;
    }

    public Integer getParentNodeId() {
        return parentNodeId;
    }

    public void setParentNodeId(final Integer parentNodeId) {
        this.parentNodeId = parentNodeId;
    }
}
