package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import cz.tacr.elza.aiprovider.client.vo.ProposedItemValue;
import cz.tacr.elza.core.data.CachedItemSpec;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;

import jakarta.persistence.EntityManager;

/**
 * A proposed item's specification must reach the database as a <b>managed
 * entity</b>, never as the static-data catalog's copy.
 *
 * <p>{@link CachedItemSpec} extends {@link RulItemSpec}, so assigning the cached
 * copy to an item compiles — and then fails at the next flush with "Unable to
 * locate persister: CachedItemSpec", reported from whichever query happened to
 * trigger that flush rather than from the assignment that caused it. It did, on
 * 2026-08-09, when applying a proposed change: the failure surfaced from the
 * block mapper rendering the result, several calls away.
 */
class AiProposalSpecResolutionTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final AiProposalService service = new AiProposalService();

    AiProposalSpecResolutionTest() {
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    /** An item type that uses specifications, offering exactly one under {@code CODE}. */
    private ItemType itemTypeWithSpec(final RulItemSpec spec) {
        RulItemType entity = new RulItemType();
        entity.setUseSpecification(Boolean.TRUE);
        ItemType itemType = mock(ItemType.class);
        when(itemType.getEntity()).thenReturn(entity);
        when(itemType.getItemSpecByCode("CODE")).thenReturn(spec);
        return itemType;
    }

    private static RulItemSpec spec(final int id) {
        RulItemSpec spec = new RulItemSpec();
        spec.setItemSpecId(id);
        return spec;
    }

    @Test
    void aCachedSpecIsResolvedToTheManagedEntityBeforeItReachesAnItem() {
        // What the static-data catalog hands out is a copy, not an entity.
        CachedItemSpec cached = new CachedItemSpec(spec(42));
        RulItemSpec managed = spec(42);
        when(entityManager.getReference(RulItemSpec.class, 42)).thenReturn(managed);

        RulItemSpec resolved = ReflectionTestUtils.invokeMethod(service, "resolveSpec",
                itemTypeWithSpec(cached), new ProposedItemValue().type("T").spec("CODE"));

        assertThat(resolved).isSameAs(managed);
        assertThat(resolved).isNotInstanceOf(CachedItemSpec.class);
    }

    @Test
    void aTypeWithoutSpecificationsResolvesToNoneAndTouchesNoEntity() {
        RulItemType entity = new RulItemType();
        entity.setUseSpecification(Boolean.FALSE);
        ItemType itemType = mock(ItemType.class);
        when(itemType.getEntity()).thenReturn(entity);

        RulItemSpec resolved = ReflectionTestUtils.invokeMethod(service, "resolveSpec",
                itemType, new ProposedItemValue().type("T").spec("CODE"));

        assertThat(resolved).isNull();
    }
}
