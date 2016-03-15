package cz.tacr.elza.bulkaction.generator;

import java.time.LocalDateTime;
import java.util.List;

import cz.tacr.elza.repository.LevelRepositoryCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionState;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.DescriptionItemService;


/**
 * Abstraktní třída pro tvorbu hromadných akcí.
 *
 * @author Martin Šlapa
 * @since 21.10.2015
 */
public abstract class BulkAction {

    @Autowired
    protected FundVersionRepository fundVersionRepository;

    @Autowired
    private ChangeRepository changeRepository;

    @Autowired
    protected LevelRepository levelRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    /**
     * Abstrakní metoda pro spuštění hromadné akce.
     *
     * @param fundVersionId      identifikátor verze archivní pomůcky
     * @param bulkActionConfig nastavení hromadné akce
     * @param bulkActionState  stav hromadné akce
     */
    abstract public void run(final Integer fundVersionId,
                             final BulkActionConfig bulkActionConfig,
                             final BulkActionState bulkActionState);


    @Override
    abstract public String toString();

    /**
     * Vytvoření nové změny.
     *
     * @return vytvořená změna
     */
    protected ArrChange createChange() {
        ArrChange change = new ArrChange();
        change.setChangeDate(LocalDateTime.now());
        return changeRepository.save(change);
    }

    /**
     * Uložení nového/existující atributu.
     *
     * @param descItem ukládaný atribut
     * @param version  verze archivní pomůcky
     * @param change   změna
     * @return finální atribut
     */
    protected ArrDescItem saveDescItem(final ArrDescItem descItem,
                                       final ArrFundVersion version,
                                       final ArrChange change) {
        if (descItem.getDescItemObjectId() == null) {
            return descriptionItemService.createDescriptionItem(descItem, descItem.getNode(), version, change);
        } else {
            return descriptionItemService.updateDescriptionItem(descItem, version, change, true);
        }
    }

    /**
     * Smazání existujícího atributu.
     *
     * @param descItem atribut ke smazání
     * @param version  verze archivní pomůcky
     * @param change   změna
     * @return finální atribut
     */
    protected ArrDescItem deleteDescItem(final ArrDescItem descItem,
                                         final ArrFundVersion version,
                                         final ArrChange change) {
        return descriptionItemService.deleteDescriptionItem(descItem, version, change, true);
    }


    /**
     * Vyhledá potomky uzlu.
     *
     * @param level rodičovský uzel
     * @return nalezený potomci
     */
    protected List<ArrLevel> getChildren(final ArrLevel level) {
        return levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(level.getNode());
    }

    /**
     * Kontrola verze.
     *
     * @param version verze archivní pomůcky
     */
    protected void checkVersion(ArrFundVersion version) {
        Assert.notNull(version);
        if (version.getLockChange() != null) {
            throw new IllegalStateException("Nelze aplikovat na uzavřenou verzi archivní pomůcky");
        }
    }
}