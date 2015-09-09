package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * Nastavení zobrazení archivního popisu pomůcky, respektive jaké atributy budou vidět v
 * hierarchickém pohledu (stromu) na pomůcku.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 *
 * @param <AT> {@link RulArrangementType}
 * @param <RS> {@link RulRuleSet}
 */
public interface RulFaView<AT extends RulArrangementType, RS extends RulRuleSet>
        extends
            Versionable,
            Serializable {


    Integer getFaViewId();


    void setFaViewId(final Integer faViewId);


    AT getArrangementType();


    void setArrangementType(final AT arrangementType);


    RS getRuleSet();


    void setRuleSet(final RS ruleSet);


    /**
     * @return definice typů atributů, které mají být zobrazeny v přehledu/stromu pomůcky)
     */
    String getViewSpecification();


    /**
     * @param viewSpecification definice typů atributů, které mají být zobrazeny v přehledu/stromu pomůcky).
     */
    void setViewSpecification(final String viewSpecification);

}
