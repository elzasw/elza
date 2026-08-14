package cz.tacr.elza.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFsLink;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.ArrFsLinkRepository;
import cz.tacr.elza.repository.ArrLegacyDaoLinkRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.service.dao.FileSystemRepoBrowser;
import cz.tacr.elza.service.dao.FileSystemRepoService;
import cz.tacr.elza.service.eventnotification.EventNotificationService;

/**
 * Unit tests for {@link DaoService#createFsDaoLink} and {@link DaoService#moveFsDaoLink}.
 * Authorization (@AuthMethod / @AuthParam) and @Transactional are AOP concerns and are
 * covered by integration tests at the controller level — not here.
 */
public class DaoServiceFsLinkTest {

    private DaoService service;

    private FileSystemRepoService fileSystemRepoService;
    private ArrFsLinkRepository fsLinkRepository;
    private DaoLinkRepository daoLinkRepository;
    private ArrLegacyDaoLinkRepository legacyDaoLinkRepository;
    private ArrangementInternalService arrangementInternalService;
    private EventNotificationService eventNotificationService;
    private ArrangementCacheService arrangementCacheService;

    private ArrDigitalRepository repo;
    private ArrFund fund;
    private ArrFundVersion fundVersion;
    private ArrNode nodeA;
    private ArrNode nodeB;

    @BeforeEach
    void setUp() {
        fileSystemRepoService = Mockito.mock(FileSystemRepoService.class);
        fsLinkRepository = Mockito.mock(ArrFsLinkRepository.class);
        daoLinkRepository = Mockito.mock(DaoLinkRepository.class);
        legacyDaoLinkRepository = Mockito.mock(ArrLegacyDaoLinkRepository.class);
        arrangementInternalService = Mockito.mock(ArrangementInternalService.class);
        eventNotificationService = Mockito.mock(EventNotificationService.class);
        arrangementCacheService = Mockito.mock(ArrangementCacheService.class);

        // Defaults: the cache-refresh path fetches links for the affected nodes; keep them empty.
        Mockito.when(legacyDaoLinkRepository.findByNodeIdsAndFetchDao(Mockito.any()))
                .thenReturn(Collections.emptyList());
        Mockito.when(fsLinkRepository.findByNodeIdInAndDeleteChangeIsNull(Mockito.any()))
                .thenReturn(Collections.emptyList());

        // save() returns the entity itself; stamp an id so the "publish + cache" paths see one.
        Mockito.when(daoLinkRepository.save(Mockito.<ArrFsLink>any())).thenAnswer(inv -> {
            ArrFsLink l = inv.getArgument(0);
            if (l.getDaoLinkId() == null) {
                l.setDaoLinkId(9000 + l.hashCode() & 0xFFFF);
            }
            return l;
        });

        // createChange returns a fresh ArrChange (unique per call).
        Mockito.when(arrangementInternalService.createChange(Mockito.any(ArrChange.Type.class), Mockito.any()))
                .thenAnswer(inv -> {
                    ArrChange c = new ArrChange();
                    c.setChangeId(System.identityHashCode(inv.getArguments()));
                    return c;
                });

        service = new DaoService();
        setField(service, "fileSystemRepoService", fileSystemRepoService);
        setField(service, "fsLinkRepository", fsLinkRepository);
        setField(service, "daoLinkRepository", daoLinkRepository);
        setField(service, "legacyDaoLinkRepository", legacyDaoLinkRepository);
        setField(service, "arrangementInternalService", arrangementInternalService);
        setField(service, "eventNotificationService", eventNotificationService);
        setField(service, "arrangementCacheService", arrangementCacheService);
        // browser only used by other methods — not needed here, but injected so no NPE on scanning
        setField(service, "fileSystemRepoBrowser", Mockito.mock(FileSystemRepoBrowser.class));

        repo = new ArrDigitalRepository();
        repo.setExternalSystemId(42);
        repo.setCode("REPO");
        repo.setName("test-repo");

        fund = new ArrFund();
        fund.setFundId(1);

        fundVersion = new ArrFundVersion();
        fundVersion.setFundVersionId(11);
        fundVersion.setFund(fund);

        nodeA = new ArrNode();
        nodeA.setNodeId(100);
        nodeB = new ArrNode();
        nodeB.setNodeId(200);

        Mockito.when(fileSystemRepoService.getPath(eq(repo), eq(fund)))
                .thenReturn(java.nio.file.Paths.get("/tmp/repo-root"));
    }

    // ------------------------------------------------------------
    //  createFsDaoLink
    // ------------------------------------------------------------

    @Test
    void createFsDaoLink_normalizesPathAndPassesContainmentCheck() {
        // surrounding whitespace is trimmed; the containment check receives the normalized path
        service.createFsDaoLink(fundVersion, repo, nodeA, "  folder/sub  ");

        Mockito.verify(fileSystemRepoService).getPath(repo, fund);
        Mockito.verify(fileSystemRepoService).resolvePath(Mockito.any(java.nio.file.Path.class), eq("folder/sub"));
    }

    @Test
    void createFsDaoLink_idempotentWhenLinkAlreadyOnSameNode() {
        ArrFsLink existing = fsLink(1, nodeA, "folder/a");
        Mockito.when(fsLinkRepository.findByDigitalRepositoryAndPathAndDeleteChangeIsNull(repo, "folder/a"))
                .thenReturn(List.of(existing));

        ArrFsLink result = service.createFsDaoLink(fundVersion, repo, nodeA, "folder/a");

        assertSame(existing, result);
        Mockito.verify(daoLinkRepository, Mockito.never()).save(Mockito.<ArrFsLink>any());
        Mockito.verify(arrangementInternalService, Mockito.never())
                .createChange(Mockito.any(ArrChange.Type.class), Mockito.any());
        Mockito.verifyNoInteractions(eventNotificationService);
    }

    @Test
    void createFsDaoLink_deniesSecondLinkWhenMultipleLinksFalse() {
        repo.setMultipleLinks(null); // default = false
        ArrFsLink existing = fsLink(1, nodeB, "folder/a");
        Mockito.when(fsLinkRepository.findByDigitalRepositoryAndPathAndDeleteChangeIsNull(repo, "folder/a"))
                .thenReturn(List.of(existing));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createFsDaoLink(fundVersion, repo, nodeA, "folder/a"));
        assertEquals(ArrangementCode.INVALID_DAO, ex.getErrorCode());

        Mockito.verify(daoLinkRepository, Mockito.never()).save(Mockito.<ArrFsLink>any());
        Mockito.verifyNoInteractions(eventNotificationService);
    }

    @Test
    void createFsDaoLink_allowsSecondLinkWhenMultipleLinksTrue() {
        repo.setMultipleLinks(Boolean.TRUE);
        ArrFsLink existing = fsLink(1, nodeB, "folder/a");
        Mockito.when(fsLinkRepository.findByDigitalRepositoryAndPathAndDeleteChangeIsNull(repo, "folder/a"))
                .thenReturn(List.of(existing));

        ArrFsLink result = service.createFsDaoLink(fundVersion, repo, nodeA, "folder/a");

        assertNotNull(result);
        assertEquals(nodeA.getNodeId(), result.getNodeId());
        assertEquals("folder/a", result.getPath());
        Mockito.verify(daoLinkRepository, Mockito.times(1)).save(Mockito.<ArrFsLink>any());
        Mockito.verify(arrangementInternalService)
                .createChange(eq(ArrChange.Type.CREATE_DAO_LINK), eq(nodeA));
        Mockito.verify(eventNotificationService).publishEvent(Mockito.any());
    }

    @Test
    void createFsDaoLink_nullPathQueriedViaPathIsNull() {
        Mockito.when(fsLinkRepository.findByDigitalRepositoryAndPathIsNullAndDeleteChangeIsNull(repo))
                .thenReturn(Collections.emptyList());

        ArrFsLink result = service.createFsDaoLink(fundVersion, repo, nodeA, null);

        assertNull(result.getPath());
        Mockito.verify(fsLinkRepository).findByDigitalRepositoryAndPathIsNullAndDeleteChangeIsNull(repo);
        Mockito.verify(fsLinkRepository, Mockito.never())
                .findByDigitalRepositoryAndPathAndDeleteChangeIsNull(Mockito.any(), Mockito.any());
    }

    @Test
    void createFsDaoLink_happyPathSavesLinkAndUpdatesCache() {
        Mockito.when(fsLinkRepository.findByDigitalRepositoryAndPathAndDeleteChangeIsNull(repo, "folder/a"))
                .thenReturn(Collections.emptyList());

        ArrFsLink result = service.createFsDaoLink(fundVersion, repo, nodeA, "folder/a");

        assertNotNull(result);
        assertEquals("folder/a", result.getPath());
        assertSame(repo, result.getDigitalRepository());
        assertEquals(nodeA.getNodeId(), result.getNodeId());
        assertNotNull(result.getCreateChange());

        Mockito.verify(arrangementInternalService)
                .createChange(eq(ArrChange.Type.CREATE_DAO_LINK), eq(nodeA));
        Mockito.verify(daoLinkRepository).save(Mockito.<ArrFsLink>any());
        Mockito.verify(eventNotificationService).publishEvent(Mockito.any());
        Mockito.verify(arrangementCacheService).updateDaoLinks(eq(Collections.singleton(nodeA.getNodeId())), Mockito.any());
    }

    // ------------------------------------------------------------
    //  moveFsDaoLink
    // ------------------------------------------------------------

    @Test
    void moveFsDaoLink_throwsWhenNotFound() {
        Mockito.when(fsLinkRepository.findById(555)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.moveFsDaoLink(fundVersion, 555, nodeB));
        assertEquals(ArrangementCode.INVALID_DAO, ex.getErrorCode());

        Mockito.verify(daoLinkRepository, Mockito.never()).save(Mockito.<ArrFsLink>any());
    }

    @Test
    void moveFsDaoLink_throwsWhenAlreadyDeleted() {
        ArrFsLink old = fsLink(1, nodeA, "folder/a");
        ArrChange delChange = new ArrChange();
        delChange.setChangeId(999);
        old.setDeleteChange(delChange);
        Mockito.when(fsLinkRepository.findById(1)).thenReturn(Optional.of(old));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.moveFsDaoLink(fundVersion, 1, nodeB));
        assertEquals(ArrangementCode.INVALID_DAO, ex.getErrorCode());
    }

    @Test
    void moveFsDaoLink_idempotentWhenSameNode() {
        ArrFsLink old = fsLink(1, nodeA, "folder/a");
        Mockito.when(fsLinkRepository.findById(1)).thenReturn(Optional.of(old));

        ArrFsLink result = service.moveFsDaoLink(fundVersion, 1, nodeA);

        assertSame(old, result);
        Mockito.verify(daoLinkRepository, Mockito.never()).save(Mockito.<ArrFsLink>any());
        Mockito.verify(arrangementInternalService, Mockito.never())
                .createChange(Mockito.any(ArrChange.Type.class), Mockito.any());
    }

    @Test
    void moveFsDaoLink_consolidatesWhenTargetAlreadyLinked() {
        ArrFsLink oldLink = fsLink(1, nodeA, "folder/a");
        oldLink.setDigitalRepository(repo);
        ArrFsLink existingOnTarget = fsLink(2, nodeB, "folder/a");
        existingOnTarget.setDigitalRepository(repo);
        Mockito.when(fsLinkRepository.findById(1)).thenReturn(Optional.of(oldLink));
        Mockito.when(fsLinkRepository.findByDigitalRepositoryAndPathAndDeleteChangeIsNull(repo, "folder/a"))
                .thenReturn(List.of(existingOnTarget));

        ArrFsLink result = service.moveFsDaoLink(fundVersion, 1, nodeB);

        assertSame(existingOnTarget, result);
        // source was closed
        assertNotNull(oldLink.getDeleteChange());
        // no *new* fs link was saved; the only save is the deleteDaoLink's save of oldLink
        ArgumentCaptor<ArrFsLink> saveCap = ArgumentCaptor.forClass(ArrFsLink.class);
        Mockito.verify(daoLinkRepository, Mockito.times(1)).save(saveCap.capture());
        assertSame(oldLink, saveCap.getValue());
        Mockito.verify(arrangementInternalService)
                .createChange(eq(ArrChange.Type.DELETE_DAO_LINK), eq(nodeA));
    }

    @Test
    void moveFsDaoLink_movesAtomicallyWithSharedChange() {
        ArrFsLink oldLink = fsLink(1, nodeA, "folder/a");
        oldLink.setDigitalRepository(repo);
        Mockito.when(fsLinkRepository.findById(1)).thenReturn(Optional.of(oldLink));
        Mockito.when(fsLinkRepository.findByDigitalRepositoryAndPathAndDeleteChangeIsNull(repo, "folder/a"))
                .thenReturn(Collections.emptyList());

        ArrFsLink result = service.moveFsDaoLink(fundVersion, 1, nodeB);

        assertNotNull(result);
        assertEquals(nodeB.getNodeId(), result.getNodeId());
        assertEquals("folder/a", result.getPath());
        assertSame(repo, result.getDigitalRepository());

        // exactly ONE createChange, of type CREATE_DAO_LINK, for the target node
        ArgumentCaptor<ArrChange.Type> typeCap = ArgumentCaptor.forClass(ArrChange.Type.class);
        Mockito.verify(arrangementInternalService, Mockito.times(1))
                .createChange(typeCap.capture(), eq(nodeB));
        assertEquals(ArrChange.Type.CREATE_DAO_LINK, typeCap.getValue());

        // both saves — the new link and the source link's soft delete — share the same ArrChange
        ArgumentCaptor<ArrFsLink> saveCap = ArgumentCaptor.forClass(ArrFsLink.class);
        Mockito.verify(daoLinkRepository, Mockito.times(2)).save(saveCap.capture());
        List<ArrFsLink> saved = saveCap.getAllValues();
        ArrFsLink newSaved = saved.stream().filter(l -> nodeB.getNodeId().equals(l.getNodeId())).findFirst().orElseThrow();
        ArrFsLink oldSaved = saved.stream().filter(l -> nodeA.getNodeId().equals(l.getNodeId())).findFirst().orElseThrow();
        assertNotNull(newSaved.getCreateChange());
        assertNotNull(oldSaved.getDeleteChange());
        assertSame(newSaved.getCreateChange(), oldSaved.getDeleteChange(),
                "move must reuse the same ArrChange for create + delete");
        assertTrue(result.getCreateChange() == newSaved.getCreateChange());
    }

    // ------------------------------------------------------------
    //  helpers
    // ------------------------------------------------------------

    private ArrFsLink fsLink(int id, ArrNode node, String path) {
        ArrFsLink link = new ArrFsLink();
        link.setDaoLinkId(id);
        link.setNode(node);
        link.setPath(path);
        link.setDigitalRepository(repo);
        return link;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
