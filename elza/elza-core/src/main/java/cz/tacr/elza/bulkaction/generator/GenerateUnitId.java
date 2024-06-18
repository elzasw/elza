package cz.tacr.elza.bulkaction.generator;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.bulkaction.generator.result.UnitIdResult;
import cz.tacr.elza.bulkaction.generator.unitid.SealedUnitIdTree;
import cz.tacr.elza.bulkaction.generator.unitid.UnitIdException;
import cz.tacr.elza.bulkaction.generator.unitid.UnitIdGenerator;
import cz.tacr.elza.bulkaction.generator.unitid.UnitIdGeneratorParams;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrLockedValue;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.LockedValueRepository;

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
    LockedValueRepository usedValueRepository;

	protected final GenerateUnitIdConfig config;

    private SealedUnitIdTree sealedUnitIdTree;

    private RulItemSpec extraSlashLevelSpec;

	public GenerateUnitId(GenerateUnitIdConfig unitIdConfig) {
		Objects.requireNonNull(unitIdConfig);
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
		Objects.requireNonNull(unitIdCode);
		ItemType itemTypeWrapper = staticDataProvider.getItemTypeByCode(unitIdCode);
		Objects.requireNonNull(itemTypeWrapper);
		descItemType = itemTypeWrapper.getEntity();

		// read level type
		String levelTypeCode = config.getLevelTypeCode();
		Objects.requireNonNull(levelTypeCode);
		ItemType levelTypeWrapper = staticDataProvider.getItemTypeByCode(levelTypeCode);
		Objects.requireNonNull(levelTypeWrapper);
		descItemLevelType = levelTypeWrapper.getEntity();

		// item for previous value
		String previousIdCode = config.getPreviousIdCode();
		Objects.requireNonNull(previousIdCode);
		ItemType previousIdTypeWrapper = staticDataProvider.getItemTypeByCode(previousIdCode);
		Objects.requireNonNull(previousIdTypeWrapper);
        // check that data type is string - ArrDataString
        Validate.isTrue(previousIdTypeWrapper.getDataType() == DataType.STRING);

		descItemPreviousType = previousIdTypeWrapper.getEntity();

		String previousIdSpecCode = config.getPreviousIdSpecCode();
		Objects.requireNonNull(previousIdSpecCode);
		descItemPreviousSpec = previousIdTypeWrapper.getItemSpecByCode(previousIdSpecCode);
		Objects.requireNonNull(descItemPreviousSpec);

		String extraLevelSpecCode = config.getExtraDelimiterAfter();
		Objects.requireNonNull(extraLevelSpecCode);
        extraSlashLevelSpec = levelTypeWrapper.getItemSpecByCode(extraLevelSpecCode);
        Objects.requireNonNull(extraSlashLevelSpec);

    }

    private SealedUnitIdTree buildUsedIdTree() {
        ArrFund fund = getFundVersion().getFund();

        List<ArrLockedValue> lockedItems = findByFundAndItemType(fund, descItemType);

        SealedUnitIdTree sealedTree = new SealedUnitIdTree();
        if (lockedItems != null) {
            for (ArrLockedValue uv : lockedItems) {
                try {
                    ArrItem item = uv.getItem();
                    ArrData data = HibernateUtils.unproxy(item.getData());
                    ArrDataUnitid unitId = HibernateUtils.unproxy(data);
                    String value = unitId.getUnitId();

                    sealedTree.addSealedValue(value, (input) -> {
                        // validate is input is same with original object
                        ArrDescItem locked = HibernateUtils.unproxy(input);
                        if (!uv.getItemId().equals(locked.getItemId())) {
                            throw new SystemException("Previously sealed value was found with different node",
                                    BaseCode.INVALID_STATE)
                                            .set("value", value)
                                            .set("originalItemId", uv.getItemId())
                                            .set("otherItemId", locked.getItemId());
                        }
                    });
                } catch (UnitIdException e) {
                    throw new SystemException("Incorrect value in used value repository", BaseCode.INVALID_STATE)
                            .set("value", uv);
                }
            }
        }

        return sealedTree;
    }

    private List<ArrLockedValue> findByFundAndItemType(ArrFund fund, RulItemType itemType) {
        return usedValueRepository.findByFundAndItemType(fund, itemType);
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
            Objects.requireNonNull(level);

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
