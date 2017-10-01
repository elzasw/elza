package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkAction;

public class UnitIdConfig extends BaseActionConfig {

	/**
	 * Item type for storing UnitId
	 */
	String itemType;

	String previousIdCode;

	String previousIdSpecCode;

	String delimiterMajor;

	String delimiterMinor;

	String levelTypeCode;

	String delimiterMajorLevelTypeNotUse;

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

	public String getDelimiterMajor() {
		return delimiterMajor;
	}

	public void setDelimiterMajor(String delimiterMajor) {
		this.delimiterMajor = delimiterMajor;
	}

	public String getDelimiterMinor() {
		return delimiterMinor;
	}

	public void setDelimiterMinor(String delimiterMinor) {
		this.delimiterMinor = delimiterMinor;
	}

	public String getLevelTypeCode() {
		return levelTypeCode;
	}

	public void setLevelTypeCode(String levelTypeCode) {
		this.levelTypeCode = levelTypeCode;
	}

	public String getDelimiterMajorLevelTypeNotUse() {
		return delimiterMajorLevelTypeNotUse;
	}

	public void setDelimiterMajorLevelTypeNotUse(String delimiterMajorLevelTypeNotUse) {
		this.delimiterMajorLevelTypeNotUse = delimiterMajorLevelTypeNotUse;
	}

	@Override
	public BulkAction createBulkAction() {
		return new UnitIdBulkAction(this);
	}

}
