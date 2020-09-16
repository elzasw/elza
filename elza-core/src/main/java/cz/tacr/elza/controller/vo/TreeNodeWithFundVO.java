package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.service.LevelTreeCacheService;

/**
 * Data o JP s archivním souborem.
 */
public class TreeNodeWithFundVO extends TreeNodeVO {

    private String nodeUuid;
    private String fundName;
    private Integer fundId;
    private Integer fundVersionId;

    protected TreeNodeWithFundVO(final Integer id, final Integer depth, final String name,
                                 final String icon, final String[] referenceMark, final Integer version,
                                 final String nodeUuid, final String fundName, final Integer fundId, final Integer fundVersionId) {
        super(id, depth, name, false, null, version);
        setIcon(icon);
        setReferenceMark(referenceMark);
        this.nodeUuid = nodeUuid;
        this.fundName = fundName;
        this.fundId = fundId;
        this.fundVersionId = fundVersionId;
    }

    public static TreeNodeWithFundVO newInstance(final LevelTreeCacheService.Node node, final ArrFundVersion fundVersion) {
        ArrFund fund = fundVersion.getFund();
        return new TreeNodeWithFundVO(node.getId(),
                node.getDepth(),
                node.getName(),
                node.getIcon(),
                node.getReferenceMark(),
                node.getVersion(),
                node.getUuid(),
                fund.getName(),
                fund.getFundId(),
                fundVersion.getFundVersionId());
    }

    public String getNodeUuid() {
        return nodeUuid;
    }

    public void setNodeUuid(final String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }

    public String getFundName() {
        return fundName;
    }

    public void setFundName(final String fundName) {
        this.fundName = fundName;
    }

    public Integer getFundId() {
        return fundId;
    }

    public void setFundId(final Integer fundId) {
        this.fundId = fundId;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public void setFundVersionId(final Integer fundVersionId) {
        this.fundVersionId = fundVersionId;
    }
}
