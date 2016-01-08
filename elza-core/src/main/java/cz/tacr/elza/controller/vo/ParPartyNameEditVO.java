package cz.tacr.elza.controller.vo;

/**
 * Jméno abstraktní osoby pro insert či update.
 */
public class ParPartyNameEditVO {

    /**
     * Vlastní ID.
     */
    private Integer partyNameId;

    /**
     * Preferované jméno.
     */
    private boolean preferredName = false;

    /**
     * Platnost jména od.
     */
    private ParUnitdateEditVO validFrom;
    /**
     * Platnost jména do.
     */
    private ParUnitdateEditVO validTo;

    /**
     * Typ jména.
     */
    private Integer nameFormTypeId;

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


    public Integer getPartyNameId() {
        return partyNameId;
    }

    public void setPartyNameId(final Integer partyNameId) {
        this.partyNameId = partyNameId;
    }

    public ParUnitdateEditVO getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(final ParUnitdateEditVO validFrom) {
        this.validFrom = validFrom;
    }

    public ParUnitdateEditVO getValidTo() {
        return validTo;
    }

    public void setValidTo(final ParUnitdateEditVO validTo) {
        this.validTo = validTo;
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

    public Integer getNameFormTypeId() {
        return nameFormTypeId;
    }

    public void setNameFormTypeId(final Integer nameFormTypeId) {
        this.nameFormTypeId = nameFormTypeId;
    }

    public boolean isPreferredName() {
        return preferredName;
    }

    public void setPreferredName(final boolean preferredName) {
        this.preferredName = preferredName;
    }
}
