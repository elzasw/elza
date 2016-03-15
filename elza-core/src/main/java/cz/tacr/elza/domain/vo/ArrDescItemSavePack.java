package cz.tacr.elza.domain.vo;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;


/**
 * Zapouzdření hodnot pro ulžení {@link ArrDescItem}.
 * @author Martin Šlapa
 * @since 28.8.2015
 */
public class ArrDescItemSavePack implements cz.tacr.elza.api.vo.ArrDescItemSavePack<ArrDescItem, ArrNode> {

    private List<ArrDescItem> descItems;

    private List<ArrDescItem> deleteDescItems;

    private Integer fundVersionId;

    private Boolean createNewVersion;

    private ArrNode node;

    @Override
    public List<ArrDescItem> getDescItems() {
        return descItems;
    }

    @Override
    public void setDescItems(List<ArrDescItem> descItems) {
        this.descItems = descItems;
    }

    @Override
    public List<ArrDescItem> getDeleteDescItems() {
        return deleteDescItems;
    }

    @Override
    public void setDeleteDescItems(List<ArrDescItem> descItems) {
        this.deleteDescItems = descItems;
    }

    @Override
    public Integer getFundVersionId() {
        return fundVersionId;
    }

    @Override
    public void setFundVersionId(Integer fundVersionId) {
        this.fundVersionId = fundVersionId;
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
