package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.print.UnitDateText;

/**
 * Party name
 * 
 * This object is used in output generators
 */
public class PartyName {

    private String mainPart;
    private String otherPart;
    private String note;
    private String degreeBefore;
    private String degreeAfter;
    private UnitDateText validFrom;
    private UnitDateText validTo;
    private String formTypeName;
    
    private PartyName(ParPartyName parPartyName) {
        this.mainPart = parPartyName.getMainPart();
        this.otherPart = parPartyName.getOtherPart();
        this.note = parPartyName.getNote();
        this.degreeBefore = parPartyName.getDegreeBefore();
        this.degreeAfter = parPartyName.getDegreeAfter();
        this.validFrom = UnitDateText.valueOf(parPartyName.getValidFrom());
        this.validTo = UnitDateText.valueOf(parPartyName.getValidTo());
        
        ParPartyNameFormType nameFormType = parPartyName.getNameFormType();
        if(nameFormType!=null) {
        	formTypeName = nameFormType.getName();
        }
    }

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

    public UnitDateText getValidFrom() {
        return validFrom;
    }

    public UnitDateText getValidTo() {
        return validTo;
    }


	public String getFormTypeName() {
		return formTypeName;
	}

	@Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }

    /**
     * Create new instance from DB object
     * @param parPartyName
     * @return
     */
	public static PartyName valueOf(ParPartyName parPartyName) {
		PartyName partyName = new PartyName(parPartyName); 
		return partyName;
	}
}
