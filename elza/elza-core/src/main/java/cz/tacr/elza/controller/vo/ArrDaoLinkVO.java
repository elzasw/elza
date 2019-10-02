package cz.tacr.elza.controller.vo;

/**
 * VO pro klienta.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 6.1.17
 */
public class ArrDaoLinkVO {
    private Integer id;
    private TreeNodeVO treeNodeClient;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TreeNodeVO getTreeNodeClient() {
        return treeNodeClient;
    }

    public void setTreeNodeClient(TreeNodeVO treeNodeClient) {
        this.treeNodeClient = treeNodeClient;
    }
}
