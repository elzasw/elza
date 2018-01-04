package cz.tacr.elza.bulkaction.generator.multiple;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.DateRangeActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Akce na zjištění rozsahu datací.
 *
 */
@Component
@Scope("prototype")
public class DateRangeAction extends Action {

    protected final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private final DateRangeConfig config;
    /**
     * Vstupní atributy datace.
     */
	private RuleSystemItemType inputItemType;

    /**
     * Vlastní rozsah fondu.
     */
    private RuleSystemItemType bulkRangeType;

    /**
     * Výstupní atribut
     */
	private RuleSystemItemType outputItemType;

    /**
     * Minimální čas prior
     */
    private ArrDataUnitdate datePriorMin;
    private ArrDataUnitdate datePriorMax;
    /**
     * Flag that prior max should be same as date min
     */
    private boolean priorMaxAsDateMin = false;

    /**
     * Minimální čas
     */
    private ArrDataUnitdate dateMin;

    /**
     * Maximální čas
     */
    private ArrDataUnitdate dateMax;

    /**
     * Maximální čas posterior
     */
    private ArrDataUnitdate datePosteriorMin;
    private ArrDataUnitdate datePosteriorMax;

    /**
     * Flag that posterior min should be same as date max
     */
    private boolean posteriorMinAsDateMax = false;

	DateRangeAction(DateRangeConfig config) {
		this.config = config;
	}

    @Override
	public void init(ArrBulkActionRun bulkActionRun) {
		RuleSystem ruleSystem = this.getRuleSystem(bulkActionRun);

		// prepare output type
		String outputType = config.getOutputType();
		if (outputType == null) {
			throw new BusinessException("Není vyplněn parametr 'output_type' v akci.", BaseCode.PROPERTY_NOT_EXIST)
			        .set("property", "outputType");
		}
		outputItemType = ruleSystem.getItemTypeByCode(outputType);
		if (outputItemType.getDataType() != DataType.TEXT) {
			throw new BusinessException(
			        "Datový typ atributu musí být " + DataType.TEXT + " (item type " + outputType + ")",
			        BaseCode.ID_NOT_EXIST);
		}
		
		String inputType = config.getInputType();
		if (inputType == null) {
			throw new BusinessException("Není vyplněn parametr 'inputType' v akci.", BaseCode.PROPERTY_NOT_EXIST)
			        .set("property", "input_type");
		}
		inputItemType = ruleSystem.getItemTypeByCode(inputType);
		checkValidDataType(inputItemType, DataType.UNITDATE);
		
        String bulkRangeCode = config.getBulkRangeType();
        if (bulkRangeCode == null) {
            throw new BusinessException("Není vyplněn parametr 'bulkRangeType' v akci.", BaseCode.PROPERTY_NOT_EXIST)
                    .set("property", "bulkRangeType");
		}
        bulkRangeType = ruleSystem.getItemTypeByCode(bulkRangeCode);
        checkValidDataType(bulkRangeType, DataType.UNITDATE);
		
    }

    @Override
	public void apply(LevelWithItems level, TypeLevel typeLevel) {
		List<ArrDescItem> items = level.getDescItems();

		// iterate all items and find unit date
        for (ArrItem item : items) {

			if (item.isUndefined()) {
                continue;
            }

			Integer itemTypeId = item.getItemTypeId();
			if (inputItemType.getItemTypeId().equals(itemTypeId)) {
                ArrDataUnitdate unitDate = (ArrDataUnitdate) item.getData();
                processUnitDate(unitDate, level);
            }
        }
    }

    private void processUnitDate(ArrDataUnitdate unitDate, LevelWithItems level) {

        // get bulk date (if exists)
        List<ArrDescItem> bulkRanges = null;
        if (bulkRangeType != null) {
            bulkRanges = level.getInheritedDescItems(bulkRangeType);
        }

        if (CollectionUtils.isEmpty(bulkRanges)) {
            processMainUnitDate(unitDate);
            return;
        }

        // process as possible prior/posterior
        // set default bulk range
        ArrDataUnitdate bulkFrom = null;
        ArrDataUnitdate bulkTo = null;

        for (ArrDescItem bulkRange : bulkRanges) {
            // check range
            ArrDataUnitdate bulkUnitDate = (ArrDataUnitdate) bulkRange.getData();

            if (bulkFrom == null || bulkFrom.getNormalizedFrom() > bulkUnitDate.getNormalizedFrom()) {
                bulkFrom = bulkUnitDate;
            }

            if (bulkTo == null || bulkTo.getNormalizedTo() < bulkUnitDate.getNormalizedTo()) {
                bulkTo = bulkUnitDate;
            }
        }

        processUnitDate(unitDate, bulkFrom, bulkTo);
    }

    /**
     * Compare unit date with bulk interval
     * 
     * @param unitDate
     * @param bulkFrom
     * @param bulkTo
     */
    private void processUnitDate(ArrDataUnitdate unitDate, ArrDataUnitdate bulkFrom, ArrDataUnitdate bulkTo) {
        // normalized form cannot be null
        Long dataNormalizedFrom = unitDate.getNormalizedFrom();
        Long dataNormalizedTo = unitDate.getNormalizedTo();
        // check if FROM inside or outside bulk range
        boolean fromStoredAsPrior = false, toStoredAsPrior = false;
        boolean fromStoredAsPosterior = false, toStoredAsPosterior = false;
        if (dataNormalizedFrom < bulkFrom.getNormalizedFrom()) {
            // store as prior
            if (datePriorMin == null || datePriorMin.getNormalizedFrom() > dataNormalizedFrom) {
                datePriorMin = unitDate;
            }
            fromStoredAsPrior = true;
        } else if (dataNormalizedFrom <= bulkTo.getNormalizedTo()) {
            // store as standard date
            if (dateMin == null || dateMin.getNormalizedFrom() > dataNormalizedFrom) {
                dateMin = unitDate;
            }

        } else {
            // store as posterior
            if (datePosteriorMin == null || dataNormalizedFrom < datePosteriorMin.getNormalizedFrom()) {
                datePosteriorMin = unitDate;
            }
            fromStoredAsPosterior = true;
        }

        // check upper boundary
        if (dataNormalizedTo < bulkFrom.getNormalizedFrom()) {
            // dates end before bulk interval
            if (datePriorMax == null || datePriorMax.getNormalizedTo() < dataNormalizedTo) {
                datePriorMax = unitDate;
            }
            // from must be stored as prior if to is prior
            Validate.isTrue(fromStoredAsPrior);
            toStoredAsPrior = true;
        } else if (dataNormalizedTo <= bulkTo.getNormalizedTo()) {
            // standard date
            if (dateMax == null || dateMax.getNormalizedTo() < dataNormalizedTo) {
                dateMax = unitDate;
            }
            // from cannot be posterior
            Validate.isTrue(fromStoredAsPosterior == false);

        } else {
            // dates end behind bulk interval
            if (datePosteriorMax == null || dataNormalizedTo > datePosteriorMax.getNormalizedTo()) {
                datePosteriorMax = unitDate;
            }
            toStoredAsPosterior = true;
        }

        // set new boundaries if dates are crossing bulk boundaries
        if (fromStoredAsPrior && !toStoredAsPrior) {
            // set end of prior as beginning of bulkFrom
            if (dateMin == null || bulkFrom.getNormalizedFrom() < dateMin.getNormalizedFrom()) {
                dateMin = bulkFrom;
            }
            priorMaxAsDateMin = true;
        }

        if (!fromStoredAsPosterior && toStoredAsPosterior) {
            // set end of standard date as end of bulkTo
            if (dateMax == null || dateMax.getNormalizedTo() < bulkTo.getNormalizedTo()) {
                dateMax = bulkTo;
            }
            posteriorMinAsDateMax = true;
        }
    }

    /**
     * Process unit date as main date
     * 
     * Method does not compare prior and posterior dates
     * 
     * @param unitDate
     */
    private void processMainUnitDate(ArrDataUnitdate unitDate) {
        // store as standard range
        if (dateMin == null || dateMin.getNormalizedFrom() > unitDate.getNormalizedFrom()) {
            dateMin = unitDate;
        }
        if (dateMax == null || dateMax.getNormalizedTo() < unitDate.getNormalizedTo()) {
            dateMax = unitDate;
        }
    }

    @Override
    public ActionResult getResult() {
        DateRangeActionResult dateRangeResult = new DateRangeActionResult();

        dateRangeResult.setItemType(outputItemType.getCode());

        StringBuilder sb = new StringBuilder();

        // append prior
        if (datePriorMin != null
                && (dateMin == null || datePriorMin.getNormalizedFrom() < dateMin.getNormalizedFrom())) {
            sb.append("(");
            appendTimeInterval(sb, datePriorMin, priorMaxAsDateMin ? null : datePriorMax);
            sb.append(") ");
        }

        // append standard date
        if (dateMin != null) {
            appendTimeInterval(sb, dateMin, dateMax);
        }

        // append posterior
        if (datePosteriorMax != null
                && (dateMax == null || datePosteriorMax.getNormalizedTo() > dateMax.getNormalizedTo())) {
            sb.append(" (");
            appendTimeInterval(sb, posteriorMinAsDateMax ? null : datePosteriorMin, datePosteriorMax);
            sb.append(")");
        }

        dateRangeResult.setText(sb.toString());

        return dateRangeResult;
    }

    /**
     * Any bounder might be null
     * 
     * @param sb
     * @param datePriorMin
     * @param datePriorMax
     */
    private void appendTimeInterval(StringBuilder sb, ArrDataUnitdate minDate, ArrDataUnitdate maxDate) {
        if (minDate != null) {
            sb.append(UnitDateConvertor.beginToString(minDate));
        }
        sb.append("-");
        if (maxDate != null) {
            sb.append(UnitDateConvertor.endToString(maxDate));
        }
    }


}
