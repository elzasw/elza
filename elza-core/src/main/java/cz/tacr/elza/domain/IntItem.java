package cz.tacr.elza.domain;

public interface IntItem {

    public Integer getItemId();

    public Integer getItemTypeId();

    public Integer getItemSpecId();

    public Integer getPosition();

    public ArrData getData();

    public boolean isUndefined();

}
