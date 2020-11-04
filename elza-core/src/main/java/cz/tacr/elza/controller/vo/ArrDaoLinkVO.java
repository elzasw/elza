package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrDaoLink;

/**
 * VO pro klienta.
 *
 */
public class ArrDaoLinkVO {
    private Integer id;
    private TreeNodeVO treeNodeClient;
    private String scenario;

    public ArrDaoLinkVO() {
    }

    public ArrDaoLinkVO(final Integer id,
                        final TreeNodeVO treeNodeClient) {
        this.id = id;
        this.treeNodeClient = treeNodeClient;
    }

    public ArrDaoLinkVO(final Integer id,
                        final TreeNodeVO treeNodeClient, final String scenario) {
        this.id = id;
        this.treeNodeClient = treeNodeClient;
        this.scenario = scenario;
    }

    public ArrDaoLinkVO(final ArrDaoLink daoLink) {
        id = daoLink.getDaoLinkId();
    }

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

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public static ArrDaoLinkVO newInstance(ArrDaoLink daoLink) {
        ArrDaoLinkVO daoLinkVO = new ArrDaoLinkVO(daoLink);
        return daoLinkVO;
    }
}
