package cz.tacr.elza.domain;

public interface AccessPointItem extends Item {

    AccessPointPart getPart();

    RulItemSpec getItemSpec();

    Integer getObjectId();
}
