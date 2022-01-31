package cz.tacr.elza.domain;

public interface AccessPointItem {

    Integer getItemId();

    Integer getItemTypeId();

    Integer getItemSpecId();

    ArrData getData();

    AccessPointPart getPart();

    RulItemSpec getItemSpec();

    Integer getObjectId();

    Integer getPosition();

}
