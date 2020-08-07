package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.print.UnitDate;
public class PartyName {

    private String mainPart;

    private String otherPart;

    private String note;

    private String degreeBefore;

    private String degreeAfter;

    private String formTypeName;

    private UnitDate validFrom;

    private UnitDate validTo;

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
        return validFrom;
    }

    public UnitDate getValidTo() {
        return validTo;
    }

    public String getFormTypeName() {
        return formTypeName;
    }

    public List<String> formatWithAllDetailsAsList() {
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
        return resultList;
    }

    /**
     * Return formatted name with all details
     *
     * @return obsah field� "mainPart otherPart degreeBefore degreeAfter" odd�len�
     *         mezerou
     */
    public String formatWithAllDetails() {
        List<String> resultList = formatWithAllDetailsAsList();
        // TODO: add party name details
        return String.join(" ", resultList);
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
}
