package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Jméno abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyName<AB extends ParParty> extends Versionable, Serializable {

    Integer getAbstractPartyNameId();

    void setAbstractPartyNameId(Integer abstractPartyNameId);

    AB getAbstractParty();

    void setAbstractParty(AB abstractParty);

    /**
     * @return Hlavní část jména.
     */
    String getMainPart();

    /**
     * @param mainPart Hlavní část jména.
     */
    void setMainPart(String mainPart);

    /**
     * @return Vedlejší část jména.
     */
    String getOtherPart();

    /**
     * @param otherPart Vedlejší část jména.
     */
    void setOtherPart(String otherPart);

    /**
     * @return Poznámka - Využije se v případě nutnosti doplnit informaci uvedenou v prvcích.
     */
    String getAnotation();

    /**
     * @param anotation Poznámka - Využije se v případě nutnosti doplnit informaci uvedenou v prvcích.
     */
    void setAnotation(String anotation);

    /**
     * @return titul před jménem.
     */
    String getDegreeBefore();

    /**
     * @param degreeBefore titul před jménem.
     */
    void setDegreeBefore(String degreeBefore);

    /**
     * @return titul za jménem.
     */
    String getDegreeAfter();

    /**
     * @param degreeAfter titul za jménem.
     */
    void setDegreeAfter(String degreeAfter);

    /**
     * @return platnost jména od.
     */
    LocalDateTime getValidFrom();

    /**
     * @param validFrom platnost jména od.
     */
    void setValidFrom(LocalDateTime validFrom);

    /**
     * @return platnost jména do.
     */
    LocalDateTime getValidTo();

    /**
     * @param validTo platnost jména do.
     */
    void setValidTo(LocalDateTime validTo);
}
