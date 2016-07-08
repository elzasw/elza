package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Jméno abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyName<PP extends ParParty, PU extends ParUnitdate, PNFT extends ParPartyNameFormType> extends Serializable {

    /**
     * Vlastní ID.
     * @return id
     */
    Integer getPartyNameId();

    /**
     * Vlastní ID.
     * @param partyNameId id
     */
    void setPartyNameId(Integer partyNameId);

    /**
     * Vazba na osobu.
     * @return osoba
     */
    PP getParty();

    /**
     * Vazba na osobu.
     * @param party osoba
     */
    void setParty(PP party);

    /**
     * Hlavní část jména.
     * @return hlavní část jména
     */
    String getMainPart();

    /**
     * Hlavní část jména.
     * @param mainPart hlavní část jména
     */
    void setMainPart(String mainPart);

    /**
     * Vedlejší část jména.
     * @return vedlejší část jména
     */
    String getOtherPart();

    /**
     * Vedlejší část jména.
     * @param otherPart vedlejší část jména
     */
    void setOtherPart(String otherPart);

    /**
     * Poznámka - využije se v případě nutnosti doplnit informaci uvedenou v prvcích.
     * @return poznámka - využije se v případě nutnosti doplnit informaci uvedenou v prvcích
     */
    String getNote();

    /**
     * Poznámka - využije se v případě nutnosti doplnit informaci uvedenou v prvcích.
     * @param note poznámka - využije se v případě nutnosti doplnit informaci uvedenou v prvcích
     */
    void setNote(String note);

    /**
     * Titul před jménem.
     * @return titul před jménem
     */
    String getDegreeBefore();

    /**
     * Titul před jménem.
     * @param degreeBefore titul před jménem
     */
    void setDegreeBefore(String degreeBefore);

    /**
     * Titul za jménem.
     * @return titul za jménem
     */
    String getDegreeAfter();

    /**
     * Titul za jménem.
     * @param degreeAfter titul za jménem
     */
    void setDegreeAfter(String degreeAfter);

    /**
     * Platnost jména od.
     * @return platnost jména od
     */
    PU getValidFrom();

    /**
     * Platnost jména od.
     * @param validFrom platnost jména od
     */
    void setValidFrom(PU validFrom);

    /**
     * Platnost jména do.
     * @return platnost jména do
     */
    PU getValidTo();

    /**
     * Platnost jména do.
     * @param validTo platnost jména do
     */
    void setValidTo(PU validTo);

    PNFT getNameFormType();

    void setNameFormType(PNFT nameFormType);

}
