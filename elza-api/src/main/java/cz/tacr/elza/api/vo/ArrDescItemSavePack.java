package cz.tacr.elza.api.vo;

import java.io.Serializable;
import java.util.List;

import cz.tacr.elza.api.ArrDescItem;
import cz.tacr.elza.api.ArrNode;


/**
 * Zapouzdření hodnot pro ulžení {@link ArrDescItem}.
 * @author Martin Šlapa
 * @since 28.8.2015
 *
 * @param <DI> {@link ArrDescItem}
 * @param <N> {@link ArrNode}
 */
public interface ArrDescItemSavePack<DI extends ArrDescItem, N extends ArrNode> extends Serializable {

    List<DI> getDescItems();


    void setDescItems(List<DI> descItems);


    List<DI> getDeleteDescItems();


    void setDeleteDescItems(List<DI> descItems);


    Integer getFundVersionId();


    void setFundVersionId(Integer fundVersionId);


    Boolean getCreateNewVersion();


    void setCreateNewVersion(Boolean createNewVersion);


    N getNode();


    void setNode(N node);

}
