package cz.tacr.elza.controller.vo;

import java.util.List;


/**
 * Jméno abstraktní osoby.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
public class ParPartyNameVO {

    /**
     * Vlastní ID.
     */
    private Integer id;

    /**
     * Platnost jména od.
     */
    private ParUnitdateVO validFrom;
    /**
     * Platnost jména do.
     */
    private ParUnitdateVO validTo;

    /**
     * Typ jména.
     */
    private ParPartyNameFormTypeVO nameFormType;

    /**
     * Id osoby.
     */
    private Integer partyId;

    /**
     * Seznam doplňků jména.
     */
    private List<ParPartyNameComplementVO> partyNameComplements;
    /**
     * Hlavní část jména.
     */
    private String mainPart;
    /**
     * Vedlejší část jména.
     */
    private String otherPart;
    /**
     * Poznámka - využije se v případě nutnosti doplnit informaci uvedenou v prvcích.
     */
    private String note;

    /**
     * Titul před jménem.
     */
    private String degreeBefore;

    /**
     * Titul za jménem.
     */
    private String degreeAfter;

    /**
     * Poskládané jméno pro zobrazení.
     */
    private String displayName;

    private boolean prefferedName;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public ParUnitdateVO getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(final ParUnitdateVO validFrom) {
        this.validFrom = validFrom;
    }

    public ParUnitdateVO getValidTo() {
        return validTo;
    }

    public void setValidTo(final ParUnitdateVO validTo) {
        this.validTo = validTo;
    }

    public ParPartyNameFormTypeVO getNameFormType() {
        return nameFormType;
    }

    public void setNameFormType(final ParPartyNameFormTypeVO nameFormType) {
        this.nameFormType = nameFormType;
    }

    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    public List<ParPartyNameComplementVO> getPartyNameComplements() {
        return partyNameComplements;
    }

    public void setPartyNameComplements(final List<ParPartyNameComplementVO> partyNameComplements) {
        this.partyNameComplements = partyNameComplements;
    }

    public String getMainPart() {
        return mainPart;
    }

    public void setMainPart(final String mainPart) {
        this.mainPart = mainPart;
    }

    public String getOtherPart() {
        return otherPart;
    }

    public void setOtherPart(final String otherPart) {
        this.otherPart = otherPart;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public String getDegreeBefore() {
        return degreeBefore;
    }

    public void setDegreeBefore(final String degreeBefore) {
        this.degreeBefore = degreeBefore;
    }

    public String getDegreeAfter() {
        return degreeAfter;
    }

    public void setDegreeAfter(final String degreeAfter) {
        this.degreeAfter = degreeAfter;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public boolean isPrefferedName() {
        return prefferedName;
    }

    public void setPrefferedName(final boolean prefferedName) {
        this.prefferedName = prefferedName;
    }
}
