package cz.tacr.elza.api;

import java.io.Serializable;



/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface ArrDescItem<FC extends ArrFaChange, RT extends RulDescItemType,
    RS extends RulDescItemSpec, N extends ArrNode> extends Versionable, Serializable {


    public Integer getDescItemId();


    public void setDescItemId(final Integer descItemId);


    FC getCreateChange();


    void setCreateChange(final FC createChange);


    FC getDeleteChange();


    void setDeleteChange(final FC deleteChange);


    Integer getDescItemObjectId();


    void setDescItemObjectId(final Integer descItemObjectId);


    RT getDescItemType();


    void setDescItemType(final RT descItemType);


    RS getDescItemSpec();


    void setDescItemSpec(final RS descItemSpec);


    N getNode();


    void setNode(final N node);


    Integer getPosition();


    void setPosition(final Integer position);

}
