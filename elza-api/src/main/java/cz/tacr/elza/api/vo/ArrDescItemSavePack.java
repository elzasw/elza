package cz.tacr.elza.api.vo;

import java.io.Serializable;
import java.util.List;

import cz.tacr.elza.api.ArrDescItem;
import cz.tacr.elza.api.ArrDescItemExt;
import cz.tacr.elza.api.ArrNode;


/**
 * Zapouzdření hodnot pro ulžení {@link ArrDescItem}.
 * @author Martin Šlapa
 * @since 28.8.2015
 *
 * @param <DIE> {@link ArrDescItemExt}
 * @param <N> {@link ArrNode}
 */
public interface ArrDescItemSavePack<DIE extends ArrDescItemExt, N extends ArrNode> extends Serializable {

    List<DIE> getDescItems();


    void setDescItems(List<DIE> descItems);


    List<DIE> getDeleteDescItems();


    void setDeleteDescItems(List<DIE> descItems);


    Integer getFaVersionId();


    void setFaVersionId(Integer faVersionId);


    Boolean getCreateNewVersion();


    void setCreateNewVersion(Boolean createNewVersion);


    N getNode();


    void setNode(N node);

}
