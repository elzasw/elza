package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.DateRangeActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
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

	private final DateRangeConfig config;
    /**
     * Vstupní atributy datace.
     */
	private RuleSystemItemType inputItemType;

    /**
     * Vstupní atributy datace - prior.
     */
	private RuleSystemItemType inputItemTypePrior;

    /**
     * Vstupní atributy datace - posterior.
     */
	private RuleSystemItemType inputItemTypePosterior;

    /**
     * Výstupní atribut
     */
	private RuleSystemItemType outputItemType;

    /**
     * Minimální čas prior
     */
    private long dateMinPrior = Long.MAX_VALUE;

    /**
     * Textová reprezentace minimální prior datace.
     */
    private String dateMinPriorString = null;

    /**
     * Minimální čas
     */
    private long dateMin = Long.MAX_VALUE;

    /**
     * Textová reprezentace minimální datace.
     */
    private String dateMinString = "?";

    /**
     * Maximální čas
     */
    private long dateMax = Long.MIN_VALUE;

    /**
     * Textová reprezentace maximální datace.
     */
    private String dateMaxString = "?";

    /**
     * Maximální čas posterior
     */
    private long dateMaxPosterior = Long.MIN_VALUE;

    /**
     * Textová reprezentace maximální posterior datace.
     */
    private String dateMaxPosteriorString = null;

	DateRangeAction(DateRangeConfig config) {
		this.config = config;
	}

    @Override
	public void init(ActionRunContext runContext) {
		RuleSystem ruleSystem = this.getRuleSystem(runContext);

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
		
		String inputTypePrior = config.getInputTypePrior();
		if (inputTypePrior == null) {
			throw new BusinessException("Není vyplněn parametr 'inputTypePrior' v akci.", BaseCode.PROPERTY_NOT_EXIST)
			        .set("property", "input_type_prior");
		}
		inputItemTypePrior = ruleSystem.getItemTypeByCode(inputTypePrior);
		checkValidDataType(inputItemTypePrior, DataType.UNITDATE);
		
		String inputTypePosterior = config.getInputTypePosterior();
		if (inputTypePosterior == null) {
		    throw new BusinessException("Není vyplněn parametr 'input_type_posterior' v akci.", BaseCode.PROPERTY_NOT_EXIST).set("property", "input_type_posterior");
		}
		inputItemTypePosterior = ruleSystem.getItemTypeByCode(inputTypePosterior);
		checkValidDataType(inputItemTypePosterior, DataType.UNITDATE);
		
    }

    @Override
	public void apply(LevelWithItems level, TypeLevel typeLevel) {
		List<ArrDescItem> items = level.getDescItems();

        for (ArrItem item : items) {

			if (item.isUndefined()) {
                continue;
            }

            // není použit záměrně if-else, protože teoreticky by šlo nakonfigurovat vše na stejnou položku
			Integer itemTypeId = item.getItemTypeId();
			if (inputItemType.getItemTypeId().equals(itemTypeId)) {
                ArrDataUnitdate data = (ArrDataUnitdate) item.getData();
                Long dataNormalizedFrom = data.getNormalizedFrom();
                if(dataNormalizedFrom==null) {
                	dataNormalizedFrom = Long.MAX_VALUE;
                }
                Long dataNormalizedTo = data.getNormalizedTo();
                if(dataNormalizedTo==null) {
                	dataNormalizedTo = Long.MIN_VALUE;
                }

                if (dateMin > dataNormalizedFrom) {
					dateMinString = UnitDateConvertor.beginToString(data);
                    dateMin = dataNormalizedFrom;
                }
                if (dateMax < dataNormalizedTo) {
                    dateMax = dataNormalizedTo;
                    dateMaxString = UnitDateConvertor.endToString(data);
                }
            }
			if (inputItemTypePrior.getItemTypeId().equals(itemTypeId)) {
                ArrDataUnitdate data = (ArrDataUnitdate) item.getData();
                Long dataNormalizedFrom = data.getNormalizedFrom();
                if(dataNormalizedFrom==null) {
                	dataNormalizedFrom = Long.MAX_VALUE;
                }
                if (dateMinPrior > dataNormalizedFrom) {
					dateMinPriorString = UnitDateConvertor.beginToString(data);
                    dateMinPrior = dataNormalizedFrom;
                }
            }
			if (inputItemTypePosterior.getItemTypeId().equals(itemTypeId)) {
                ArrDataUnitdate data = (ArrDataUnitdate) item.getData();
                Long dataNormalizedTo = data.getNormalizedTo();
                if(dataNormalizedTo==null) {
                	dataNormalizedTo = Long.MIN_VALUE;
                }
                if (dateMaxPosterior < dataNormalizedTo) {
                    dateMaxPosterior = dataNormalizedTo;
					dateMaxPosteriorString = UnitDateConvertor.endToString(data);
                }
            }
        }
    }

    @Override
    public ActionResult getResult() {
        DateRangeActionResult dateRangeResult = new DateRangeActionResult();

        dateRangeResult.setItemType(outputItemType.getCode());

        String text = "";

        if (dateMinPriorString != null) {
            text += "(" + dateMinPriorString + ") ";
        }

        text += dateMinString + "-" + dateMaxString;

        if (dateMaxPosteriorString != null) {
            text += " (" + dateMaxPosteriorString + ")";
        }

        dateRangeResult.setText(text);

        return dateRangeResult;
    }


}
