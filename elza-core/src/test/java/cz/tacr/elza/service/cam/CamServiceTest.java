package cz.tacr.elza.service.cam;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.common.db.HibernateUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.DateTimeXml;
import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.EntityIdXml;
import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.schema.cam.ItemStringXml;
import cz.tacr.cam.schema.cam.ItemsXml;
import cz.tacr.cam.schema.cam.LongStringXml;
import cz.tacr.cam.schema.cam.PartTypeXml;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.PartsXml;
import cz.tacr.cam.schema.cam.RevInfoXml;
import cz.tacr.cam.schema.cam.StringXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.controller.AbstractControllerTest;
import cz.tacr.elza.controller.vo.ApExternalSystemVO;
import cz.tacr.elza.controller.vo.ApScopeVO;
import cz.tacr.elza.controller.vo.SysExternalSystemVO;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;

public class CamServiceTest extends AbstractControllerTest {
    static public final String SYSTEM_CODE = "TESTSYSTEM";
    static public final Long EXT_ID_1 = 1L;
    static public final String EXT_UUID_1 = UUID.randomUUID().toString();
    static public final Long EXT_ID_2 = 2L;
    static public final String EXT_UUID_2 = UUID.randomUUID().toString();

    @Autowired
    CamService camService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    ExternalSystemService externalSystemService;

    @Autowired
    StaticDataService staticDataService;

    @Autowired
    ScopeRepository scopeRepository;

    @Autowired
    AccessPointCacheService accessPointCacheService;

    static public final EntitiesXml IMPORT_ENTITIES = new EntitiesXml();
    // prepare test data
    static {
        EntityXml ent1 = createEntity(EXT_ID_1, EXT_UUID_1, "ent1");
    	IMPORT_ENTITIES.getList().add(ent1);
    	// ent2
    	EntityXml ent2 = createEntity(EXT_ID_2, EXT_UUID_2, "ent2");
    	// add ref to ent1
    	PartXml refPart = new PartXml(new ItemsXml(), null, null, new UuidXml(UUID.randomUUID().toString()), PartTypeXml.PT_REL);

    	EntityRecordRefXml refEntity = new EntityRecordRefXml(new EntityIdXml(EXT_ID_1), new UuidXml(EXT_UUID_1));

    	ItemEntityRefXml refItem = new ItemEntityRefXml(refEntity, new CodeXml("RT_FATHER"), new CodeXml("REL_ENTITY"), new UuidXml(UUID.randomUUID().toString()));
    	refPart.getItms().getItems().add(refItem);
    	ent2.getPrts().getList().add(refPart);

    	IMPORT_ENTITIES.getList().add(ent2);
    }

	private static EntityXml createEntity(Long extId, String uuid, String prefName) {
		EntityXml ent = new EntityXml();
		ent.setEid(new EntityIdXml(extId));
		ent.setEuid(new UuidXml(uuid));
		ent.setEns(EntityRecordStateXml.ERS_NEW);
		ent.setEnt(new CodeXml("PERSON_BEING"));
		ent.setPrts(new PartsXml());

		PartXml prefNamePart = new PartXml(new ItemsXml(), null, null, new UuidXml(uuid), PartTypeXml.PT_NAME);
		prefNamePart.getItms().getItems().add(new ItemStringXml(new StringXml(prefName), null,
				new CodeXml("NM_MAIN"), new UuidXml(uuid)));
		ent.getPrts().getList().add(prefNamePart);

		ent.setRevi(new RevInfoXml(new UuidXml(uuid), new LongStringXml("user"), null, new DateTimeXml(LocalDateTime.now())));
		return ent;
	}

	@Test
	public void importNewTest() {
    	ApScopeVO scopeVo = createScope();

        // create external system
        ApExternalSystemVO externalSystemVO = new ApExternalSystemVO();
        externalSystemVO.setCode(SYSTEM_CODE);
        externalSystemVO.setName(SYSTEM_CODE);
        externalSystemVO.setUrl("camurl");
        externalSystemVO.setApiKeyId("apikey");
        externalSystemVO.setApiKeyValue("apikeyvalue");
        externalSystemVO.setType(ApExternalSystemType.CAM);
        externalSystemVO.setScopeId(scopeVo.getId());
        SysExternalSystemVO externalSystemCreatedVO = createExternalSystem(externalSystemVO);
        assertNotNull(externalSystemCreatedVO.getId());

        // create transaction
        TransactionTemplate tt = new TransactionTemplate(transactionManager).execute(a -> {
        	// prepare bindings
        	ApScope scope = scopeRepository.getOne(scopeVo.getId());

            ApExternalSystem externalSystem = externalSystemService.findApExternalSystemByCode(SYSTEM_CODE);

            ApBinding binding1 = externalSystemService.createApBinding(EXT_ID_1.toString(), externalSystem, true);
            ApBinding binding2 = externalSystemService.createApBinding(EXT_ID_2.toString(), externalSystem, true);

            Map<String, ApBinding> bindings = new HashMap<>();
            bindings.put(binding1.getValue(), binding1);
            bindings.put(binding2.getValue(), binding2);

            Map<String, EntityXml> entXmlMap = IMPORT_ENTITIES.getList().stream().collect(Collectors.toMap(ie -> String
                    .valueOf(ie.getEid().getValue()), Function.identity()));
            EntityXml ent1 = entXmlMap.get(String.valueOf(EXT_ID_1));
            assertNotNull(ent1);
            EntityXml ent2 = entXmlMap.get(String.valueOf(EXT_ID_2));
            assertNotNull(ent2);

            ProcessingContext procCtx = new ProcessingContext(scope, externalSystem, staticDataService);
            try {
                camService.synchronizeAccessPoint(procCtx, binding1, ent1, true);
                camService.synchronizeAccessPoint(procCtx, binding2, ent2, true);
            } catch (SyncImpossibleException e) {
                fail();
            }
        	return null;
        });

        // check results
        tt = new TransactionTemplate(transactionManager).execute(a -> {
        	ApAccessPoint ap1 = accessPointService.getAccessPointByUuid(EXT_UUID_1);
        	CachedAccessPoint cachedAp1 = accessPointCacheService.findCachedAccessPoint(ap1.getAccessPointId());
        	ApAccessPoint ap2 = accessPointService.getAccessPointByUuid(EXT_UUID_2);
        	CachedAccessPoint cachedAp2 = accessPointCacheService.findCachedAccessPoint(ap2.getAccessPointId());

        	// check if cachedAp2 points to cachecAp1
        	CachedPart secondPart = cachedAp2.getParts().get(1);
        	assertEquals(secondPart.getPartTypeCode(), PartTypeXml.PT_REL.toString());
        	ApItem item = secondPart.getItems().get(0);
        	ArrData data = HibernateUtils.unproxy(item.getData());
        	ArrDataRecordRef dataRr = (ArrDataRecordRef)data;
        	assertEquals(ap1.getAccessPointId(), dataRr.getRecordId());
        	return null;
        });
	}
}
