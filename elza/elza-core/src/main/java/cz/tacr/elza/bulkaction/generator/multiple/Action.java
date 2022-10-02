package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
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

    @Autowired
    protected ApplicationContext appCtx;

    /**
     * Parent bulk action
     */
    protected BulkAction bulkAction;

    /**
     * Inicializace akce.
     *
     * @param bulkAction
     *            Master bulk action
     * @param bulkActionRun
     */
    public void init(BulkAction bulkAction, ArrBulkActionRun bulkActionRun) {
        this.bulkAction = bulkAction;
    }

    /**
     * Aplikování akce na uzel.
     *
     * @param level
     * @param typeLevel
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
	protected StaticDataProvider getStaticDataProvider() {
		return staticDataService.getData();
	}

	/**
	 * Kontrola datového typu atributů
	 *
	 * @param itemType
	 *            kontrolované atributy
	 * @param codes
	 *            kódy povolených datových typů
	 */
	protected void checkValidDataType(final ItemType itemType, final DataType... codes) {
		DataType dataType = itemType.getDataType();
		List<DataType> codeList = Arrays.asList(codes);
		if (!codeList.contains(dataType)) {
			throw new BusinessException(
			        "Datový typ atributu musí být " + codeList + " (item type " + itemType.getCode() + ")",
			        BaseCode.ID_NOT_EXIST);
		}
	}

    /**
     * Check if subLevel has given parent
     * 
     * @param parent
     *            Expected parent
     * @param subLevel
     *            level to be checked
     * @return true if subLevel is sub-level or equal with parent node.
     */
    static protected boolean isInTree(LevelWithItems parent, LevelWithItems subLevel) {
    	if (parent == subLevel) {
    		return true;
    	}
    	LevelWithItems level = subLevel.getParent();
    	if (level == null) {
    		return false;
    	}
    	return isInTree(parent, level);
    }
}
