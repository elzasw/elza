package cz.tacr.elza.generator;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemUnitid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;


/**
 * Generátor prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy - nastavení ZP2015_UNIT_ID.
 *
 * @author Martin Šlapa
 * @since 21.10.2015
 */
@Component
public class UnitIdGenerator extends Generator implements InitializingBean {

    public static final String DESC_ITEM_TYPE = "ZP2015_UNIT_ID";
    public static final String DESC_ITEM_LEVEL_TYPE = "ZP2015_LEVEL_TYPE";

    public static final String SEPARATOR = "/";
    public static final String SEPARATOR_CHANGE = "//";

    private RulDescItemType descItemType;
    private RulDescItemType descItemLevelType;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    @Override
    @Transactional
    public void rebuild(ArrFindingAidVersion version) {
        init();
        checkVersion(version);
        ArrChange change = createChange();
        generate(Type.REBUILD, version.getRootLevel(), version, change, null, null);
    }

    @Override
    @Transactional
    public void clean(ArrFindingAidVersion version) {
        init();
        checkVersion(version);
        ArrChange change = createChange();
        generate(Type.CLEAN, version.getRootLevel(), version, change, null, null);
    }

    /**
     * Inicializace komponenty.
     */
    private void init() {
        if (descItemType == null) {
            descItemType = descItemTypeRepository.getOneByCode(DESC_ITEM_TYPE);
            Assert.notNull(descItemType);
        }
        if (descItemLevelType == null) {
            descItemLevelType = descItemTypeRepository.getOneByCode(DESC_ITEM_LEVEL_TYPE);
            Assert.notNull(descItemLevelType);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *
     * @param type           typ akce
     * @param level          uzel
     * @param version        verze archivní pomůcky
     * @param change         změna
     * @param unitId         generátor pořadových čísel
     * @param parentSpecCode specifický kód rodiče
     */
    private void generate(Type type, final ArrLevel level, final ArrFindingAidVersion version, final ArrChange change, UnitId unitId, final String parentSpecCode) {

        ArrDescItem descItem = loadDescItem(level);
        ArrDescItem descItemLevel = loadDescItemLevel(level);

        switch (type) {
            case REBUILD:

                if (unitId == null) {
                    unitId = new UnitId(1);
                } else {
                    String specCode = descItemLevel == null ? null : descItemLevel.getDescItemSpec().getCode();

                    if ((specCode == null && parentSpecCode == null)
                            || (specCode != null && specCode.equals(parentSpecCode))
                            || (parentSpecCode != null && parentSpecCode.equals(specCode))) {
                        unitId.setSeparator(SEPARATOR);
                    } else {
                        unitId.setSeparator(SEPARATOR_CHANGE);
                    }

                    unitId.genNext();
                }

                // vytvoření nového atributu
                if (descItem == null) {
                    descItem = new ArrDescItemUnitid();
                    descItem.setDescItemType(descItemTypeRepository.getOneByCode(DESC_ITEM_TYPE));
                    descItem.setNode(level.getNode());
                }

                if (!(descItem instanceof ArrDescItemUnitid)) {
                    throw new IllegalStateException(DESC_ITEM_TYPE + " neni typu ArrDescItemUnitid");
                }

                // uložit pouze při rozdílu
                if (((ArrDescItemUnitid) descItem).getValue() == null || !unitId.getData().equals(((ArrDescItemUnitid) descItem).getValue())) {
                    ((ArrDescItemUnitid) descItem).setValue(unitId.getData());
                    ArrDescItem ret = saveDescItem(descItem, version, change);
                    level.setNode(ret.getNode());
                }

                break;

            case CLEAN:
                // pokud existuje smazat
                if (descItem != null) {
                    deleteDescItem(descItem, version, change);
                }
                break;

            default:
                throw new IllegalStateException("Nedefinovaný typ generování");
        }

        List<ArrLevel> childLevels = getChildren(level);

        UnitId unitIdChild = null;
        for (ArrLevel childLevel : childLevels) {
            if (unitId != null && unitIdChild == null) {
                unitIdChild = unitId.getClone();
            }
            generate(type, childLevel, version, change, unitIdChild, descItemLevel == null ? null : descItemLevel.getDescItemSpec().getCode());
        }

    }

    /**
     * Načtení atributu.
     *
     * @param level uzel
     * @return nalezený atribut
     */
    private ArrDescItem loadDescItem(final ArrLevel level) {
        List<ArrDescItem> descItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeId(level.getNode(), descItemType.getDescItemTypeId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new IllegalStateException(DESC_ITEM_TYPE + " nemuze byt vice nez jeden (" + descItems.size() + ")");
        }
        return descItemFactory.getDescItem(descItems.get(0));
    }

    /**
     * Načtení atributu - level.
     *
     * @param level uzel
     * @return nalezený atribut
     */
    private ArrDescItem loadDescItemLevel(final ArrLevel level) {
        List<ArrDescItem> descItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeId(level.getNode(), descItemLevelType.getDescItemTypeId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new IllegalStateException(DESC_ITEM_TYPE + " nemuze byt vice nez jeden (" + descItems.size() + ")");
        }
        return descItemFactory.getDescItem(descItems.get(0));
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
            return this.data + separator + id.toString();
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

}