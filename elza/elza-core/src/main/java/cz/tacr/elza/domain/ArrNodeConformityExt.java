package cz.tacr.elza.domain;

import java.util.List;


/**
 * Rozšíření {@link ArrNodeConformity} o seznam chybových zpráv.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 26.11.2015
 */
public class ArrNodeConformityExt extends ArrNodeConformity {

    /**
     * Seznam chybějících hodnot.
     */
    private List<ArrNodeConformityMissing> missingList;

    /**
     * Seznam chybějících hodnot.
     */
    private List<ArrNodeConformityError> errorList;


    /**
     * @return Seznam chybějících hodnot.
     */
    public List<ArrNodeConformityMissing> getMissingList() {
        return missingList;
    }

    /**
     * @param missingList Seznam chybějících hodnot.
     */
    public void setMissingList(final List<ArrNodeConformityMissing> missingList) {
        this.missingList = missingList;
    }

    /**
     * @return Seznam špatně zadaných hodnot
     */
    public List<ArrNodeConformityError> getErrorList() {
        return errorList;
    }

    /**
     * @param errorList Seznam špatně zadaných hodnot Seznam chybějících hodnot.
     */
    public void setErrorList(final List<ArrNodeConformityError> errorList) {
        this.errorList = errorList;
    }
}
