package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.vo.ArrFundOpenVersion;


/**
 * Rozšiřující rozhraní pro archivní fondy.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.04.2016
 */
public interface FundRepositoryCustom {

    /**
     * Najde všechny archivní fondy podle fulltextu (název a interní číslo)
     *
     * @param fulltext fulltext
     * @param max      max počet záznamů
     * @param readAllFunds vyhledávat ve všech AS?
     * @param user uživatel, kterému se data vyhledávají
     * @return archivní fond s otevřenou verzí
     */
    List<ArrFundOpenVersion> findByFulltext(String fulltext, int max, final boolean readAllFunds, final UsrUser user);


    /**
     * Najde počet všech archivních fondů podle fulltextu.
     *
     * @param fulltext fulltext (název a interní číslo)
     * @param readAllFunds vyhledávat ve všech AS?
     * @param user uživatel, kterému se data vyhledávají
     * @return počet archivních fondů splňujících podmínky fulltextu
     */
    Integer findCountByFulltext(String fulltext, final boolean readAllFunds, final UsrUser user);

    /**
     * Vyhledá AS na které jsou vázaná nějaká oprávnění.
     *
     * @param search      hledané řetězec
     * @param firstResult od jakého záznamu
     * @param maxResults  maximální počet vrácených záznamů
     * @param userId      identifikátor uživatele, podle kterého filtrujeme (pokud je null, nefiltrujeme)
     * @return výsledek
     */
    FilteredResult<ArrFund> findFundsWithPermissions(String search, Integer firstResult, Integer maxResults, final Integer userId);
}
