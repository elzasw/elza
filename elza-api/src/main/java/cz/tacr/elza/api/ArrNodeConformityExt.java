package cz.tacr.elza.api;

import java.util.List;


/**
 * Rozšíření {@link ArrNodeConformity} o seznam chybových zpráv.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 26.11.2015
 */
public interface ArrNodeConformityExt<AN extends ArrNode, AFAV extends ArrFundVersion, MIS extends ArrNodeConformityMissing, ERR extends ArrNodeConformityError> extends
                                                                                                                                                                        ArrNodeConformity<AN, AFAV> {

    /**
     * @return Seznam chybějících hodnot.
     */
    List<MIS> getMissingList();


    /**
     * @param missingList Seznam chybějících hodnot.
     */
    void setMissingList(List<MIS> missingList);


    /**
     * @return Seznam špatně zadaných hodnot
     */
    List<ERR> getErrorList();


    /**
     * @param errorList Seznam špatně zadaných hodnot
     */
    void setErrorList(List<ERR> errorList);
}
