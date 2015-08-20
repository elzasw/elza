package cz.tacr.elza.api;

import java.io.Serializable;



/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface RulDescItemConstraint<RIT extends RulDescItemType, RIS extends RulDescItemSpec, AV extends ArrFaVersion>
        extends Serializable {


    Integer getDescItemConstraintId();


    void setDescItemConstraintId(final Integer descItemConstraintId);


    RIT getDescItemType();


    void setDescItemType(final RIT descItemType);


    RIS getDescItemSpec();


    void setDescItemSpec(final RIS descItemSpec);


    AV getVersion();


    void setVersion(final AV version);


    Boolean getRepeatable();


    void setRepeatable(final Boolean repeatable);


    String getRegexp();


    void setRegexp(final String regexp);


    Integer getTextLenghtLimit();


    void setTextLenghtLimit(final Integer textLenghtLimit);

}
