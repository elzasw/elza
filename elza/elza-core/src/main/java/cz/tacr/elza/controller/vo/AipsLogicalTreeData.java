package cz.tacr.elza.controller.vo;

import java.util.List;

public class AipsLogicalTreeData {
    private TreeData tree;
    private List<TreeNodeVO> nodesWithoutStructure;

    public TreeData getTree() {
        return tree;
    }

    public void setTree(TreeData treeData) {
        this.tree = treeData;
    }

    public List<TreeNodeVO> getNodesWithoutStructure() {
        return nodesWithoutStructure;
    }

    public void setNodesWithoutStructure(List<TreeNodeVO> nodesWithoutStructure) {
        this.nodesWithoutStructure = nodesWithoutStructure;
    }
}
