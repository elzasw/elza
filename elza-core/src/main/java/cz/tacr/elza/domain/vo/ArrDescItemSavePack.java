package cz.tacr.elza.domain.vo;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;


/**
 * Zapouzdření hodnot pro ulžení {@link ArrDescItem}.
 * @author Martin Šlapa
 * @since 28.8.2015
 */
public class ArrDescItemSavePack {

    private List<ArrDescItem> descItems;

    private List<ArrDescItem> deleteDescItems;

    private Integer fundVersionId;

    private Boolean createNewVersion;

    private ArrNode node;

    public List<ArrDescItem> getDescItems() {
        return descItems;
    }

    public void setDescItems(final List<ArrDescItem> descItems) {
        this.descItems = descItems;
    }

    public List<ArrDescItem> getDeleteDescItems() {
        return deleteDescItems;
    }

    public void setDeleteDescItems(final List<ArrDescItem> descItems) {
        this.deleteDescItems = descItems;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public void setFundVersionId(final Integer fundVersionId) {
        this.fundVersionId = fundVersionId;
    }

    public Boolean getCreateNewVersion() {
        return createNewVersion;
    }

    public void setCreateNewVersion(final Boolean createNewVersion) {
        this.createNewVersion = createNewVersion;
    }

    public ArrNode getNode() {
        return node;
    }

    public void setNode(final ArrNode node) {
        this.node = node;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
