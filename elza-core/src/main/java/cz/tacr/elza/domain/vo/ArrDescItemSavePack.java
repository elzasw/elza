package cz.tacr.elza.domain.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrDescItemExt;


/**
 * @author Martin Šlapa
 * @since 28.8.2015
 */
public class ArrDescItemSavePack implements cz.tacr.elza.api.vo.ArrDescItemSavePack<ArrDescItemExt> {

    private List<ArrDescItemExt> descItems;

    private List<ArrDescItemExt> deleteDescItems;

    private Integer faVersionId;

    private Boolean createNewVersion;

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

}
