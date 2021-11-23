package cz.tacr.elza.domain;

public interface ItemGroovy {

    Integer getItemId();

    Integer getItemTypeId();

    Integer getItemSpecId();

    ArrData getData();

    PartGroovy getPart();

    RulItemSpec getItemSpec();

}
