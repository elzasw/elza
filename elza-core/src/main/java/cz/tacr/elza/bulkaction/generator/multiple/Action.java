package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulRuleSet;
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
	abstract public void init(ActionRunContext runContext);

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
	RuleSystem getRuleSystem(ActionRunContext runContext) {
		RulRuleSet rrs = runContext.getFundVersion().getRuleSet();
		StaticDataProvider sdp = staticDataService.getData();
		return sdp.getRuleSystems().getByRuleSetId(rrs.getRuleSetId());
	}

    /**
     * Vyhledá typ podle kódu.
     *
     * @param code  kód typu atributu
     * @param param hledaný parametr
     * @return  atribut
     */
    public RulItemType findItemType(final String code, final String param) {
        RulItemType itemType = itemTypeRepository.findOneByCode(code);
        if (itemType == null) {
            throw new BusinessException("Typ atributu neexistuje: " + param + " (" + code + ")", BaseCode.ID_NOT_EXIST);
        }
        return itemType;
    }

    /**
     * Vyhledá specifikaci podle kódu.
     *
     * @param code  kód specifikace atributu
     * @return  specifikace
     */
    public RulItemSpec findItemSpec(final String code) {
        RulItemSpec itemSpec = itemSpecRepository.findOneByCode(code);
        if (itemSpec == null) {
            throw new BusinessException("Typ atributu neexistuje: " + code, BaseCode.ID_NOT_EXIST);
        }
        return itemSpec;
    }

    /**
     * Vyhledá typy podle kódů.
     *
     * @param codes kódy typů atributů
     * @return  atributy
     */
    public Set<RulItemType> findItemTypes(final Set<String> codes) {
        Set<RulItemType> itemTypes = itemTypeRepository.findByCode(codes);
        if (itemTypes.size() != codes.size()) {
            throw new BusinessException("Některý atribut neexistuje -> potřeba: " + codes + ", nalezene:" + itemTypes, BaseCode.ID_NOT_EXIST);
        }
        return itemTypes;
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
