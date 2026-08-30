package cz.tacr.elza.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;

/**
 * The rule behind "Vícenásobné napojení", on its own.
 *
 * It is pure decision-making over a list that is handed to it, so it is tested as such; that the
 * digital archive asks it at all is the subject of {@link cz.tacr.elza.service.da.DaServiceLinkTest},
 * which runs against a database.
 */
public class DaoLinkPolicyTest {

    private static final Integer NODE_A = 1;
    private static final Integer NODE_B = 2;

    private final DaoLinkPolicy policy = new DaoLinkPolicy();

    private static ArrDigitalRepository repository(final Boolean multipleLinks) {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setMultipleLinks(multipleLinks);
        return repository;
    }

    private static ArrDaoLink linkOn(final Integer nodeId) {
        ArrNode node = new ArrNode();
        node.setNodeId(nodeId);
        ArrDaLink link = new ArrDaLink();
        link.setNode(node);
        return link;
    }

    @Test
    void anObjectThatHangsNowhereMayBeAttached() {
        assertTrue(policy.checkCanLink(List.of(), NODE_A, repository(false)).isEmpty());
    }

    /** Attaching again where it already hangs changes nothing, whatever the setting says. */
    @Test
    void attachingWhereItAlreadyHangsReturnsTheExistingLink() {
        ArrDaoLink existing = linkOn(NODE_A);

        Optional<ArrDaoLink> result = policy.checkCanLink(List.of(existing), NODE_A, repository(false));

        assertTrue(result.isPresent());
        assertSame(existing, result.get());
    }

    @Test
    void aSecondUnitOfDescriptionIsRefusedWhenTheRepositoryForbidsIt() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> policy.checkCanLink(List.of(linkOn(NODE_A)), NODE_B, repository(false)));

        assertEquals(ArrangementCode.DAO_ALREADY_LINKED, e.getErrorCode());
    }

    /** The column is not null in the database, but an object built in memory can still be. */
    @Test
    void anUnsetSettingCountsAsForbidden() {
        assertThrows(BusinessException.class,
                () -> policy.checkCanLink(List.of(linkOn(NODE_A)), NODE_B, repository(null)));
    }

    @Test
    void aSecondUnitOfDescriptionIsAllowedWhenTheRepositoryPermitsIt() {
        assertTrue(policy.checkCanLink(List.of(linkOn(NODE_A)), NODE_B, repository(true)).isEmpty());
    }

    @Test
    void theExistingLinkIsFoundEvenAmongSeveral() {
        ArrDaoLink wanted = linkOn(NODE_B);

        Optional<ArrDaoLink> result = policy.checkCanLink(List.of(linkOn(NODE_A), wanted), NODE_B,
                                                          repository(true));

        assertTrue(result.isPresent());
        assertSame(wanted, result.get());
    }

    @Test
    void wouldBeRefusedAnswersWithoutRefusing() {
        assertTrue(policy.wouldBeRefused(List.of(linkOn(NODE_A)), NODE_B, repository(false)));
        assertTrue(!policy.wouldBeRefused(List.of(linkOn(NODE_A)), NODE_A, repository(false)));
        assertTrue(!policy.wouldBeRefused(List.of(), NODE_B, repository(false)));
    }
}
