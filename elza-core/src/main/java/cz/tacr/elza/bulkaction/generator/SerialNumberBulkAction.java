package cz.tacr.elza.bulkaction.generator;

import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.result.SerialNumberResult;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * Hromadná akce prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy.
 *
 */
@Component
@Scope("prototype")
public class SerialNumberBulkAction extends BulkActionDFS {

    /**
     * Pomocná třída pro generování pořadových čísel
     */
	final private SerialNumber serialNumber = new SerialNumber();

    /**
     * Typ atributu
     */
    private RulItemType descItemType;

    /**
     * Počet změněných položek.
     */
    private Integer countChanges = 0;

	protected final SerialNumberConfig config;

	SerialNumberBulkAction(SerialNumberConfig config) {
		Validate.notNull(config);
		this.config = config;
	}

    /**
     * Inicializace hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
	@Override
	protected void init(ArrBulkActionRun bulkActionRun) {
		super.init(bulkActionRun);

		// prepare item type
		RuleSystemItemType itemType = ruleSystem.getItemTypeByCode(config.getItemType());
		Validate.notNull(itemType);

		descItemType = itemType.getEntity();
    }

    /**
     * Update number for given level
     * @param level Level to be updated
     */
	@Override
	protected void update(final ArrLevel level) {
        ArrNode currNode = level.getNode();

		ArrDescItem descItem = loadSingleDescItem(currNode, descItemType);
        int sn = serialNumber.getNext();

		ArrDataInteger item;

        // vytvoření nového atributu
        if (descItem == null) {
            descItem = new ArrDescItem();
            descItem.setItemType(descItemType);
            descItem.setNode(currNode);
			item = new ArrDataInteger();
			descItem.setData(item);
		} else if (descItem.isUndefined()) {
			item = new ArrDataInteger();
			descItem.setData(item);
		} else {
			ArrData data = descItem.getData();
			if (!(data instanceof ArrDataInteger)) {
				throw new BusinessException(descItemType.getCode() + " není typu ArrDescItemInt",
				        BaseCode.PROPERTY_HAS_INVALID_TYPE)
				                .set("property", descItemType.getCode())
				                .set("expected", "ArrItemInt")
				                .set("actual", descItem.getData().getClass().getSimpleName());
			}
			item = (ArrDataInteger) data;
        }

        // uložit pouze při rozdílu
		if (item.getValue() == null || sn != item.getValue()) {
            item.setValue(sn);
			ArrDescItem ret = saveDescItem(descItem, version, getChange());
            level.setNode(ret.getNode());
            countChanges++;
        }
    }

	protected void done() {
		SerialNumberResult snr = new SerialNumberResult();
		snr.setCountChanges(countChanges);

		this.result.getResults().add(snr);
	}

    /**
     * Generátor pořadových čísel.
     */
    private class SerialNumber {

        private int i;

        public SerialNumber() {
            this.i = 0;
        }

        public int getNext() {
            return ++i;
        }
    }

    @Override
	public String getName() {
		return "SerialNumberBulkAction";
    }
}
