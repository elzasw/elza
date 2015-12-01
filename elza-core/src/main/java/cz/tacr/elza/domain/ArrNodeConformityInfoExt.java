package cz.tacr.elza.domain;

import java.util.List;


/**
 * Rozšíření {@link cz.tacr.elza.domain.ArrNodeConformityInfo} o seznam chybových zpráv.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 26.11.2015
 */
public class ArrNodeConformityInfoExt extends ArrNodeConformityInfo
        implements cz.tacr.elza.api.ArrNodeConformityInfoExt<ArrNode, ArrFindingAidVersion, ArrNodeConformityMissing, ArrNodeConformityErrors> {

    /**
     * Seznam chybějících hodnot.
     */
    private List<ArrNodeConformityMissing> missingList;

    /**
     * Seznam chybějících hodnot.
     */
    private List<ArrNodeConformityErrors> errorList;


    /**
     * @return Seznam chybějících hodnot.
     */
    @Override
    public List<ArrNodeConformityMissing> getMissingList() {
        return missingList;
    }

    /**
     * @param missingList Seznam chybějících hodnot.
     */
    @Override
    public void setMissingList(final List<ArrNodeConformityMissing> missingList) {
        this.missingList = missingList;
    }

    /**
     * @return Seznam chybějících hodnot.
     */
    public List<ArrNodeConformityErrors> getErrorList() {
        return errorList;
    }

    /**
     * @param errorList Seznam špatně zadaných hodnot Seznam chybějících hodnot.
     */
    public void setErrorList(final List<ArrNodeConformityErrors> errorList) {
        this.errorList = errorList;
    }
}
