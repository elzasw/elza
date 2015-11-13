package cz.tacr.elza.bulkaction.generator;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionState;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;


/**
 * Abstraktní třída pro tvorbu hromadných akcí.
 *
 * @author Martin Šlapa
 * @since 21.10.2015
 */
public abstract class BulkAction {

    @Autowired
    protected FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private ChangeRepository changeRepository;

    @Autowired
    private ArrangementManager arrangementManager;

    @Autowired
    private LevelRepository levelRepository;

    /**
     * Abstrakní metoda pro spuštění hromadné akce.
     *
     * @param faVersionId      identifikátor verze archivní pomůcky
     * @param bulkActionConfig nastavení hromadné akce
     * @param bulkActionState  stav hromadné akce
     */
    abstract public void run(final Integer faVersionId,
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
                                       final ArrFindingAidVersion version,
                                       final ArrChange change) {
        if (descItem.getDescItemObjectId() == null) {
            return arrangementManager.createDescriptionItem(descItem, version, change, true);
        } else {
            return arrangementManager.updateDescriptionItem(descItem, version, true, change);
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
                                         final ArrFindingAidVersion version,
                                         final ArrChange change) {
        return arrangementManager.deleteDescriptionItem(descItem, version, change);
    }


    /**
     * Vyhledá potomky uzlu.
     *
     * @param level rodičovský uzel
     * @return nalezený potomci
     */
    protected List<ArrLevel> getChildren(final ArrLevel level) {
        return levelRepository.findByNodeParentAndDeleteChangeIsNullOrderByPositionAsc(level.getNode());
    }

    /**
     * Kontrola verze.
     *
     * @param version verze archivní pomůcky
     */
    protected void checkVersion(ArrFindingAidVersion version) {
        Assert.notNull(version);
        if (version.getLockChange() != null) {
            throw new IllegalStateException("Nelze aplikovat na uzavřenou verzi archivní pomůcky");
        }
    }
}