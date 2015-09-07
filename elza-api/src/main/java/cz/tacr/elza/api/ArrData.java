package cz.tacr.elza.api;

import java.io.Serializable;



/**
 * tabulka pro evidenci hodnot atributů archivního popisu.
 * 
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public interface ArrData<RD extends RulDataType, AI extends ArrDescItem> extends Versionable, Serializable {

    public Integer getDataId();


    public void setDataId(final Integer dataId);


    public RD getDataType();


    public void setDataType(final RD dataType);


    public AI getDescItem();


    public void setDescItem(final AI descItem);
}
