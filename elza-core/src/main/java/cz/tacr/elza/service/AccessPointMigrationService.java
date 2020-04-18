package cz.tacr.elza.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.controller.vo.ap.item.*;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.Subscribe;

import cz.tacr.elza.controller.vo.nodes.descitems.UpdateOp;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApRule;
import cz.tacr.elza.domain.ApRuleSystem;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.ApRuleRepository;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.service.vo.AccessPointMigrate;
import cz.tacr.elza.service.vo.NameMigrate;
import cz.tacr.elza.service.vo.SimpleItem;

/**
 * Serviska pro migrace přístupových bodů.
 */
@Service
public class AccessPointMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointMigrationService.class);
    public static final String AP = "AP";

    private final ApRuleRepository ruleRepository;
    private final ResourcePathResolver resourcePathResolver;
    private final ApAccessPointRepository accessPointRepository;
    private final ApNameRepository apNameRepository;
    private final ApDescriptionRepository apDescriptionRepository;
    private final AccessPointDataService apDataService;
    private final AccessPointService accessPointService;
    private final StaticDataService staticDataService;

    private Map<File, GroovyScriptService.GroovyScriptFile> groovyScriptMap = new HashMap<>();

    @Autowired
    public AccessPointMigrationService(final ApRuleRepository ruleRepository,
                                       final ResourcePathResolver resourcePathResolver,
                                       final ApAccessPointRepository accessPointRepository,
                                       final ApNameRepository apNameRepository,
                                       final ApDescriptionRepository apDescriptionRepository,
                                       final AccessPointDataService apDataService,
                                       final AccessPointService accessPointService,
                                       final StaticDataService staticDataService) {
        this.ruleRepository = ruleRepository;
        this.resourcePathResolver = resourcePathResolver;
        this.accessPointRepository = accessPointRepository;
        this.apNameRepository = apNameRepository;
        this.apDescriptionRepository = apDescriptionRepository;
        this.apDataService = apDataService;
        this.accessPointService = accessPointService;
        this.staticDataService = staticDataService;
    }

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.GROOVY)) {
            groovyScriptMap = new HashMap<>();
        }
    }

    /**
     * Provede migraci přístupového bodu na strukturovaný.
     *
     * @param apState přístupový bod
     */
    @AuthMethod(permission = {UsrPermission.Permission.AP_SCOPE_WR_ALL, UsrPermission.Permission.AP_SCOPE_WR})
    public void migrateAccessPoint(@AuthParam(type = AuthParam.Type.AP_STATE) final ApState apState) {
        Validate.notNull(apState, "Přístupový bod musí být vyplněn");

        ApAccessPoint accessPoint = apState.getAccessPoint();
        ApType apType = apState.getApType();
        ApRuleSystem ruleSystem = apType.getRuleSystem();

        List<ApName> names = apNameRepository.findByAccessPoint(accessPoint);
        Map<Integer, ApName> nameMap = names.stream().collect(Collectors.toMap(ApName::getNameId, Function.identity()));
        ApDescription description = apDescriptionRepository.findByAccessPoint(accessPoint);

        File groovyFile = findGroovyFile(ruleSystem);
        AccessPointMigrate result = executeMigrationScript(accessPoint, names, description, groovyFile);

        List<ApUpdateItemVO> apItems = new ArrayList<>();
        for (SimpleItem item : result.getItems()) {
            apItems.add(createUpdateItemVO(item));
        }

        Map<ApName, List<ApUpdateItemVO>> nameItemsMap = new HashMap<>();
        for (NameMigrate name : result.getNames()) {
            ApName apName = nameMap.get(name.getId());
            List<ApUpdateItemVO> itemsVO = nameItemsMap.computeIfAbsent(apName, k -> new ArrayList<>());
            for (SimpleItem item : name.getItems()) {
                itemsVO.add(createUpdateItemVO(item));
            }
        }

        accessPointService.migrateApItems(apState, apItems, nameItemsMap);
    }

    /**
     * Vykonání skriptu pro sestavení strukturovaných položek.
     *
     * @param accessPoint přístupový bod
     * @param names       jména přístupového bodu
     * @param description charakteristika přístupového bodu
     * @param groovyFile  migrační souboru
     * @return výsledek migrace
     */
    protected AccessPointMigrate executeMigrationScript(@AuthParam(type = AuthParam.Type.AP) final ApAccessPoint accessPoint, final List<ApName> names, final ApDescription description, final File groovyFile) {
        GroovyScriptService.GroovyScriptFile groovyScriptFile = groovyScriptMap.get(groovyFile);
        if (groovyScriptFile == null) {
            groovyScriptFile = new GroovyScriptService.GroovyScriptFile(groovyFile);
            groovyScriptMap.put(groovyFile, groovyScriptFile);
        }

        Map<String, Object> input = new HashMap<>();
        input.put(AP, ModelFactory.createApMigrate(accessPoint, names, description));

        return (AccessPointMigrate) groovyScriptFile.evaluate(input);
    }

    /**
     * Konverze itemu pro migraci.
     *
     * Podporují se pouze textové typy!
     *
     * @param si jednoduchý item popisující hodnotu
     * @return item pro migraci
     */
    private ApUpdateItemVO createUpdateItemVO(final SimpleItem si) {
        Validate.notNull(si);
        StaticDataProvider data = staticDataService.getData();

        ItemType type = data.getItemTypeByCode(si.getType());
        if (type == null) {
            throw new ObjectNotFoundException("Typ atributu '" + si.getType() + "' neexistuje", ArrangementCode.ITEM_TYPE_NOT_FOUND)
                    .setId(si.getType());
        }

        RulItemSpec spec = StringUtils.isNotEmpty(si.getSpec()) ? data.getItemSpecByCode(si.getSpec()) : null;

        ApItemVO item;
        DataType dataType = type.getDataType();
        switch (dataType) {
            case STRING:
                item = new ApItemStringVO();
                ((ApItemStringVO) item).setValue(si.getValue());
                break;
            case TEXT:
                item = new ApItemTextVO();
                ((ApItemTextVO) item).setValue(si.getValue());
                break;
            case FORMATTED_TEXT:
                item = new ApItemFormattedTextVO();
                ((ApItemFormattedTextVO) item).setValue(si.getValue());
                break;
            default:
                throw new NotImplementedException("Není implementováno: " + dataType.getCode());
        }

        item.setTypeId(type.getItemTypeId());
        item.setSpecId(spec == null ? null : spec.getItemSpecId());

        ApUpdateItemVO result = new ApUpdateItemVO();
        result.setItem(item);
        result.setUpdateOp(UpdateOp.CREATE);
        return result;
    }

    /**
     * Nalezení scriptu pro migraci.
     *
     * @param ruleSystem systém pravidel
     * @return nalezený groovy soubor pro migraci
     */
    private File findGroovyFile(final ApRuleSystem ruleSystem) {
        ApRule rule = ruleRepository.findByRuleSystemAndRuleType(ruleSystem, ApRule.RuleType.MIGRATE);
        if (rule == null) {
            throw new SystemException("Nebyly nalezeny pravidla migrace", BaseCode.SYSTEM_ERROR)
                    .set("ruleSystemId", ruleSystem.getRuleSystemId());
        }
        RulComponent component = rule.getComponent();
        return resourcePathResolver.getGroovyDir(ruleSystem.getRulPackage())
                .resolve(component.getFilename())
                .toFile();
    }
}
