package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.bulkaction.generator.result.UnitIdResult;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.DescItemRepository;

/**
 * Hromadná akce prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy.
 *
 * @author Martin Šlapa
 * @since 21.10.2015
 */
public class UnitIdBulkAction extends BulkAction {

    /**
     * Identifikátor hromadné akce
     */
    public static final String TYPE = "GENERATOR_UNIT_ID";

    /**
     * Změna
     */
    private ArrChange change;

    /**
     * Typ atributu
     */
    private RulItemType descItemType;

    /**
     * Typ atributu levelu
     */
    private RulItemType descItemLevelType;

    /**
     * Typ atributu pro předchozí uložení
     */
    private RulItemType descItemPreviousType;

    /**
     * Specifikace atributu pro předchozí uložení
     */
    private RulItemSpec descItemPreviousSpec;

    /**
     * Vedlejší oddělovač
     */
    private String delimiterMinor;

    /**
     * Hlavní oddělovač
     */
    private String delimiterMajor;

    /**
     * Seznam kódů typů uzlů při které se nepoužije major oddělovač.
     */
    private List<String> delimiterMajorLevelTypeNotUseList;

    /**
     * Počet změněných položek.
     */
    private Integer countChanges = 0;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescItemFactory descItemFactory;

	protected final UnitIdConfig config;

	public UnitIdBulkAction(UnitIdConfig unitIdConfig) {
		Validate.notNull(unitIdConfig);
		this.config = unitIdConfig;
	}

	/**
	 * Inicializace hromadné akce.
	 *
	 */
	@Override
	protected void init(ArrBulkActionRun bulkActionRun) {
		super.init(bulkActionRun);

		// read item type for UnitId
		String unitIdCode = config.getItemType();
		Validate.notNull(unitIdCode);
		RuleSystemItemType itemTypeWrapper = staticDataProvider.getItemTypeByCode(unitIdCode);
		Validate.notNull(itemTypeWrapper);
		descItemType = itemTypeWrapper.getEntity();

		// read level type
		String levelTypeCode = config.getLevelTypeCode();
		Validate.notNull(levelTypeCode);
		RuleSystemItemType levelTypeWrapper = staticDataProvider.getItemTypeByCode(levelTypeCode);
		Validate.notNull(levelTypeWrapper);
		descItemLevelType = levelTypeWrapper.getEntity();

		// read delimiters
		delimiterMajor = config.getDelimiterMajor();
		Validate.notNull(delimiterMajor);

		delimiterMinor = config.getDelimiterMinor();
		Validate.notNull(delimiterMinor);

		// item for previous value
		String previousIdCode = config.getPreviousIdCode();
		Validate.notNull(previousIdCode);
		RuleSystemItemType previousIdTypeWrapper = staticDataProvider.getItemTypeByCode(previousIdCode);
		Validate.notNull(previousIdTypeWrapper);
		descItemPreviousType = previousIdTypeWrapper.getEntity();

		String previousIdSpecCode = config.getPreviousIdSpecCode();
		Validate.notNull(previousIdSpecCode);
		descItemPreviousSpec = previousIdTypeWrapper.getItemSpecByCode(previousIdSpecCode);
		Validate.notNull(descItemPreviousSpec);

		String delimiterMajorLevelTypeNotUse = config.getDelimiterMajorLevelTypeNotUse();
		if (delimiterMajorLevelTypeNotUse == null) {
		    delimiterMajorLevelTypeNotUseList = new ArrayList<>();
		} else {
		    delimiterMajorLevelTypeNotUseList = Arrays.asList(delimiterMajorLevelTypeNotUse.split("\\|"));
		}
    }

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *  @param level          uzel
     * @param rootNode
     * @param unitId         generátor pořadových čísel
     * @param parentSpecCode specifický kód rodiče
     */
    private void generate(final ArrLevel level, final ArrNode rootNode, UnitId unitId, final String parentSpecCode) {
        if (bulkActionRun.isInterrupted()) {
            bulkActionRun.setState(State.INTERRUPTED);
            throw new BusinessException("Hromadná akce " + toString() + " byla přerušena.", ArrangementCode.BULK_ACTION_INTERRUPTED).set("code", bulkActionRun.getBulkActionCode());
        }
        change = bulkActionRun.getChange();

        ArrDescItem descItemLevel = loadDescItemLevel(level);
        if (!level.getNode().equals(rootNode)) {

            if (level.getNodeParent() != null) {

                ArrDescItem descItem = loadDescItem(level);

                if (unitId == null) {
                    unitId = new UnitId(1);
                } else {
                    String specCode = descItemLevel == null ? null : descItemLevel.getItemSpec().getCode();

                    if ((specCode == null && parentSpecCode == null)
                            || (specCode != null && specCode.equals(parentSpecCode))
                            || (parentSpecCode != null && parentSpecCode.equals(specCode))
                            || (delimiterMajorLevelTypeNotUseList.contains(specCode))) {
                        unitId.setSeparator(delimiterMinor);
                    } else {
                        unitId.setSeparator(delimiterMajor);
                    }

                    unitId.genNext();
                }

				ArrDataUnitid dataUnitId;
                // vytvoření nového atributu
                if (descItem == null) {
                    descItem = new ArrDescItem();
                    descItem.setItemType(descItemType);
                    descItem.setNode(level.getNode());
					dataUnitId = new ArrDataUnitid();
					descItem.setData(dataUnitId);
				} else if (descItem.isUndefined()) {
					// create new value if not exists
					dataUnitId = new ArrDataUnitid();
					descItem.setData(dataUnitId);
				} else {
					ArrData data = descItem.getData();

					if (!(data instanceof ArrDataUnitid)) {
						throw new IllegalStateException(descItemType.getCode() + " neni typu ArrDescItemUnitid");
					}

					dataUnitId = (ArrDataUnitid) data;
                }

                // uložit pouze při rozdílu
				if (dataUnitId.getValue() == null || !unitId.getData().equals(dataUnitId.getValue())) {

                    ArrDescItem ret;

                    // uložit původní hodnotu pouze při první změně z předchozí verze
                    if (descItem.getDescItemObjectId() != null && descItem.getCreateChange().getChangeId() < version
                            .getCreateChange().getChangeId()) {
                        ArrDescItem descItemPrev = new ArrDescItem();
                        ArrData dataPrev = descItemPrev.getData();
                        descItemPrev.setItemType(descItemPreviousType);
                        descItemPrev.setItemSpec(descItemPreviousSpec);
                        descItemPrev.setNode(level.getNode());

                        if (dataPrev instanceof ArrDataString) {
                            ((ArrDataString) dataPrev).setValue(((ArrDataUnitid) dataPrev).getValue());
                        } else {
                            throw new IllegalStateException(
                                    descItemPrev.getClass().getName() + " nema definovany prevod hodnoty");
                        }

                        ret = saveDescItem(descItemPrev, version, change);
                        level.setNode(ret.getNode());

                    }

					dataUnitId.setValue(unitId.getData());

                    ret = saveDescItem(descItem, version, change);
                    level.setNode(ret.getNode());
                    countChanges++;
                }

            }
        }

        List<ArrLevel> childLevels = getChildren(level);

        if (unitId == null) {
            unitId = new UnitId("");
            unitId.setSeparator("");
        }

        UnitId unitIdChild = null;
        for (ArrLevel childLevel : childLevels) {
            if (unitId != null && unitIdChild == null) {
                unitIdChild = unitId.getClone();
            }
            generate(childLevel, rootNode, unitIdChild, descItemLevel == null ? null : descItemLevel.getItemSpec().getCode());
        }

    }

    /**
     * Načtení atributu.
     *
     * @param level uzel
     * @return nalezený atribut
     */
    private ArrDescItem loadDescItem(final ArrLevel level) {
        List<ArrDescItem> descItems = descItemRepository
                .findByNodeAndDeleteChangeIsNullAndItemTypeId(level.getNode(), descItemType.getItemTypeId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new IllegalStateException(
                    descItemType.getCode() + " nemuze byt vice nez jeden (" + descItems.size() + ")");
        }
        return descItems.get(0);
    }

    /**
     * Načtení atributu - level.
     *
     * @param level uzel
     * @return nalezený atribut
     */
    private ArrDescItem loadDescItemLevel(final ArrLevel level) {
        List<ArrDescItem> descItems = descItemRepository
                .findByNodeAndDeleteChangeIsNullAndItemTypeId(level.getNode(),
                        descItemLevelType.getItemTypeId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new IllegalStateException(
                    descItemType.getCode() + " nemuze byt vice nez jeden (" + descItems.size() + ")");
        }
        return descItems.get(0);
    }


	@Override
	public void run(ActionRunContext runContext) {
        ArrNode rootNode = version.getRootNode();

		for (Integer nodeId : runContext.getInputNodeIds()) {
            ArrNode nodeRef = nodeRepository.getOne(nodeId);
            ArrLevel level = levelRepository.findByNodeAndDeleteChangeIsNull(nodeRef);
			Validate.notNull(level);

            ArrDescItem descItem = loadDescItem(level);
            ArrDescItem descItemLevel = loadDescItemLevel(level);
			if (descItem != null && !descItem.isUndefined()) {
                ArrData item = descItem.getData();

                if (!(item instanceof ArrDataUnitid)) {
                    throw new IllegalStateException(descItemType.getCode() + " neni typu ArrDescItemUnitid");
                }

                List<ArrLevel> childLevels = getChildren(level);

                String value = ((ArrDataUnitid) item).getValue();
                UnitId unitId = new UnitId(value);
                unitId.setSeparator("");

                UnitId unitIdChild = null;
                for (ArrLevel childLevel : childLevels) {
                    if (unitIdChild == null) {
                        unitIdChild = unitId.getClone();
                    }
                    generate(childLevel, rootNode, unitIdChild, descItemLevel == null ? null : descItemLevel.getItemSpec().getCode());
                }

            } else if(nodeId.equals(rootNode.getNodeId())) {
                generate(level, rootNode, null, descItemLevel == null ? null : descItemLevel.getItemSpec().getCode());
            }

        }

        Result resultBA = new Result();
        UnitIdResult result = new UnitIdResult();
        result.setCountChanges(countChanges);
        resultBA.getResults().add(result);
        bulkActionRun.setResult(resultBA);
    }

    /**
     * Generátor pořadových čísel.
     */
    private class UnitId {

        String data;
        Integer id = null;
        String separator = null;

        public UnitId(final Integer id) {
            this.id = id;
            this.data = "";
            this.separator = "";
        }

        private UnitId(final String data) {
            this.data = data;
        }

        public String getData() {
            String tmp;
            if (this.data.equals("")) {
                tmp = (id == null ? "" : id.toString());
            } else {
                tmp = this.data + separator + (id == null ? "" : id.toString());
            }
            return tmp;
        }

        public UnitId getClone() {
            return new UnitId(getData());
        }

        public void setSeparator(final String separator) {
            this.separator = separator;
        }

        public void genNext() {
            if (id == null) {
                id = 1;
            } else {
                id++;
            }
        }
    }

    @Override
	public String getName() {
		return "UnitIdBulkAction";
    }
}
