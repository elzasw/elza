package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.input.DEImportParams.ImportDirection;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;

public class ImportPosition {

	private final ArrFundVersion fundVersion;

	private final ArrLevel parentLevel;

	private final ArrLevel targetLevel;

	private final ImportDirection direction;

	private Integer levelPosition;

	public ImportPosition(ArrFundVersion fundVersion, ArrLevel parentLevel, ArrLevel targetLevel,
			ImportDirection direction) {
		this.fundVersion = Validate.notNull(fundVersion);
		this.parentLevel = Validate.notNull(parentLevel);
		this.targetLevel = targetLevel;
		this.direction = Validate.notNull(direction);
	}

	public ArrFundVersion getFundVersion() {
		return fundVersion;
	}

	public ArrLevel getParentLevel() {
		return parentLevel;
	}

	public ArrLevel getTargetLevel() {
		return targetLevel;
	}

	public ImportDirection getDirection() {
		return direction;
	}

	public Integer getLevelPosition() {
		return levelPosition;
	}

	void setLevelPosition(Integer levelPosition) {
		this.levelPosition = levelPosition;
	}
}