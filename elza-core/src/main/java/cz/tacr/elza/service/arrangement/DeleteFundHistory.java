package cz.tacr.elza.service.arrangement;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.repository.vo.ItemChange;

import java.util.Collection;
import java.util.List;

/**
 * Pomocný interface pro potřebné metody na repository pro mazání historie AS.
 */
public interface DeleteFundHistory {

    /**
     * Získání všech položek se změnami pro AS.
     *
     * @param fund archivní soubor, pro který hledáme itemy změn
     * @return seznam položek (identifikátor objektu + identifikátor změny)
     */
    List<ItemChange> findByFund(ArrFund fund);

    /**
     * Aktualizace change u požadovaných itemů.
     *
     * @param ids    identifikátory itemů, u kterých měníme change
     * @param change nová nastavovaná change
     */
    void updateCreateChange(Collection<Integer> ids, ArrChange change);

}
