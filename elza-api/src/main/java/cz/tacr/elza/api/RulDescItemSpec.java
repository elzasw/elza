package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface RulDescItemSpec<RIT extends RulDescItemType> extends Serializable {


    public Integer getDescItemSpecId();


    public void setDescItemSpecId(final Integer descItemSpecId);


    public RIT getDescItemType();


    public void setDescItemType(final RIT descItemType);


    public String getCode();


    public void setCode(final String code);


    public String getName();


    public void setName(final String name);


    public String getShortcut();


    public void setShortcut(final String shortcut);


    public String getDescription();


    public void setDescription(final String description);


    public Integer getViewOrder();


    public void setViewOrder(final Integer viewOrder);


}
