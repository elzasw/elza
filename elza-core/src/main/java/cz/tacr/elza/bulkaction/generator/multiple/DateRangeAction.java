package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.DateRangeActionResult;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemUnitdate;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.utils.Yaml;

/**
 * Akce na zjištění rozsahu datací.
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
@Component
@Scope("prototype")
public class DateRangeAction extends Action {

    /**
     * Vstupní atributy datace.
     */
    private RulItemType inputItemType;

    /**
     * Vstupní atributy datace - prior.
     */
    private RulItemType inputItemTypePrior;

    /**
     * Vstupní atributy datace - posterior.
     */
    private RulItemType inputItemTypePosterior;

    /**
     * Výstupní atribut
     */
    private RulItemType outputItemType;

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

    DateRangeAction(final Yaml config) {
        super(config);
    }

    @Override
    public void init() {
        String outputType = config.getString("output_type", null);
        if (outputType == null) {
            throw new BusinessException("Není vyplněn parametr 'output_type' v akci.", BaseCode.PROPERTY_NOT_EXIST).set("property", "output_type");
        }

        String inputType = config.getString("input_type", null);
        if (inputType == null) {
            throw new BusinessException("Není vyplněn parametr 'input_type' v akci.", BaseCode.PROPERTY_NOT_EXIST).set("property", "input_type");
        }

        String inputTypePrior = config.getString("input_type_prior", null);
        if (inputTypePrior == null) {
            throw new BusinessException("Není vyplněn parametr 'input_type_prior' v akci.", BaseCode.PROPERTY_NOT_EXIST).set("property", "input_type_prior");
        }

        String inputTypePosterior = config.getString("input_type_posterior", null);
        if (inputTypePosterior == null) {
            throw new BusinessException("Není vyplněn parametr 'input_type_posterior' v akci.", BaseCode.PROPERTY_NOT_EXIST).set("property", "input_type_posterior");
        }

        Set<String> inputTypes = Sets.newHashSet(inputType, inputTypePrior, inputTypePosterior);

        Set<RulItemType> inputItemTypes = findItemTypes(inputTypes);
        for (RulItemType inputItemType : inputItemTypes) {
            if (inputType.equalsIgnoreCase(inputItemType.getCode())) {
                this.inputItemType = inputItemType;
            } else if(inputTypePrior.equalsIgnoreCase(inputItemType.getCode())) {
                this.inputItemTypePrior = inputItemType;
            } else if (inputTypePosterior.equalsIgnoreCase(inputItemType.getCode())) {
                this.inputItemTypePosterior = inputItemType;
            } else {
                throw new IllegalStateException("Neplatný typ v akci: " + inputItemType.getCode());
            }
            checkValidDataType(inputItemType, "UNITDATE");
        }

        outputItemType = findItemType(outputType, "output_type");
        checkValidDataType(outputItemType, "TEXT");
    }

    @Override
    public void apply(final ArrNode node, final List<ArrDescItem> items, final LevelWithItems parentLevelWithItems) {
        for (ArrItem item : items) {        	
            // není použit záměrně if-else, protože teoreticky by šlo nakonfigurovat vše na stejnou položku
            if (inputItemType.equals(item.getItemType())) {
                ArrItemUnitdate data = (ArrItemUnitdate) item.getItem();
                data.setFormat(UnitDateConvertor.DATE);
                Long dataNormalizedFrom = data.getNormalizedFrom();
                if(dataNormalizedFrom==null) {
                	dataNormalizedFrom = Long.MAX_VALUE;
                }
                Long dataNormalizedTo = data.getNormalizedTo();
                if(dataNormalizedTo==null) {
                	dataNormalizedTo = Long.MIN_VALUE;
                }
                
                if (dateMin > dataNormalizedFrom) {
                    dateMinString = UnitDateConvertor.convertToString(data);
                    dateMin = dataNormalizedFrom;
                }
                if (dateMax < dataNormalizedTo) {
                    dateMax = dataNormalizedTo;
                    dateMaxString = UnitDateConvertor.convertToString(data);
                }
            }
            if (inputItemTypePrior.equals(item.getItemType())) {
                ArrItemUnitdate data = (ArrItemUnitdate) item.getItem();
                data.setFormat(UnitDateConvertor.DATE);
                Long dataNormalizedFrom = data.getNormalizedFrom();
                if(dataNormalizedFrom==null) {
                	dataNormalizedFrom = Long.MAX_VALUE;
                }
                if (dateMinPrior > dataNormalizedFrom) {
                    dateMinPriorString = UnitDateConvertor.convertToString(data);
                    dateMinPrior = dataNormalizedFrom;
                }
            }
            if (inputItemTypePosterior.equals(item.getItemType())) {
                ArrItemUnitdate data = (ArrItemUnitdate) item.getItem();
                data.setFormat(UnitDateConvertor.DATE);
                Long dataNormalizedTo = data.getNormalizedTo();
                if(dataNormalizedTo==null) {
                	dataNormalizedTo = Long.MIN_VALUE;
                }
                if (dateMaxPosterior < dataNormalizedTo) {
                    dateMaxPosterior = dataNormalizedTo;
                    dateMaxPosteriorString = UnitDateConvertor.convertToString(data);
                }
            }
        }
    }

    @Override
    public boolean canApply(final TypeLevel typeLevel) {
        if (typeLevel.equals(TypeLevel.PARENT) && applyParents) {
            return true;
        }

        if (typeLevel.equals(TypeLevel.CHILD) && applyChildren) {
            return true;
        }

        return false;
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
