package cz.tacr.elza.print.party;

import cz.tacr.elza.print.UnitDateText;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class PartyName {
    private String mainPart;
    private String otherPart;
    private String note;
    private String degreeBefore;
    private String degreeAfter;
    private UnitDateText validFrom;
    private UnitDateText validTo;

    /**
     * @return obsah fieldů "mainPart otherPart degreeBefore degreeAfter" oddělený mezerou
     */
    public String serialize() {
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
        return StringUtils.join(resultList, " ").trim();
    }

    public String getDegreeAfter() {
        return degreeAfter;
    }

    public void setDegreeAfter(String degreeAfter) {
        this.degreeAfter = degreeAfter;
    }

    public String getDegreeBefore() {
        return degreeBefore;
    }

    public void setDegreeBefore(String degreeBefore) {
        this.degreeBefore = degreeBefore;
    }

    public String getMainPart() {
        return mainPart;
    }

    public void setMainPart(String mainPart) {
        this.mainPart = mainPart;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getOtherPart() {
        return otherPart;
    }

    public void setOtherPart(String otherPart) {
        this.otherPart = otherPart;
    }

    public UnitDateText getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(UnitDateText validFrom) {
        this.validFrom = validFrom;
    }

    public UnitDateText getValidTo() {
        return validTo;
    }

    public void setValidTo(UnitDateText validTo) {
        this.validTo = validTo;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(o, this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
