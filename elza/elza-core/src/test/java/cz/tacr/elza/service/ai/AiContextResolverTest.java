package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import cz.tacr.elza.aiprovider.client.vo.AiObject;
import cz.tacr.elza.aiprovider.client.vo.ArchivalEntity;
import cz.tacr.elza.aiprovider.client.vo.ArchivalEntityObject;
import cz.tacr.elza.aiprovider.client.vo.DescriptionItem;
import cz.tacr.elza.aiprovider.client.vo.EntityPart;
import cz.tacr.elza.controller.vo.AiContextAccesspointVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedBinding;
import cz.tacr.elza.service.cache.CachedPart;

/**
 * The AccessPoint → {@code elza.archivalEntity} resolution in {@link AiContextResolver}:
 * identity, classification from the {@link ApType} hierarchy, external identity, the
 * part tree (preferred part flagged, sub-parts nested), item mapping, and the batched
 * enrichment of referenced entities carried by {@code RECORD_REF} items.
 */
class AiContextResolverTest {

    private static final int AP_ID = 100;
    private static final int SCOPE_ID = 5;
    private static final int REF_AP_ID = 101;
    private static final int TYPE_INDIVIDUAL = 20; // subclass
    private static final int TYPE_PERSON = 10;     // class (hierarchy root)
    private static final int PART_NAME = 200;
    private static final int PART_BODY = 201;
    private static final int PART_NAME_SUB = 202;
    private static final int IT_STRING = 1;
    private static final int IT_RECORD_REF = 2;

    private final AccessPointCacheService cacheService = mock(AccessPointCacheService.class);
    private final AccessPointService accessPointService = mock(AccessPointService.class);
    private final StaticDataService staticDataService = mock(StaticDataService.class);
    private final StaticDataProvider sdp = mock(StaticDataProvider.class);
    private final UserService userService = mock(UserService.class);

    private final AiContextResolver resolver = new AiContextResolver();

    private void wire() {
        ReflectionTestUtils.setField(resolver, "accessPointCacheService", cacheService);
        ReflectionTestUtils.setField(resolver, "accessPointService", accessPointService);
        ReflectionTestUtils.setField(resolver, "staticDataService", staticDataService);
        ReflectionTestUtils.setField(resolver, "userService", userService);
        when(staticDataService.getData()).thenReturn(sdp);

        UserDetail user = mock(UserDetail.class);
        when(user.hasPermission(Permission.ADMIN)).thenReturn(true);
        when(userService.getLoggedUserDetail()).thenReturn(user);
    }

    @Test
    void resolvesAccessPointToArchivalEntityWithEnrichedReferences() {
        wire();

        // Type hierarchy: PERSON_INDIVIDUAL (subclass) → PERSON (class, root).
        ApType person = apType(TYPE_PERSON, "PERSON", "osoba / bytost", null);
        ApType individual = apType(TYPE_INDIVIDUAL, "PERSON_INDIVIDUAL", "fyzická osoba", person);
        when(sdp.getApTypeById(TYPE_PERSON)).thenReturn(person);
        when(sdp.getApTypeById(TYPE_INDIVIDUAL)).thenReturn(individual);

        // Item types: a plain string and an access-point reference. Built before
        // stubbing, since the builder stubs its own mocks (avoids UnfinishedStubbing).
        ItemType stringType = itemType("NM_MAIN", "STRING");
        ItemType refType = itemType("REL_PERSON", "RECORD_REF");
        when(sdp.getItemTypeById(IT_STRING)).thenReturn(stringType);
        when(sdp.getItemTypeById(IT_RECORD_REF)).thenReturn(refType);
        when(sdp.getPartTypeByCode("PT_NAME")).thenReturn(partType("Jméno"));
        when(sdp.getPartTypeByCode("PT_BODY")).thenReturn(partType("Tělo"));
        when(sdp.getPartTypeByCode("PT_NAME_SUB")).thenReturn(partType("Doplněk jména"));

        // Cached access point: state, uuid, one binding, a small part tree.
        CachedAccessPoint cap = new CachedAccessPoint();
        cap.setAccessPointId(AP_ID);
        cap.setUuid("ap-uuid");
        cap.setPreferredPartId(PART_NAME);
        cap.setApState(apState(individual));
        cap.setBindings(List.of(binding("CAM", "ext-123")));
        cap.setParts(List.of(
                namePart(),
                simplePart(PART_BODY, "PT_BODY", null, "Tělo hesla"),
                simplePart(PART_NAME_SUB, "PT_NAME_SUB", PART_NAME, "vévoda")));
        when(cacheService.findCachedAccessPoint(AP_ID)).thenReturn(cap);

        // Enrichment batch for the referenced entity (a person) carried by REL_PERSON.
        when(accessPointService.groupStateByAccessPointId(any()))
                .thenReturn(Map.of(REF_AP_ID, apState(individual)));
        when(accessPointService.findPreferredPartIndexMapByIds(any()))
                .thenReturn(Map.of(REF_AP_ID, index("Karel IV.")));

        AiContextAccesspointVO ctx = new AiContextAccesspointVO();
        ctx.setAccessPointId(AP_ID);

        List<AiObject> resolved = resolver.resolveAll(List.of(ctx));

        assertThat(resolved).hasSize(1);
        ArchivalEntity entity = ((ArchivalEntityObject) resolved.get(0)).getData();

        // Identity + classification + external identity.
        assertThat(entity.getAccessPointId()).isEqualTo(AP_ID);
        assertThat(entity.getUuid()).isEqualTo("ap-uuid");
        assertThat(entity.getClassCode()).isEqualTo("PERSON");
        assertThat(entity.getClassName()).isEqualTo("osoba / bytost");
        assertThat(entity.getTypeCode()).isEqualTo("PERSON_INDIVIDUAL");
        assertThat(entity.getTypeName()).isEqualTo("fyzická osoba");
        assertThat(entity.getExternalSystemCode()).isEqualTo("CAM");
        assertThat(entity.getExternalId()).isEqualTo("ext-123");

        // Two top-level parts; the name part is preferred and holds the sub-part.
        assertThat(entity.getParts()).hasSize(2);
        EntityPart namePart = entity.getParts().get(0);
        assertThat(namePart.getPartType()).isEqualTo("PT_NAME");
        assertThat(namePart.getPartTypeName()).isEqualTo("Jméno");
        assertThat(namePart.getPreferred()).isTrue();
        assertThat(namePart.getValue()).isEqualTo("Jan Novák");
        assertThat(namePart.getParts()).extracting(EntityPart::getPartType).containsExactly("PT_NAME_SUB");

        EntityPart bodyPart = entity.getParts().get(1);
        assertThat(bodyPart.getPartType()).isEqualTo("PT_BODY");
        assertThat(bodyPart.getPreferred()).isNull();

        // The name part's items: a scalar with its value, and an enriched entity reference.
        DescriptionItem stringItem = namePart.getItems().get(0);
        assertThat(stringItem.getType()).isEqualTo("NM_MAIN");
        assertThat(stringItem.getValue()).isEqualTo("Novák");
        assertThat(stringItem.getEntity()).isNull();

        DescriptionItem refItem = namePart.getItems().get(1);
        assertThat(refItem.getType()).isEqualTo("REL_PERSON");
        assertThat(refItem.getEntity()).isNotNull();
        assertThat(refItem.getEntity().getAccessPointId()).isEqualTo(REF_AP_ID);
        // Filled by the batched enrichment (cache path carried only the id).
        assertThat(refItem.getEntity().getClassName()).isEqualTo("osoba / bytost");
        assertThat(refItem.getEntity().getTypeName()).isEqualTo("fyzická osoba");
        assertThat(refItem.getEntity().getPreferredName()).isEqualTo("Karel IV.");
    }

    @Test
    void skipsUnreadableAccessPoint() {
        ReflectionTestUtils.setField(resolver, "accessPointCacheService", cacheService);
        ReflectionTestUtils.setField(resolver, "accessPointService", accessPointService);
        ReflectionTestUtils.setField(resolver, "staticDataService", staticDataService);
        ReflectionTestUtils.setField(resolver, "userService", userService);

        UserDetail user = mock(UserDetail.class); // no permissions granted
        when(userService.getLoggedUserDetail()).thenReturn(user);

        CachedAccessPoint cap = new CachedAccessPoint();
        cap.setApState(apState(apType(TYPE_INDIVIDUAL, "PERSON_INDIVIDUAL", "fyzická osoba", null)));
        when(cacheService.findCachedAccessPoint(AP_ID)).thenReturn(cap);

        AiContextAccesspointVO ctx = new AiContextAccesspointVO();
        ctx.setAccessPointId(AP_ID);

        assertThat(resolver.resolveAll(List.of(ctx))).isEmpty();
    }

    // --- builders -----------------------------------------------------------

    private CachedPart namePart() {
        ApItem string = apItem(IT_STRING, stringData("Novák"));
        ApItem ref = apItem(IT_RECORD_REF, recordRef(REF_AP_ID));
        CachedPart part = simplePart(PART_NAME, "PT_NAME", null, "Jan Novák");
        part.setItems(List.of(string, ref));
        return part;
    }

    private CachedPart simplePart(int partId, String typeCode, Integer parentId, String displayName) {
        CachedPart part = new CachedPart();
        part.setPartId(partId);
        part.setPartTypeCode(typeCode);
        part.setParentPartId(parentId);
        part.setIndices(List.of(index(displayName)));
        return part;
    }

    private ApState apState(ApType type) {
        ApState state = new ApState();
        state.setApType(type);
        ApScope scope = new ApScope();
        scope.setScopeId(SCOPE_ID);
        state.setScope(scope);
        return state;
    }

    private ApType apType(int id, String code, String name, ApType parent) {
        ApType type = new ApType();
        type.setApTypeId(id);
        type.setCode(code);
        type.setName(name);
        if (parent != null) {
            type.setParentApType(parent);
        }
        return type;
    }

    private CachedBinding binding(String systemCode, String value) {
        CachedBinding binding = new CachedBinding();
        binding.setExternalSystemCode(systemCode);
        binding.setValue(value);
        return binding;
    }

    private ApIndex index(String value) {
        ApIndex index = new ApIndex();
        index.setIndexType("DISPLAY_NAME");
        index.setIndexValue(value);
        return index;
    }

    private ApItem apItem(int itemTypeId, ArrData data) {
        RulItemType rulItemType = new RulItemType();
        rulItemType.setItemTypeId(itemTypeId);
        ApItem item = new ApItem();
        item.setItemType(rulItemType);
        item.setData(data);
        return item;
    }

    private ArrData stringData(String value) {
        ArrData data = mock(ArrData.class);
        when(data.getFulltextValue()).thenReturn(value);
        return data;
    }

    private ArrDataRecordRef recordRef(int accessPointId) {
        ApAccessPoint ap = new ApAccessPoint();
        ap.setAccessPointId(accessPointId);
        ArrDataRecordRef ref = new ArrDataRecordRef();
        ref.setRecord(ap);
        return ref;
    }

    private ItemType itemType(String code, String dataTypeCode) {
        ItemType itemType = mock(ItemType.class);
        when(itemType.getCode()).thenReturn(code);
        cz.tacr.elza.core.data.DataType dataType = mock(cz.tacr.elza.core.data.DataType.class);
        when(dataType.getCode()).thenReturn(dataTypeCode);
        when(itemType.getDataType()).thenReturn(dataType);
        return itemType;
    }

    private RulPartType partType(String name) {
        RulPartType partType = new RulPartType();
        partType.setName(name);
        return partType;
    }
}
