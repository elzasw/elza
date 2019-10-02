package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ArrFund;
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
	 * @param fulltext
	 *            fulltext
	 * @param max
	 *            max počet záznamů
	 * @param user
	 *            uživatel, kterému se data vyhledávají, if null all funds are
	 *            returned
	 * @return archivní fond s otevřenou verzí
	 */
	List<ArrFundOpenVersion> findByFulltext(String fulltext, int max, final Integer userId);


    /**
	 * Najde počet všech archivních fondů podle fulltextu.
	 *
	 * @param fulltext
	 *            fulltext (název a interní číslo)
	 * @param user
	 *            uživatel, kterému se data vyhledávají, if null all funds are
	 *            returned
	 * @return počet archivních fondů splňujících podmínky fulltextu
	 */
	Integer findCountByFulltext(String fulltext, final Integer userId);

    /**
     * Vyhledavani AS pres vsecny AS. Zohlednuje opravneni uzivatele k pristupu k AS.
     *
     * @param fulltext nepovinny text - vyhledavani 'LIKE' v {@code name} a {@code internalCode}
     * @param userId ID uzivatele, muze byt null v pripade opravneni typu ALL
     * @return seznam AS
     */
    List<ArrFund> findFundByFulltext(String fulltext, Integer userId);

	FilteredResult<ArrFund> findFunds(String search, int firstResult, int maxResults);

	/**
	 * Vyhledá AS na které jsou vázaná nějaká oprávnění.
	 *
	 * @param search
	 *            hledané řetězec
	 * @param firstResult
	 *            od jakého záznamu
	 * @param maxResults
	 *            maximální počet vrácených záznamů
	 * @param userId
	 *            identifikátor uživatele, podle kterého filtrujeme
	 * @return výsledek
	 */
	FilteredResult<ArrFund> findFundsWithPermissions(String search, int firstResult, int maxResults,
	        final int userId);
}
