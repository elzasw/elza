package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.print.UnitDate;

/**
 * Party name
 *
 * This object is used in output generators
 */
public class PartyName {

    private final String mainPart;

    private final String otherPart;

    private final String note;

    private final String degreeBefore;

    private final String degreeAfter;

    // can be proxy, initialized only when needed
    private final ParUnitdate srcValidFrom;

    // can be proxy, initialized only when needed
    private final ParUnitdate srcValidTo;

    private final String formTypeName;

    private UnitDate validFrom;

    private UnitDate validTo;

    private PartyName(ParPartyName parPartyName, String formTypeName) {
        this.mainPart = parPartyName.getMainPart();
        this.otherPart = parPartyName.getOtherPart();
        this.note = parPartyName.getNote();
        this.degreeBefore = parPartyName.getDegreeBefore();
        this.degreeAfter = parPartyName.getDegreeAfter();
        this.srcValidFrom = parPartyName.getValidFrom();
        this.srcValidTo = parPartyName.getValidTo();
        this.formTypeName = formTypeName;
    }

    public String getDegreeAfter() {
        return degreeAfter;
    }

    public String getDegreeBefore() {
        return degreeBefore;
    }

    public String getMainPart() {
        return mainPart;
    }

    public String getNote() {
        return note;
    }

    public String getOtherPart() {
        return otherPart;
    }

    public UnitDate getValidFrom() {
        if (validFrom == null && srcValidFrom != null) {
            validFrom = new UnitDate(srcValidFrom); // lazy initialization
        }
        return validFrom;
    }

    public UnitDate getValidTo() {
        if (validTo == null && srcValidTo != null) {
            validTo = new UnitDate(srcValidTo); // lazy initialization
        }
        return validTo;
    }

    public String getFormTypeName() {
        return formTypeName;
    }

    /**
     * Return formatted name with all details
     *
     * @return obsah fieldů "mainPart otherPart degreeBefore degreeAfter" oddělený mezerou
     */
    public String formatWithAllDetails() {
        List<String> resultList = new ArrayList<>();
        if (StringUtils.isNotBlank(mainPart)) {
            resultList.add(mainPart);
        }
        if (StringUtils.isNotBlank(otherPart)) {
            resultList.add(otherPart);
        }
        if (StringUtils.isNotBlank(degreeBefore)) {
            resultList.add(degreeBefore);
        }
        if (StringUtils.isNotBlank(degreeAfter)) {
            resultList.add(degreeAfter);
        }
        // TODO: add party name details
        return StringUtils.join(resultList, " ").trim();
    }

    /**
     * Format time/date when date was used.
     *
     * @return
     */
    public String formatValidFromTo() {
        String formattedFrom = null, formattedTo = null;
        if (getValidFrom() != null) {
            formattedFrom = getValidFrom().getValueText();
        }
        if (getValidTo() != null) {
            formattedTo = getValidTo().getValueText();
        }
        boolean blankFrom = StringUtils.isBlank(formattedFrom), blankTo = StringUtils.isBlank(formattedTo);
        if (blankFrom && blankTo) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (!blankFrom) {
            sb.append(formattedFrom);
        }
        sb.append(" - ");
        if (!blankTo) {
            sb.append(formattedTo);
        }
        return sb.toString();
    }

    /**
     * Return new instance of PartyName. Valid From/To unit dates are required (fetched from
     * database if not initialized).
     */
    public static PartyName newInstance(ParPartyName parPartyName, StaticDataProvider staticData) {
        // prepare form type name
        Integer nameFormTypeId = parPartyName.getNameFormTypeId();
        String formTypeName = null;
        if (nameFormTypeId != null) {
            ParPartyNameFormType formType = staticData.getPartyNameFormTypeById(nameFormTypeId);
            formTypeName = formType.getName();
        }
        return new PartyName(parPartyName, formTypeName);
    }
}
