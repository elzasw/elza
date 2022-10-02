package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.CopyActionResult;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Akce pro kopírování hodnot atributů.
 *
 */
@Component
@Scope("prototype")
public class CopyAction extends Action {

    /**
     * Vstupní atributy
     */
	private ItemType inputItemType;

    /**
     * Výstupní atribut
     */
	private ItemType outputItemType;

    /**
	 * Zkopírované hodnoty, výsledek
	 */
    private List<ArrDescItem> dataItems = new ArrayList<>();

	final CopyConfig config;

	CopyAction(final CopyConfig config) {
		Validate.notNull(config);
		this.config = config;
    }

    @Override
    public void init(BulkAction bulkAction, ArrBulkActionRun bulkActionRun) {
        super.init(bulkAction, bulkActionRun);

        StaticDataProvider sdp = getStaticDataProvider();

        inputItemType = sdp.getItemTypeByCode(config.getInputType());
        Validate.notNull(inputItemType);

        String outputType = config.getOutputType();
        if (StringUtils.isEmpty(outputType)) {
            outputItemType = inputItemType;
            return;
        }

        outputItemType = sdp.getItemTypeByCode(outputType);
        // check if input and output have same data type
        if (inputItemType.getDataType() != outputItemType.getDataType()) {
            throw new BusinessException("Item " + config.getInputType() + " and " + outputType + " have different data type",
                    BaseCode.PROPERTY_HAS_INVALID_TYPE);
        }
    }

	/**
	 * Check if item is used in output
	 *
	 * @param item
	 * @return
	 */
    private boolean isInResult(ArrDescItem item) {
        for (ArrDescItem dataItem : dataItems) {

            if(!Objects.equals(item.getItemSpecId(),  dataItem.getItemSpecId())) {
                continue;
            }
            ArrData cmpData = dataItem.getData();
            ArrData data = item.getData();
            // data are not null -> we can compare them
            if(data.isEqualValue(cmpData)) {
                return true;
            }
        }
        return false;
    }

	@Override
	public void apply(LevelWithItems level, TypeLevel typeLevel) {
		List<ArrDescItem> items = level.getDescItems();

        for (ArrDescItem item : items) {
			// check if item has same itemType
			if (!inputItemType.getItemTypeId().equals(item.getItemTypeId())) {
				continue;
			}
			// skip undefined items
			if (item.isUndefined()) {
				continue;
			}
			// check if exists
			if (config.isDistinct()) {
				if (isInResult(item)) {
					continue;
				}
			}

			// Copy item
			// TODO: Ma zde byt?, nutno overit
			/*
			ArrData itemData = item.getData();
			if (itemData == null) {
				itemData = descItemFactory.createItemByType(item.getItemType().getDataType());
			}
			itemData.setSpec(item.getItemSpec());
			*/
			dataItems.add(item);
        }
	}

    @Override
    public ActionResult getResult() {
        CopyActionResult copyActionResult = new CopyActionResult();
        copyActionResult.setItemType(outputItemType.getCode());
        copyActionResult.setDataItems(new ArrayList<>(dataItems));
        return copyActionResult;
    }


}
