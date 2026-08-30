package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;

/**
 * "Vícenásobné napojení" on the path of the digital archive.
 *
 * The rule was implemented twice before this - for the legacy DAO and for a filesystem repository -
 * and not a third time when the digital archive was added, so an AIP could be attached to any number
 * of units of description whatever the repository said. These pin the behaviour that replaced it,
 * against a database, because attaching is only meaningful once the links are really stored.
 */
public class DaServiceLinkTest extends AbstractServiceTest {

    @Autowired
    private DaService daService;
    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;
    @Autowired
    private FundLevelService fundLevelService;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    /**
     * The shared cleanup of the base class does not know the tables of the digital archive, and the
     * rows created here would keep the next test from deleting the external systems they point at.
     */
    @AfterEach
    public void deleteCreatedRows() {
        tx().executeWithoutResult(t -> {
            daLinkRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    private ArrDigitalRepository createRepository(final boolean multipleLinks) {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-LINK-TEST");
        repository.setName("Testovaci digitalni archiv");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setSendNotification(false);
        repository.setMultipleLinks(multipleLinks);
        return digitalRepositoryRepository.save(repository);
    }

    private DaAip createAip(final ArrDigitalRepository repository) {
        DaAip aip = new DaAip();
        aip.setCode("aip-link-test");
        aip.setDigitalRepository(repository);
        return aipRepository.save(aip);
    }

    /** A second unit of description under the root, to attach the same AIP to twice. */
    private Integer secondNodeId(final FundInfo fund) {
        return tx().execute(t -> {
            ArrNode parent = nodeRepository.findById(fund.getRootNodeId()).orElseThrow();
            List<ArrLevel> levels = fundLevelService.addNewLevel(fund.getFundVersion(), parent, parent,
                    AddLevelDirection.CHILD, null, null, null, null, null);
            return levels.stream().max(Comparator.comparing(ArrLevel::getLevelId)).orElseThrow()
                    .getNode().getNodeId();
        });
    }

    @Test
    public void attachingTwiceToTheSameNodeCreatesOneLink() {
        FundInfo fund = tx().execute(t -> createFund("F-da-link-same-node"));
        Integer aipId = tx().execute(t -> createAip(createRepository(false)).getAipId());

        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));
        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));

        tx().executeWithoutResult(t -> assertEquals(1,
                daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).size(),
                "attaching where it already hangs must not create a second link"));
    }

    @Test
    public void aSecondNodeIsRefusedWhenMultipleLinksIsOff() {
        FundInfo fund = tx().execute(t -> createFund("F-da-link-refused"));
        Integer aipId = tx().execute(t -> createAip(createRepository(false)).getAipId());
        Integer otherNodeId = secondNodeId(fund);

        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));

        BusinessException e = assertThrows(BusinessException.class,
                () -> tx().executeWithoutResult(t -> daService.connectToJP(otherNodeId, aipId)));
        assertEquals(ArrangementCode.DAO_ALREADY_LINKED, e.getErrorCode());

        tx().executeWithoutResult(t -> assertEquals(1,
                daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).size(),
                "the refused attempt must leave the AIP on the one node it had"));
    }

    @Test
    public void aSecondNodeIsAllowedWhenMultipleLinksIsOn() {
        FundInfo fund = tx().execute(t -> createFund("F-da-link-allowed"));
        Integer aipId = tx().execute(t -> createAip(createRepository(true)).getAipId());
        Integer otherNodeId = secondNodeId(fund);

        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));
        tx().executeWithoutResult(t -> daService.connectToJP(otherNodeId, aipId));

        tx().executeWithoutResult(t -> assertEquals(2,
                daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).size()));
    }

    /** The bulk action goes through the same guard as attaching one AIP. */
    @Test
    public void bulkAttachingIsRefusedForAnAipThatAlreadyHangsElsewhere() {
        FundInfo fund = tx().execute(t -> createFund("F-da-link-bulk"));
        Integer aipId = tx().execute(t -> createAip(createRepository(false)).getAipId());
        Integer otherNodeId = secondNodeId(fund);

        tx().executeWithoutResult(t -> daService.connectToJP(fund.getRootNodeId(), aipId));

        BusinessException e = assertThrows(BusinessException.class,
                () -> tx().executeWithoutResult(t -> daService.bulkConnectToJP(otherNodeId, List.of(aipId))));
        assertEquals(ArrangementCode.DAO_ALREADY_LINKED, e.getErrorCode());
    }
}
