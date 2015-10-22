package cz.tacr.elza.generator;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;


/**
 * Generátor prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy - nastavení ZP2015_SERIAL_NUMBER.
 *
 * @author Martin Šlapa
 * @since 21.10.2015
 */
@Component
public class SerialNumberGenerator extends Generator implements InitializingBean {

    public static final String DESC_ITEM_TYPE = "ZP2015_SERIAL_NUMBER";

    private RulDescItemType descItemType = null;

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
        SerialNumber serialNumber = new SerialNumber();
        ArrChange change = createChange();
        generate(Type.REBUILD, version.getRootLevel(), version, change, serialNumber);
    }

    @Override
    @Transactional
    public void clean(ArrFindingAidVersion version) {
        init();
        checkVersion(version);
        ArrChange change = createChange();
        generate(Type.CLEAN, version.getRootLevel(), version, change, null);
    }

    /**
     * Inicializace komponenty.
     */
    private void init() {
        if (descItemType == null) {
            descItemType = descItemTypeRepository.getOneByCode(DESC_ITEM_TYPE);
            Assert.notNull(descItemType);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *
     * @param type         typ akce
     * @param level        uzel
     * @param version      verze archivní pomůcky
     * @param change       změna
     * @param serialNumber generátor pořadových čísel
     */
    private void generate(Type type, final ArrLevel level, final ArrFindingAidVersion version, final ArrChange change, final SerialNumber serialNumber) {

        ArrDescItem descItem = loadDescItem(level);

        switch (type) {
            case REBUILD:
                int sn = serialNumber.getNext();

                // vytvoření nového atributu
                if (descItem == null) {
                    descItem = new ArrDescItemInt();
                    descItem.setDescItemType(descItemType);
                    descItem.setNode(level.getNode());
                }

                if (!(descItem instanceof ArrDescItemInt)) {
                    throw new IllegalStateException(DESC_ITEM_TYPE + " není typu ArrDescItemInt");
                }

                // uložit pouze při rozdílu
                if (((ArrDescItemInt) descItem).getValue() == null || sn != ((ArrDescItemInt) descItem).getValue()) {
                    ((ArrDescItemInt) descItem).setValue(sn);
                    ArrDescItem ret = saveDescItem(descItem, version, change);
                    level.setNode(ret.getNode());
                }

                break;

            case CLEAN:
                // pokud existuje smazat
                if (descItem != null) {
                    ArrDescItem ret = deleteDescItem(descItem, version, change);
                    level.setNode(ret.getNode()); // zpětné nastavení kvůli optimistickým zámkům
                }
                break;

            default:
                throw new IllegalStateException("Nedefinovaný typ generování");
        }

        List<ArrLevel> childLevels = getChildren(level);

        for (ArrLevel childLevel : childLevels) {
            generate(type, childLevel, version, change, serialNumber);
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
            throw new IllegalStateException(DESC_ITEM_TYPE + " nemuže být více než jeden (" + descItems.size() + ")");
        }
        return descItemFactory.getDescItem(descItems.get(0));
    }

    /**
     * Generátor pořadových čísel.
     */
    private class SerialNumber {

        private int i;

        public SerialNumber() {
            this.i = 0;
        }

        public int getNext() {
            return ++i;
        }
    }

}