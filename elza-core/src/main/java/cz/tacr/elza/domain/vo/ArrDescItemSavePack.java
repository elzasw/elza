package cz.tacr.elza.domain.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.ArrNode;


/**
 * @author Martin Šlapa
 * @since 28.8.2015
 */
public class ArrDescItemSavePack implements cz.tacr.elza.api.vo.ArrDescItemSavePack<ArrDescItemExt, ArrNode> {

    private List<ArrDescItemExt> descItems;

    private List<ArrDescItemExt> deleteDescItems;

    private Integer faVersionId;

    private Boolean createNewVersion;

    private ArrNode node;

    @Override
    public List<ArrDescItemExt> getDescItems() {
        return descItems;
    }

    @Override
    public void setDescItems(List<ArrDescItemExt> descItems) {
        this.descItems = descItems;
    }

    @Override
    public List<ArrDescItemExt> getDeleteDescItems() {
        return deleteDescItems;
    }

    @Override
    public void setDeleteDescItems(List<ArrDescItemExt> descItems) {
        this.deleteDescItems = descItems;
    }

    @Override
    public Integer getFaVersionId() {
        return faVersionId;
    }

    @Override
    public void setFaVersionId(Integer faVersionId) {
        this.faVersionId = faVersionId;
    }

    @Override
    public Boolean getCreateNewVersion() {
        return createNewVersion;
    }

    @Override
    public void setCreateNewVersion(Boolean createNewVersion) {
        this.createNewVersion = createNewVersion;
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @Override
    public void setNode(ArrNode node) {
        this.node = node;
    }

}
