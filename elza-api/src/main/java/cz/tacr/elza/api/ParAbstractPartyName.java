package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Jméno abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParAbstractPartyName<AB extends ParAbstractParty> extends Versionable, Serializable {

    Integer getAbstractPartyNameId();

    void setAbstractPartyNameId(Integer abstractPartyNameId);

    AB getAbstractParty();

    void setAbstractParty(AB abstractParty);

    String getMainPart();

    void setMainPart(String mainPart);

    String getOtherPart();

    void setOtherPart(String otherPart);

    String getAnotation();

    void setAnotation(String anotation);

    String getDegreeBefore();

    void setDegreeBefore(String degreeBefore);

    String getDegreeAfter();

    void setDegreeAfter(String degreeAfter);

    LocalDateTime getValidFrom();

    void setValidFrom(LocalDateTime validFrom);

    LocalDateTime getValidTo();

    void setValidTo(LocalDateTime validTo);
}
