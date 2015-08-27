package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface RulFaView<AT extends ArrArrangementType, RS extends RulRuleSet> extends Versionable, Serializable {


    Integer getFaViewId();


    void setFaViewId(final Integer faViewId);


    AT getArrangementType();


    void setArrangementType(final AT arrangementType);


    RS getRuleSet();


    void setRuleSet(final RS ruleSet);


    String getViewSpecification();


    void setViewSpecification(final String viewSpecification);

}
