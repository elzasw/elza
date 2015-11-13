package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionState;
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
 * Hromadná akce prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy.
 *
 * @author Martin Šlapa
 * @since 21.10.2015
 */
@Component
@Scope("prototype")
public class UnitIdBulkAction extends BulkAction {

    /**
     * Identifikátor hromadné akce
     */
    public static final String TYPE = "GENERATOR_UNIT_ID";

    /**
     * Verze archivní pomůcky
     */
    private ArrFindingAidVersion version;

    /**
     * Změna
     */
    private ArrChange change;

    /**
     * Typ atributu
     */
    private RulDescItemType descItemType;

    /**
     * Typ atributu levelu
     */
    private RulDescItemType descItemLevelType;

    /**
     * Vedlejší oddělovač
     */
    private String delimiterMinor;

    /**
     * Hlavní oddělovač
     */
    private String delimiterMajor;

    /**
     * Stav hromadné akce
     */
    private BulkActionState bulkActionState;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    /**
     * Inicializace hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    private void init(final BulkActionConfig bulkActionConfig) {

        Assert.notNull(bulkActionConfig);

        String unitIdCode = (String) bulkActionConfig.getProperty("unit_id_code");
        Assert.notNull(unitIdCode);

        if (descItemType == null) {
            descItemType = descItemTypeRepository.getOneByCode(unitIdCode);
            Assert.notNull(descItemType);
        }

        String levelTypeCode = (String) bulkActionConfig.getProperty("level_type_code");
        Assert.notNull(levelTypeCode);

        if (descItemLevelType == null) {
            descItemLevelType = descItemTypeRepository.getOneByCode(levelTypeCode);
            Assert.notNull(descItemLevelType);
        }

        String delimiterMajor = (String) bulkActionConfig.getProperty("delimiter_major");
        Assert.notNull(delimiterMajor);
        this.delimiterMajor = delimiterMajor;

        String delimiterMinor = (String) bulkActionConfig.getProperty("delimiter_minor");
        Assert.notNull(delimiterMinor);
        this.delimiterMinor = delimiterMinor;

    }

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *
     * @param level          uzel
     * @param unitId         generátor pořadových čísel
     * @param parentSpecCode specifický kód rodiče
     */
    private void generate(final ArrLevel level, UnitId unitId, final String parentSpecCode) {

        ArrDescItem descItem = loadDescItem(level);
        ArrDescItem descItemLevel = loadDescItemLevel(level);

        if (unitId == null) {
            unitId = new UnitId(1);
        } else {
            String specCode = descItemLevel == null ? null : descItemLevel.getDescItemSpec().getCode();

            if ((specCode == null && parentSpecCode == null)
                    || (specCode != null && specCode.equals(parentSpecCode))
                    || (parentSpecCode != null && parentSpecCode.equals(specCode))) {
                unitId.setSeparator(delimiterMinor);
            } else {
                unitId.setSeparator(delimiterMajor);
            }

            unitId.genNext();
        }

        // vytvoření nového atributu
        if (descItem == null) {
            descItem = new ArrDescItemUnitid();
            descItem.setDescItemType(descItemType);
            descItem.setNode(level.getNode());
        }

        if (!(descItem instanceof ArrDescItemUnitid)) {
            throw new IllegalStateException(descItemType.getCode() + " neni typu ArrDescItemUnitid");
        }

        // uložit pouze při rozdílu
        if (((ArrDescItemUnitid) descItem).getValue() == null || !unitId.getData()
                .equals(((ArrDescItemUnitid) descItem).getValue())) {
            ((ArrDescItemUnitid) descItem).setValue(unitId.getData());
            ArrDescItem ret = saveDescItem(descItem, version, change);
            level.setNode(ret.getNode());
        }

        List<ArrLevel> childLevels = getChildren(level);

        UnitId unitIdChild = null;
        for (ArrLevel childLevel : childLevels) {
            if (unitId != null && unitIdChild == null) {
                unitIdChild = unitId.getClone();
            }
            generate(childLevel, unitIdChild, descItemLevel == null ? null : descItemLevel.getDescItemSpec().getCode());
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
                .findByNodeAndDeleteChangeIsNullAndDescItemTypeId(level.getNode(), descItemType.getDescItemTypeId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new IllegalStateException(
                    descItemType.getCode() + " nemuze byt vice nez jeden (" + descItems.size() + ")");
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
        List<ArrDescItem> descItems = descItemRepository
                .findByNodeAndDeleteChangeIsNullAndDescItemTypeId(level.getNode(),
                        descItemLevelType.getDescItemTypeId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new IllegalStateException(
                    descItemType.getCode() + " nemuze byt vice nez jeden (" + descItems.size() + ")");
        }
        return descItemFactory.getDescItem(descItems.get(0));
    }


    @Override
    @Transactional
    public void run(final Integer faVersionId,
                    final BulkActionConfig bulkAction,
                    final BulkActionState bulkActionState) {
        this.bulkActionState = bulkActionState;
        init(bulkAction);

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(faVersionId);

        Assert.notNull(version);
        checkVersion(version);
        this.version = version;

        this.change = createChange();
        this.bulkActionState.setRunChange(this.change);

        generate(version.getRootLevel(), null, null);
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

    @Override
    public String toString() {
        return "UnitIdBulkAction{" +
                "change=" + change +
                ", version=" + version +
                '}';
    }
}