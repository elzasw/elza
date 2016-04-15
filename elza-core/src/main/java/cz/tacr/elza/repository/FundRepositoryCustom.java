package cz.tacr.elza.repository;

import java.util.List;

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
     * @return archivní fond s otevřenou verzí
     */
    List<ArrFundOpenVersion> findByFulltext(String fulltext, int max);


    /**
     * Najde počet všech archivních fondů podle fulltextu.
     *
     * @param fulltext fulltext (název a interní číslo)
     * @return počet archivních fondů splňujících podmínky fulltextu
     */
    Integer findCountByFulltext(String fulltext);

}
