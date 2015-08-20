package cz.tacr.elza.api;

import java.io.Serializable;



/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface RulDescItemType<RT extends RulDataType> extends Serializable {


    public Integer getDescItemTypeId();


    public void setDescItemTypeId(final Integer descItemTypeId);


    public RT getDataType();


    public void setDataType(final RT dataType);


    public Boolean getSys();


    public void setSys(final Boolean sys);


    public String getCode();


    public void setCode(final String code);


    public String getName();


    public void setName(final String name);


    public String getShortcut();


    public void setShortcut(final String shortcut);


    public String getDescription();


    public void setDescription(final String description);


    public Boolean getIsValueUnique();


    public void setIsValueUnique(final Boolean isValueUnique);


    public Boolean getCanBeOrdered();


    public void setCanBeOrdered(final Boolean canBeOrdered);


    public Boolean getUseSpecification();


    public void setUseSpecification(final Boolean useSpecification);


    public Integer getViewOrder();


    public void setViewOrder(final Integer viewOrder);

}
