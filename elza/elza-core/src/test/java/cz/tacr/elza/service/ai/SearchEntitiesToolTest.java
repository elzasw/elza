package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.repository.ApCachedAccessPointRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.AccessPointCacheService;

/**
 * The {@code searchEntities} AI tool's type-code resolution. The rejection of
 * an unknown code must <em>name the archive's entity classes</em>: the model
 * knows international vocabularies (a live turn sent {@code CORPORATE_BODY},
 * the EAC-CPF term, for {@code PARTY_GROUP}), not this archive's typology, and
 * each failed call costs the conversation a full suspend/poll round-trip — the
 * error is what lets it correct the call in one retry. A known class code must
 * search the class's whole subtree.
 */
class SearchEntitiesToolTest {

    private final UserService userService = mock(UserService.class);
    private final AccessPointService accessPointService = mock(AccessPointService.class);
    private final AccessPointCacheService accessPointCacheService = mock(AccessPointCacheService.class);
    private final ApCachedAccessPointRepository cachedAccessPointRepository =
            mock(ApCachedAccessPointRepository.class);
    private final ApTypeRepository apTypeRepository = mock(ApTypeRepository.class);
    private final ScopeRepository scopeRepository = mock(ScopeRepository.class);
    private final AiContextResolver aiContextResolver = mock(AiContextResolver.class);
    private final StaticDataService staticDataService = mock(StaticDataService.class);
    private final StaticDataProvider sdp = mock(StaticDataProvider.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

    private SearchEntitiesTool tool;

    @BeforeEach
    void setUp() {
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(staticDataService.getData()).thenReturn(sdp);
        // the virtual admin account: every scope readable
        when(scopeRepository.findAllIds()).thenReturn(Set.of(1));
        tool = new SearchEntitiesTool(userService, accessPointService, accessPointCacheService,
                cachedAccessPointRepository, apTypeRepository, scopeRepository, aiContextResolver,
                staticDataService, new TransactionTemplate(txManager), new ObjectMapper());
    }

    @Test
    void unknownTypeCodeIsRejectedNamingTheValidClasses() {
        when(sdp.getApTypeByCode("CORPORATE_BODY")).thenReturn(null);
        when(sdp.getApTypes()).thenReturn(List.of(
                apType(1, "PERSON", null),
                apType(2, "PARTY_GROUP", null),
                apType(3, "COMPANY", 2), // a subtype — never part of the taught list
                apType(4, "DYNASTY", null)));

        assertThatThrownBy(() -> tool.execute(Map.of("typeCode", "CORPORATE_BODY"),
                new AiToolContext(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type code: CORPORATE_BODY")
                // the top-level classes only, sorted — the corrective vocabulary
                .hasMessageContaining("Valid entity classes (each includes its subtypes): "
                        + "DYNASTY, PARTY_GROUP, PERSON");
    }

    @Test
    void knownClassCodeSearchesItsWholeSubtree() {
        when(sdp.getApTypeByCode("PARTY_GROUP")).thenReturn(apType(2, "PARTY_GROUP", null));
        when(apTypeRepository.findSubtreeIds(Set.of(2))).thenReturn(Set.of(2, 3));
        when(accessPointService.findApAccessPointBySearchFilter(any(), any(), any(), anyCollection(),
                any(), anyInt(), anyInt(), any())).thenReturn(Page.empty());

        tool.execute(Map.of("typeCode", "PARTY_GROUP"), new AiToolContext(null));

        ArgumentCaptor<Set<Integer>> typeIds = typeIdsCaptor();
        verify(accessPointService).findApAccessPointBySearchFilter(any(), typeIds.capture(), any(),
                anyCollection(), any(), anyInt(), anyInt(), any());
        assertThat(typeIds.getValue()).containsExactlyInAnyOrder(2, 3);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static ArgumentCaptor<Set<Integer>> typeIdsCaptor() {
        return ArgumentCaptor.forClass((Class) Set.class);
    }

    /** A real ApType row: id + code, parent linked when given (a class is parentless). */
    private static ApType apType(final int id, final String code, final Integer parentId) {
        ApType type = new ApType();
        type.setApTypeId(id);
        type.setCode(code);
        if (parentId != null) {
            ApType parent = new ApType();
            parent.setApTypeId(parentId);
            type.setParentApType(parent);
        }
        return type;
    }
}
