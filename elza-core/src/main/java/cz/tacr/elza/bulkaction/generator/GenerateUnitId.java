package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.bulkaction.generator.result.UnitIdResult;
import cz.tacr.elza.bulkaction.generator.unitid.SealedUnitIdTree;
import cz.tacr.elza.bulkaction.generator.unitid.UnitIdException;
import cz.tacr.elza.bulkaction.generator.unitid.UnitIdGenerator;
import cz.tacr.elza.bulkaction.generator.unitid.UnitIdGeneratorParams;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.UsedValueRepository;

/**
 * Hromadná akce prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy.
 *
 */
public class GenerateUnitId extends BulkAction {

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

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    UsedValueRepository usedValueRepository;

	protected final GenerateUnitIdConfig config;

    private SealedUnitIdTree sealedUnitIdTree;

    private RulItemSpec extraSlashLevelSpec;

	public GenerateUnitId(GenerateUnitIdConfig unitIdConfig) {
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
		ItemType itemTypeWrapper = staticDataProvider.getItemTypeByCode(unitIdCode);
		Validate.notNull(itemTypeWrapper);
		descItemType = itemTypeWrapper.getEntity();

		// read level type
		String levelTypeCode = config.getLevelTypeCode();
		Validate.notNull(levelTypeCode);
		ItemType levelTypeWrapper = staticDataProvider.getItemTypeByCode(levelTypeCode);
		Validate.notNull(levelTypeWrapper);
		descItemLevelType = levelTypeWrapper.getEntity();

		// item for previous value
		String previousIdCode = config.getPreviousIdCode();
		Validate.notNull(previousIdCode);
		ItemType previousIdTypeWrapper = staticDataProvider.getItemTypeByCode(previousIdCode);
		Validate.notNull(previousIdTypeWrapper);
        // check that data type is string - ArrDataString
        Validate.isTrue(previousIdTypeWrapper.getDataType() == DataType.STRING);

		descItemPreviousType = previousIdTypeWrapper.getEntity();

		String previousIdSpecCode = config.getPreviousIdSpecCode();
		Validate.notNull(previousIdSpecCode);
		descItemPreviousSpec = previousIdTypeWrapper.getItemSpecByCode(previousIdSpecCode);
		Validate.notNull(descItemPreviousSpec);
		
		String extraLevelSpecCode = config.getExtraDelimiterAfter();
		Validate.notNull(extraLevelSpecCode);
        extraSlashLevelSpec = levelTypeWrapper.getItemSpecByCode(extraLevelSpecCode);
        Validate.notNull(extraSlashLevelSpec);

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

    private SealedUnitIdTree buildUsedIdTree() {
        ArrFund fund = getFundVersion().getFund();

        List<String> usedValues = usedValueRepository.findByFundAndItemType(fund, descItemType);

        SealedUnitIdTree sealedTree = new SealedUnitIdTree();
        if (usedValues != null) {
            for (String uv : usedValues) {
                try {
                    sealedTree.addSealedValue(uv);
                } catch (UnitIdException e) {
                    throw new SystemException("Incorrect value in used value repository", BaseCode.INVALID_STATE)
                            .set("value", uv);
                }
            }
        }

        return sealedTree;
    }

	@Override
	public void run(ActionRunContext runContext) {
        //ArrNode rootNode = version.getRootNode();

        this.sealedUnitIdTree = buildUsedIdTree();

        UnitIdGeneratorParams params = new UnitIdGeneratorParams(this, descItemType,
                descItemLevelType, sealedUnitIdTree, this.extraSlashLevelSpec,
                this.descItemPreviousType, this.descItemPreviousSpec);

        // Počet změněných položek.
        int countChanges = 0;

        for (Integer nodeId : runContext.getInputNodeIds()) {
            ArrNode nodeRef = nodeRepository.getOne(nodeId);
            ArrLevel level = levelRepository.findByNodeAndDeleteChangeIsNull(nodeRef);
			Validate.notNull(level);

            UnitIdGenerator generator = appCtx.getBean(UnitIdGenerator.class, level, params);
            generator.run();

            countChanges += generator.getCountChanges();
        }

        Result resultBA = new Result();
        UnitIdResult result = new UnitIdResult();
        result.setCountChanges(countChanges);
        resultBA.getResults().add(result);
        bulkActionRun.setResult(resultBA);
    }

    @Override
	public String getName() {
		return "UnitIdBulkAction";
    }
}
