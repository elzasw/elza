package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.utils.Yaml;

/**
 * Akce pro vícenásobnou hromadnou akci.
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
public abstract class Action implements InitializingBean {

    @Autowired
    protected ItemTypeRepository itemTypeRepository;

    @Autowired
    protected ItemSpecRepository itemSpecRepository;

    /**
     * Konfigurace akce.
     */
    protected final Yaml config;

    /**
     * Aplikovat na předky uzlů
     */
    protected boolean applyParents;

    /**
     * Aplikovat na potomky
     */
    protected boolean applyChildren;

    Action(final Yaml config) {
        this.config = config;
        applyParents = config.getBoolean("apply_parents", true);
        applyChildren = config.getBoolean("apply_children", true);
    }

    /**
     * Inicializace akce.
     */
    abstract public void init();

    /**
     * Aplikování akce na uzel.
     *
     * @param node
     * @param items                 hodnoty atributy uzlu
     * @param parentLevelWithItems   hodnoty atributu nadřízených uzlů
     */
    abstract public void apply(final ArrNode node, final List<ArrDescItem> items, final LevelWithItems parentLevelWithItems);

    /**
     * Má se vykonat aplikování?
     *
     * @param typeLevel typ levelu
     * @return  aplikovat?
     */
    abstract public boolean canApply(final TypeLevel typeLevel);

    /**
     * Nashromážděný výsledek akce.
     *
     * @return výsledek akce, je serializovan do JSON pro uložení
     */
    abstract public ActionResult getResult();

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
            throw new IllegalArgumentException("Typ atributu neexistuje: " + param + " (" + code + ")");
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
            throw new IllegalArgumentException("Typ atributu neexistuje: " + code);
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
            throw new IllegalArgumentException("Některý atribut neexistuje -> potřeba: " + codes + ", nalezene:" + itemTypes);
        }
        return itemTypes;
    }

    /**
     * Kontrola datového typu atributů
     *
     * @param inputItemType kontrolované atributy
     * @param codes kódy povolených datových typů
     */
    protected void checkValidDataType(final RulItemType inputItemType, final String ...codes) {
        RulDataType dataType = inputItemType.getDataType();
        List<String> codeList = Arrays.asList(codes);
        if (!codeList.contains(dataType.getCode())) {
            throw new IllegalArgumentException("Datový typ atributu musí být " + codeList + " (item type " + inputItemType.getCode() + ")");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }
}
