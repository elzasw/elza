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

	private int partyNameId;
    private String mainPart;
    private String otherPart;
    private String note;
    private String degreeBefore;
    private String degreeAfter;
    private UnitDateText validFrom;
    private UnitDateText validTo;
    private String formTypeName;
    
    private PartyName(ParPartyName parPartyName) {
    	this.partyNameId = parPartyName.getPartyNameId();
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
     * Return formatted name with all details
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
    
    /**
     * Format time/date when date was used.
     * @return
     */
    public String formatValidFromTo() {
    	String formattedFrom = null, formattedTo = null;    	
    	if(validFrom!=null) {
    		formattedFrom = validFrom.getValueText();
    	}
    	if(validTo!=null) {
    		formattedTo = validTo.getValueText();
    	}
    	boolean blankFrom = StringUtils.isBlank(formattedFrom), blankTo = StringUtils.isBlank(formattedTo);
    	if(blankFrom&&blankTo) {
    		return null;
    	}
    	StringBuilder sb = new StringBuilder();
    	if(!blankFrom) {
    		sb.append(formattedFrom);
    	}
    	sb.append(" - ");
    	if(!blankTo) {
    		sb.append(formattedTo);
    	}
    	return sb.toString();
    }


	public String getFormTypeName() {
		return formTypeName;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj.getClass()!=PartyName.class) {
			return false;
		}
		PartyName other = (PartyName)(obj);
		return this.partyNameId == other.partyNameId;
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
