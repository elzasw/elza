package cz.tacr.elza.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.service.FundLevelService.AddLevelDirection;

/**
 * Test {@link RevertingChangesService#existsChangeAfter(Integer, Integer, Integer, java.util.Collection)}.
 *
 * Dotaz se skládá z mnoha větví UNION a sestavuje se za běhu, test proto ověřuje jak
 * jeho syntaxi a navázání parametrů, tak sémantiku - inkluzivní hranici, ignorované
 * změny a omezení na AS, případně na JP.
 */
public class RevertingChangesServiceTest extends AbstractServiceTest {

    @Autowired
    private RevertingChangesService revertingChangesService;

    @Autowired
    private FundLevelService fundLevelService;

    @Test
    public void existsChangeAfter() {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.executeWithoutResult(r -> existsChangeAfterInTransaction());
    }

    private void existsChangeAfterInTransaction() {
        authorizeAsAdmin();

        FundInfo fund = createFund("F-exists-change");
        Integer fundId = fund.getFund().getFundId();
        Integer fundCreateChangeId = fund.getFundVersion().getCreateChange().getChangeId();

        // hranice je inkluzivní - změna, kterou AS vzniklo, se musí najít
        assertTrue(revertingChangesService.existsChangeAfter(fundId, null, fundCreateChangeId, null));
        // neprázdný, ale nerelevantní seznam ignorovaných změn nesmí výsledek ovlivnit
        assertTrue(revertingChangesService.existsChangeAfter(fundId, null, fundCreateChangeId,
                                                             List.of(Integer.MIN_VALUE)));

        // přidání JP = nová změna v AS
        ArrLevel child = addChild(fund);
        Integer childChangeId = child.getCreateChange().getChangeId();
        assertTrue(childChangeId > fundCreateChangeId);
        assertTrue(revertingChangesService.existsChangeAfter(fundId, null, childChangeId, null));

        // druhé AS vzniká až po prvním, jeho změna tedy ohraničuje změny prvního AS
        FundInfo otherFund = createFund("F-exists-change-other");
        Integer otherFundId = otherFund.getFund().getFundId();
        Integer otherCreateChangeId = otherFund.getFundVersion().getCreateChange().getChangeId();
        assertTrue(otherCreateChangeId > childChangeId);

        // změny cizího AS se do prvního AS nesmí propsat
        assertFalse(revertingChangesService.existsChangeAfter(fundId, null, otherCreateChangeId, null));
        assertTrue(revertingChangesService.existsChangeAfter(otherFundId, null, otherCreateChangeId, null));

        // po vyignorování všech změn prvního AS od přidání JP dál už nic nezbyde
        List<Integer> changesSinceChild = IntStream.range(childChangeId, otherCreateChangeId)
                .boxed().collect(Collectors.toList());
        assertFalse(revertingChangesService.existsChangeAfter(fundId, null, childChangeId, changesSinceChild));

        // omezení na JP
        assertTrue(revertingChangesService.existsChangeAfter(fundId, child.getNode().getNodeId(),
                                                              childChangeId, null));
    }

    private ArrLevel addChild(FundInfo fund) {
        ArrNode parent = nodeRepository.findById(fund.getRootNodeId()).orElseThrow();
        List<ArrLevel> levels = fundLevelService.addNewLevel(fund.getFundVersion(), parent, parent,
                                                              AddLevelDirection.CHILD, null, null, null, null, null);
        // nově vytvořená úroveň má nejvyšší identifikátor
        return levels.stream().max(Comparator.comparing(ArrLevel::getLevelId)).orElseThrow();
    }
}
