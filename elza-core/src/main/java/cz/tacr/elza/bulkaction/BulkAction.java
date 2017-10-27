package cz.tacr.elza.bulkaction;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
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
    protected LevelRepository levelRepository;

    @Autowired
    protected NodeRepository nodeRepository;

    @Autowired
    protected DescriptionItemService descriptionItemService;

    /**
     * Abstrakní metoda pro spuštění hromadné akce.
     *
     * @param inputNodeIds      seznam vstupních uzlů (podstromů AS)
     * @param bulkActionConfig  nastavení hromadné akce
     * @param bulkActionRun     stav hromadné akce
     */
	abstract public void run(ActionRunContext runContext);

	/**
	 * Return name of bulkaction
	 * 
	 * Value is used to log result, etc.
	 * 
	 * @return
	 */
	abstract public String getName();

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
    /*protected ArrDescItem deleteDescItem(final ArrDescItem descItem,
                                         final ArrFundVersion version,
                                         final ArrChange change) {
        return descriptionItemService.deleteDescriptionItem(descItem, version, change, true);
    }*/


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
		Validate.notNull(version);
        if (version.getLockChange() != null) {
            throw new BusinessException("Nelze aplikovat na uzavřenou verzi archivní pomůcky", ArrangementCode.VERSION_ALREADY_CLOSED);
        }
    }
}
