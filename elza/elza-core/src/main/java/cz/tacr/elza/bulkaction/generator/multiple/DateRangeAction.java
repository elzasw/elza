package cz.tacr.elza.bulkaction.generator.multiple;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.DateRangeActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
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

    public static final String PARAM_PROPERTY = "property";

	private final DateRangeConfig config;
    /**
     * Skip subtree
     */
    LevelWithItems skipSubtree;

    /**
     * Vstupní atributy datace.
     */
	private ItemType inputItemType;

    /**
     * Vlastní rozsah fondu.
     */
    private ItemType bulkRangeType;

    /**
     * Výstupní atribut
     */
	private ItemType outputItemType;

    /**
     * Minimální čas prior
     */
    private ArrDataUnitdate datePriorMin;
    private ArrDataUnitdate datePriorMax;

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

    private WhenCondition excludeWhen;

	DateRangeAction(DateRangeConfig config) {
		this.config = config;
	}

    @Override
    public void init(BulkAction bulkAction, ArrBulkActionRun bulkActionRun) {
        super.init(bulkAction, bulkActionRun);

        StaticDataProvider sdp = this.getStaticDataProvider();

        // initialize exclude configuration
        WhenConditionConfig excludeWhenConfig = config.getExcludeWhen();
        if (excludeWhenConfig != null) {
            excludeWhen = new WhenCondition(excludeWhenConfig, sdp);
        }

		// prepare output type
		String outputType = config.getOutputType();
		if (outputType == null) {
			throw new BusinessException("Není vyplněn parametr 'output_type' v akci.", BaseCode.PROPERTY_NOT_EXIST)
                    .set(PARAM_PROPERTY, "outputType");
		}
        outputItemType = sdp.getItemTypeByCode(outputType);
		if (outputItemType.getDataType() != DataType.TEXT) {
			throw new BusinessException(
			        "Datový typ atributu musí být " + DataType.TEXT + " (item type " + outputType + ")",
			        BaseCode.ID_NOT_EXIST);
		}

		String inputType = config.getInputType();
		if (inputType == null) {
			throw new BusinessException("Není vyplněn parametr 'inputType' v akci.", BaseCode.PROPERTY_NOT_EXIST)
                    .set(PARAM_PROPERTY, "input_type");
		}
        inputItemType = sdp.getItemTypeByCode(inputType);
		checkValidDataType(inputItemType, DataType.UNITDATE);

        String bulkRangeCode = config.getBulkRangeType();
        if (bulkRangeCode == null) {
            throw new BusinessException("Není vyplněn parametr 'bulkRangeType' v akci.", BaseCode.PROPERTY_NOT_EXIST)
                    .set(PARAM_PROPERTY, "bulkRangeType");
		}
        bulkRangeType = sdp.getItemTypeByCode(bulkRangeCode);
        checkValidDataType(bulkRangeType, DataType.UNITDATE);

    }

    /**
     * Mark level to be skipped
     * 
     * @param level
     */
    public void setSkipSubtree(LevelWithItems level) {
        this.skipSubtree = level;
    }

    @Override
	public void apply(LevelWithItems level, TypeLevel typeLevel) {
        // Check if node stopped
        if (skipSubtree != null) {
            if (isInTree(skipSubtree, level)) {
                return;
            }
            // reset limit
            skipSubtree = null;
        }

        // check exclude condition
        if (excludeWhen != null) {
            if (excludeWhen.isTrue(level)) {
                // set as skip
                setSkipSubtree(level);
                return;
            }
        }

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

            if (bulkFrom == null || bulkFrom.getNormalizedFrom() > bulkUnitDate.getNormalizedFrom()
            // if same value -> not estimated is more important then estimated
                    || (Objects.equals(bulkFrom.getNormalizedFrom(), bulkUnitDate.getNormalizedFrom())
                            && !Boolean.TRUE.equals(bulkUnitDate.getValueFromEstimated()))) {
                bulkFrom = bulkUnitDate;
            }

            if (bulkTo == null || bulkTo.getNormalizedTo() < bulkUnitDate.getNormalizedTo() ||
            // if same value -> not estimated is more important then estimated
                    (Objects.equals(bulkTo.getNormalizedTo(), bulkUnitDate.getNormalizedTo())
                            && !Boolean.TRUE.equals(bulkUnitDate.getValueToEstimated()))) {
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
        }

        if (!fromStoredAsPosterior && toStoredAsPosterior) {
            // set end of standard date as end of bulkTo
            if (dateMax == null || dateMax.getNormalizedTo() < bulkTo.getNormalizedTo()) {
                dateMax = bulkTo;
            }
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
        if (dateMin == null || dateMin.getNormalizedFrom() > unitDate.getNormalizedFrom()
                || (Objects.equals(dateMin.getNormalizedFrom(), unitDate.getNormalizedFrom())
                        && !unitDate.getValueFromEstimated())) {

            dateMin = unitDate;
        }
        if (dateMax == null || dateMax.getNormalizedTo() < unitDate.getNormalizedTo()
                || (Objects.equals(dateMax.getNormalizedTo(), unitDate.getNormalizedTo())
                        && !unitDate.getValueToEstimated())) {
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
            // add only start of interval
            sb.append(UnitDateConvertor.beginToString(datePriorMin, true));
            //appendTimeInterval(sb, datePriorMin, priorMaxAsDateMin ? null : datePriorMax);
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
            // add only end of interval
            sb.append(UnitDateConvertor.endToString(datePosteriorMax, true));
            //appendTimeInterval(sb, posteriorMinAsDateMax ? null : datePosteriorMin, datePosteriorMax);
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
            sb.append(UnitDateConvertor.beginToString(minDate, true));
        }
        sb.append("-");
        if (maxDate != null) {
            sb.append(UnitDateConvertor.endToString(maxDate, true));
        }
    }


}
