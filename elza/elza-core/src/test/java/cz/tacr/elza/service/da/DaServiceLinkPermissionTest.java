package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.UserPermission;

/**
 * Who may attach an AIP to the archival description.
 *
 * Linking and unlinking change the description of a fund, so they take the same permission as
 * arranging it - ADMIN, FUND_ARR_ALL, or FUND_ARR on that very fund. Both endpoints were open to
 * any authenticated user before this, which is what these pin shut; the fund-scoped case is here
 * because a permission for one fund must not carry over to another.
 */
public class DaServiceLinkPermissionTest extends AbstractServiceTest {

    @Autowired
    private DaService daService;
    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    /**
     * The shared cleanup of the base class does not know the tables of the digital archive, and it
     * runs as the user this test left behind - so the context goes back to admin first.
     */
    @AfterEach
    public void deleteCreatedRows() {
        authorizeAsAdmin();
        tx().executeWithoutResult(t -> {
            daLinkRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    private Integer createAip() {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-LINK-PERM-TEST");
        repository.setName("Testovaci digitalni archiv");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setSendNotification(false);
        repository.setMultipleLinks(false);

        DaAip aip = new DaAip();
        aip.setCode("aip-link-perm-test");
        aip.setDigitalRepository(digitalRepositoryRepository.save(repository));
        return aipRepository.save(aip).getAipId();
    }

    /**
     * Logs in a user holding exactly the given permissions. The user is not stored - it has no id,
     * so it is the same case as the built-in admin as far as recording the change goes, and the
     * permissions are all the check reads.
     */
    private void authorizeAs(UserPermission... perms) {
        UsrUser user = new UsrUser();
        user.setUsername("da-link-perm-test");
        user.setActive(true);
        UserDetail userDetail = new UserDetail(user, Arrays.asList(perms), levelTreeCacheService,
                                               List.of());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("", "", null);
        auth.setDetails(userDetail);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private UserPermission fundArr(Integer fundId) {
        UserPermission permission = new UserPermission(Permission.FUND_ARR);
        permission.addFundId(fundId);
        return permission;
    }

    private Integer linkIdOf(Integer aipId) {
        return tx().execute(t -> daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).get(0)
                .getDaoLinkId());
    }

    private void assertLinkCount(int expected, Integer aipId) {
        authorizeAsAdmin();
        tx().executeWithoutResult(t -> assertEquals(expected,
                daLinkRepository.findByAipIdAndDeleteChangeIsNull(aipId).size()));
    }

    @Test
    public void aUserWithoutArrPermissionCannotLink() {
        FundInfo fund = tx().execute(t -> createFund("F-da-link-perm-none"));
        Integer aipId = tx().execute(t -> createAip());

        authorizeAs();

        assertThrows(AccessDeniedException.class, () -> tx().executeWithoutResult(t -> daService
                .createDaoLink(aipId, null, fund.getRootNodeId(), ArrDaoLink.LinkType.AIP)));

        assertLinkCount(0, aipId);
    }

    @Test
    public void aUserWithoutArrPermissionCannotUnlink() {
        FundInfo fund = tx().execute(t -> createFund("F-da-unlink-perm-none"));
        Integer aipId = tx().execute(t -> createAip());
        tx().executeWithoutResult(t -> daService
                .createDaoLink(aipId, null, fund.getRootNodeId(), ArrDaoLink.LinkType.AIP));
        Integer linkId = linkIdOf(aipId);

        authorizeAs();

        assertThrows(AccessDeniedException.class,
                     () -> tx().executeWithoutResult(t -> daService.deleteDaoLink(linkId)));

        assertLinkCount(1, aipId);
    }

    @Test
    public void fundArrOnTheFundIsEnoughToLinkAndUnlink() {
        FundInfo fund = tx().execute(t -> createFund("F-da-link-perm-arr"));
        Integer aipId = tx().execute(t -> createAip());

        authorizeAs(fundArr(fund.getFund().getFundId()));

        tx().executeWithoutResult(t -> daService
                .createDaoLink(aipId, null, fund.getRootNodeId(), ArrDaoLink.LinkType.AIP));
        assertLinkCount(1, aipId);

        authorizeAs(fundArr(fund.getFund().getFundId()));
        Integer linkId = linkIdOf(aipId);
        tx().executeWithoutResult(t -> daService.deleteDaoLink(linkId));

        assertLinkCount(0, aipId);
    }

    /** Pořádání jednoho fondu neopravňuje sahat na jiný. */
    @Test
    public void fundArrOnAnotherFundIsNotEnough() {
        FundInfo fund = tx().execute(t -> createFund("F-da-link-perm-other"));
        FundInfo otherFund = tx().execute(t -> createFund("F-da-link-perm-other-2"));
        Integer aipId = tx().execute(t -> createAip());

        authorizeAs(fundArr(otherFund.getFund().getFundId()));

        assertThrows(AccessDeniedException.class, () -> tx().executeWithoutResult(t -> daService
                .createDaoLink(aipId, null, fund.getRootNodeId(), ArrDaoLink.LinkType.AIP)));

        assertLinkCount(0, aipId);
    }
}
