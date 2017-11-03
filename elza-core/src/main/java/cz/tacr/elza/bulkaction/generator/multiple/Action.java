package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;

/**
 * Akce pro vícenásobnou hromadnou akci.
 *
 */
public abstract class Action {

	@Autowired
	protected ItemTypeRepository itemTypeRepository;

	@Autowired
	protected ItemSpecRepository itemSpecRepository;

	@Autowired
	protected StaticDataService staticDataService;

    /**
	 * Inicializace akce.
	 * 
	 * @param runContext
	 */
	abstract public void init(ArrBulkActionRun bulkActionRun);

    /**
     * Aplikování akce na uzel.
     *
     * @param node
     * @param items                 hodnoty atributy uzlu
     * @param parentLevelWithItems   hodnoty atributu nadřízených uzlů
     */
	abstract public void apply(LevelWithItems level, TypeLevel typeLevel);

    /**
     * Nashromážděný výsledek akce.
     *
     * @return výsledek akce, je serializovan do JSON pro uložení
     */
    abstract public ActionResult getResult();

	/**
	 * Return rule system for given context
	 * 
	 * @return
	 */
	RuleSystem getRuleSystem(ArrBulkActionRun bulkActionRun) {
		ArrFundVersion version = bulkActionRun.getFundVersion();
		StaticDataProvider sdp = staticDataService.getData();
		return sdp.getRuleSystems().getByRuleSetId(version.getRuleSetId());
	}

	/**
	 * Kontrola datového typu atributů
	 *
	 * @param inputItemType
	 *            kontrolované atributy
	 * @param codes
	 *            kódy povolených datových typů
	 */
	protected void checkValidDataType(final RuleSystemItemType itemType, final DataType... codes) {
		DataType dataType = itemType.getDataType();
		List<DataType> codeList = Arrays.asList(codes);
		if (!codeList.contains(dataType)) {
			throw new BusinessException(
			        "Datový typ atributu musí být " + codeList + " (item type " + itemType.getCode() + ")",
			        BaseCode.ID_NOT_EXIST);
		}
	}
}
