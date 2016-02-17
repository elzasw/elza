package cz.tacr.elza.controller;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVersionVO;
import cz.tacr.elza.controller.vo.RulArrangementTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;


/**
 * Testování metod z AdminController.
 *
 * @author Martin Šlapa
 * @since 16.2.2016
 */
public class ArrangementControllerUsecaseTest extends AbstractControllerTest {

    public static final Logger logger = LoggerFactory.getLogger(ArrangementControllerUsecaseTest.class);

    public static final String NAME_AP = "UseCase";
    public static final String RENAME_AP = "Renamed UseCase";

    @Test
    public void usecaseTest() {

        // vytvoření
        ArrFindingAidVO findingAid = createdFindingAid();

        // přejmenování
        findingAid = updatedFindingAid(findingAid);

        ArrFindingAidVersionVO findingAidVersion = getOpenVersion(findingAid);

        // uzavření verze
        findingAidVersion = approvedVersion(findingAidVersion);

        // vytvoření uzlů
        // TODO

        // přesunutí && smazání uzlů
        // TODO

        // atributy
        // TODO

        // vazba na rejstříky
        // TODO

    }

    private ArrFindingAidVersionVO approvedVersion(final ArrFindingAidVersionVO findingAidVersion) {
        Assert.notNull(findingAidVersion);
        List<RulRuleSetVO> ruleSets = getRuleSets();
        RulArrangementTypeVO arrangementType = ruleSets.get(0).getArrangementTypes().get(1);
        ArrFindingAidVersionVO newFindingAidVersion = approveVersion(findingAidVersion, arrangementType);

        Assert.isTrue(!findingAidVersion.getId().equals(newFindingAidVersion.getId()),
                "Musí být odlišné identifikátory");
        Assert.isTrue(!findingAidVersion.getArrangementType().getId().equals(
                newFindingAidVersion.getArrangementType().getId()), "Musí být odlišné typy výstupu");

        return newFindingAidVersion;
    }

    private ArrFindingAidVersionVO getOpenVersion(final ArrFindingAidVO findingAid) {
        Assert.notNull(findingAid);

        List<ArrFindingAidVO> findingAids = getFindingAids();

        for (ArrFindingAidVO findingAidFound : findingAids) {
            if (findingAidFound.getId().equals(findingAid.getId())) {
                for (ArrFindingAidVersionVO findingAidVersion : findingAidFound.getVersions()) {
                    if (findingAidVersion.getLockDate() == null) {
                        return findingAidVersion;
                    }
                }
            }
        }

        return null;
    }

    private ArrFindingAidVO updatedFindingAid(final ArrFindingAidVO findingAid) {
        findingAid.setName(RENAME_AP);
        ArrFindingAidVO updatedFindingAid = updateFindingAid(findingAid);
        Assert.isTrue(RENAME_AP.equals(updatedFindingAid.getName()), "Jméno AP musí být stejné");
        return updatedFindingAid;
    }

    private ArrFindingAidVO createdFindingAid() {
        ArrFindingAidVO findingAid = createFindingAid(NAME_AP);
        Assert.notNull(findingAid);
        return findingAid;
    }

}
