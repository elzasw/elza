package cz.tacr.elza.api;



/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public interface ArrData<RD extends RulDataType, AI extends ArrDescItem> {

    public Integer getDataId();


    public void setDataId(final Integer dataId);


    public RD getDataType();


    public void setDataType(final RD dataType);


    public AI getDescItem();


    public void setDescItem(final AI descItem);
}
