package cz.tacr.elza.validation;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.vo.DataValidationResult;

import java.util.List;


/**
 * Validátor povinnosti, opakovatelnosti, hodnot atd pro atributy. Validace probíhá až po uložení všech hodnot.
 * Neslouží
 * k validaci při ukládání jedné hodnoty.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 30.11.2015
 */
public interface ArrDescItemsPostValidator {

    /**
     * Provede validaci uložených atributů uzlu v dané verzi. Validace probíhá až po uložení všech hodnot. Neslouží k
     * validaci při ukládání jedné hodnoty.
     *
     * @param level   uzel
     * @param version verze
     */
    List<DataValidationResult> postValidateNodeDescItems(ArrLevel level, ArrFundVersion version);
}
