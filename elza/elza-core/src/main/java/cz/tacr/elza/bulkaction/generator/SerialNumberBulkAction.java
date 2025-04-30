package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.common.db.HibernateUtils;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.result.SerialNumberResult;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Hromadná akce prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy.
 *
 */
@Component
@Scope("prototype")
public class SerialNumberBulkAction extends BulkActionDFS {

    /**
	 * Number generator.
	 */
	private Generator generator;

    /**
     * Typ atributu.
     */
    private RulItemType descItemType;

    /**
     * Node úrovně
     */
    private ArrNode levelNode;
    
    /**
     * Počet změněných položek.
     */
    private Integer countChanges = 0;

	protected final SerialNumberConfig config;

	public SerialNumberBulkAction(SerialNumberConfig config) {
		Objects.requireNonNull(config);
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

        multipleItemChangeContext = descriptionItemService.createChangeContext(version.getFundVersionId(), true);

		// prepare item type
		ItemType itemType = staticDataProvider.getItemTypeByCode(config.getItemType());
		Objects.requireNonNull(itemType);

		// check if supported type
		switch (itemType.getDataType()) {
		case INT:
			if (config.getUseCurrentNumbering()) {
				throw new SystemException("Unsupported item type: " + itemType.getDataType().getCode(), BaseCode.SYSTEM_ERROR);
			}
			generator = new IntegerGenerator();
			break;
		case STRING:
			generator = new StringGenerator(config.getUseCurrentNumbering());
			break;
		default:
			throw new SystemException("Unsupported item type: " + itemType.getDataType().getCode(), BaseCode.SYSTEM_ERROR);
		}

		descItemType = itemType.getEntity();
    }

    /**
     * Update number for given level
     * @param level Level to be updated
     */
	@Override
	protected void update(final ArrLevel level) {
        levelNode = level.getNode();

		ArrDescItem descItem = loadSingleDescItem(levelNode, descItemType);

		generator.generate(descItem);
    }

	@Override
    protected void done() {
		SerialNumberResult snr = new SerialNumberResult();
		snr.setCountChanges(countChanges);
		result.getResults().add(snr);
	}

	private interface Generator {
		void generate(ArrDescItem descItem);
	}

    /**
     * Generátor pořadových čísel.
     */
	private class IntegerGenerator implements Generator {

        private int counter;

		public IntegerGenerator() {
            this.counter = 0;
        }

		@Override
		public void generate(ArrDescItem descItem) {

			counter++;

			// vytvoření nového atributu
			if (descItem == null) {
	            descItem = new ArrDescItem();
	            descItem.setItemType(descItemType);
	            descItem.setNode(levelNode);
				
	            ArrDataInteger dataInteger = new ArrDataInteger();
				dataInteger.setIntegerValue(counter);
				descItem.setData(dataInteger);
                saveNewDescItem(descItem);
				countChanges++;
			} else {
				ArrData data = HibernateUtils.unproxy(descItem.getData());
				if (!(data instanceof ArrDataInteger)) {
					throw new BusinessException(descItemType.getCode() + " není typu ArrDataInteger",
					        BaseCode.PROPERTY_HAS_INVALID_TYPE)
					                .set("property", descItemType.getCode())
					                .set("expected", "ArrItemInt")
					                .set("actual", descItem.getData().getClass().getSimpleName());
				}
				ArrDataInteger dataInteger = (ArrDataInteger) ArrData.makeCopyWithoutId(data);
				Integer value = dataInteger.getIntegerValue();

				// uložit pouze při rozdílu
				if (counter != value) {
					dataInteger.setIntegerValue(counter);
	                updateDescItem(descItem, dataInteger);
					countChanges++;
				}
			}
		}
    }

	/**
     * Generátor inventárních čísel.
	 */
	private class StringGenerator implements Generator {

		private int counter = 0;

		private String prefix = "";

		private int minLength;

		private boolean useCurrentNumbering;

		public StringGenerator(boolean useCurrentNumbering) {
			this.useCurrentNumbering = useCurrentNumbering;
		}

		protected void setLastNumber(String lastNumber) {
			minLength = lastNumber.length();

			int pos = minLength;

			//
			while (pos > 0) {
				char c = lastNumber.charAt(pos - 1);
				if (c < '0' || c > '9') {
					break;
				}
				pos--;
			}
			if (pos > 0) {
				prefix = lastNumber.substring(0, pos);
				counter = Integer.parseInt(lastNumber.substring(pos));
			} else {
				prefix = "";
				counter = Integer.parseInt(lastNumber);
			}
		}

		private String prepareValue() {
			String numberPart;
			if (minLength < 1) {
				numberPart = Integer.valueOf(counter).toString();
			} else {
				String format = String.format("%%0%dd", minLength - prefix.length());
				numberPart = String.format(format, counter);
			}

			return prefix + numberPart;
		}

		@Override
		public void generate(ArrDescItem descItem) {

			counter++;

			ArrDataString item;
			// vytvoření nového atributu
			if (descItem == null) {
	            descItem = new ArrDescItem();
	            descItem.setItemType(descItemType);
	            descItem.setNode(levelNode);

	            ArrDataString dataString = new ArrDataString();
				descItem.setData(dataString);
				dataString.setStringValue(prepareValue());
                saveNewDescItem(descItem);
				countChanges++;
			} else {
				ArrData data = HibernateUtils.unproxy(descItem.getData());
				if (!(data instanceof ArrDataString)) {
					throw new BusinessException(descItemType.getCode() + " není typu ArrDataString",
					        BaseCode.PROPERTY_HAS_INVALID_TYPE)
					                .set("property", descItemType.getCode())
					                .set("expected", "ArrDataString")
					                .set("actual", descItem.getData().getClass().getSimpleName());
				}
				ArrDataString dataString = (ArrDataString) ArrData.makeCopyWithoutId(data);
				String value = dataString.getStringValue();
				if (useCurrentNumbering && StringUtils.isNotBlank(value)) {
					// read current value
					setLastNumber(value);
				} else {
					String nextValue = prepareValue();
					// uložit pouze při rozdílu
					if (!nextValue.equals(value)) {
						dataString.setStringValue(nextValue);
						updateDescItem(descItem, dataString);
						countChanges++;
					}
				}
			}
		}
	}

    @Override
	public String getName() {
		return "SerialNumberBulkAction";
    }
}
