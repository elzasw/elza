package cz.tacr.elza.api;

import java.io.Serializable;



/**
 * tabulka pro evidenci hodnot atributů archivního popisu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public interface ArrData<RD extends RulDataType, AI extends ArrDescItem> extends Versionable, Serializable {

    Integer getDataId();


    void setDataId(final Integer dataId);


    RD getDataType();


    void setDataType(final RD dataType);


    AI getDescItem();


    void setDescItem(final AI descItem);
}
