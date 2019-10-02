package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkAction;

public class GenerateUnitIdConfig extends BaseActionConfig {

	/**
	 * Item type for storing UnitId
	 */
	String itemType;

	String previousIdCode;

	String previousIdSpecCode;

	String levelTypeCode;

    String extraDelimiterAfter;

	public String getItemType() {
		return itemType;
	}

	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	public String getPreviousIdCode() {
		return previousIdCode;
	}

	public void setPreviousIdCode(String previousIdCode) {
		this.previousIdCode = previousIdCode;
	}

	public String getPreviousIdSpecCode() {
		return previousIdSpecCode;
	}

	public void setPreviousIdSpecCode(String previousIdSpecCode) {
		this.previousIdSpecCode = previousIdSpecCode;
	}

	public String getLevelTypeCode() {
		return levelTypeCode;
	}

	public void setLevelTypeCode(String levelTypeCode) {
		this.levelTypeCode = levelTypeCode;
	}

    public String getExtraDelimiterAfter() {
        return extraDelimiterAfter;
	}

    public void setExtraDelimiterAfter(String extraDelimiterAfter) {
        this.extraDelimiterAfter = extraDelimiterAfter;
	}

	@Override
	public BulkAction createBulkAction() {
		return new GenerateUnitId(this);
	}

}
