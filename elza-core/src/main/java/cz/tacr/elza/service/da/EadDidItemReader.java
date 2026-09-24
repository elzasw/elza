package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import org.archivists.ead3.schema.Did;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.GroovyScriptService;

/**
 * Reads the items of a unit of description from its {@code <did>} in EAD.
 *
 * The IMPORT_DA script of the rules decides the item type and specification of each element,
 * {@link DidElementConverters} its value. Inherited values are not read - the unit of
 * description they are inherited from carries them. Nothing is stored: the items are meant for
 * the import of the archival description from a package, which decides where they go.
 */
@Component
public class EadDidItemReader {

    private static final Logger logger = LoggerFactory.getLogger(EadDidItemReader.class);

    private static final String IMPORT_DA = "IMPORT_DA";

    /** One item read from EAD; the data are not stored yet. */
    public record EadItem(RulItemType itemType, @Nullable RulItemSpec itemSpec, ArrData data) {
    }

    private final GroovyScriptService groovyScriptService;
    private final StaticDataService staticDataService;
    private final ResourcePathResolver resourcePathResolver;

    public EadDidItemReader(GroovyScriptService groovyScriptService, StaticDataService staticDataService,
                            ResourcePathResolver resourcePathResolver) {
        this.groovyScriptService = groovyScriptService;
        this.staticDataService = staticDataService;
        this.resourcePathResolver = resourcePathResolver;
    }

    /**
     * @param unitId id of the unit of description, for the error message
     * @param eadHref path of the EAD inside the package, for the error message
     * @throws AipProblemException when an element is not written the way it can be read; the
     *             problem names the file, the unit of description and the element
     * @throws SystemException when the rules map an element wrongly
     */
    public List<EadItem> read(Did did, String unitId, String eadHref) {
        List<EadItem> items = new ArrayList<>();
        for (Object element : did.getMDid()) {
            if (!DidElementConverters.isSupported(element) || DidElementConverters.isInherited(element)) {
                continue;
            }
            DidElementConverters.Mapping mapping = resolveMapping(element);
            if (mapping == null) {
                continue;
            }
            ArrData data;
            try {
                data = DidElementConverters.convert(element, mapping);
            } catch (EadContentException e) {
                throw AipProblemException.metadata("Inherentní archivní popis v souboru '" + eadHref
                        + "' obsahuje u jednotky popisu '" + unitId + "' element <" + elementName(element)
                        + ">, který nelze převzít: " + e.getMessage() + ".", eadHref, e);
            }
            if (data != null) {
                items.add(new EadItem(mapping.itemType(), mapping.itemSpec(), data));
            }
        }
        return items;
    }

    /**
     * @return the item type and specification the rules map the supported element to; null
     *         when the element is not taken over
     */
    @Nullable
    private DidElementConverters.Mapping resolveMapping(Object element) {
        String className = element.getClass().getSimpleName();
        String localType = DidElementConverters.localType(element);
        Object result = groovyScriptService.processImportDa(className, localType, getGroovyFilePath());

        String itemTypeCode;
        String itemSpecCode = null;
        if (result == null) {
            if (localType != null) {
                logger.warn("Element <{}> s localtype={} pravidla nepřebírají", elementName(element), localType);
            }
            return null;
        } else if (result instanceof String code) {
            itemTypeCode = code;
        } else if (result instanceof Map<?, ?> map) {
            itemTypeCode = Objects.toString(map.get("itemType"), null);
            itemSpecCode = Objects.toString(map.get("itemSpec"), null);
        } else {
            throw new SystemException("Skript IMPORT_DA vrátil pro element " + className
                    + " nepodporovaný výsledek: " + result, BaseCode.INVALID_STATE);
        }

        StaticDataProvider sdp = staticDataService.getData();
        RulItemType itemType = sdp.getItemType(itemTypeCode);
        RulItemSpec itemSpec = null;
        if (itemSpecCode != null) {
            itemSpec = sdp.getItemTypeByCode(itemTypeCode).getItemSpecByCode(itemSpecCode);
            if (itemSpec == null) {
                throw new SystemException("Skript IMPORT_DA přiřazuje elementu " + className + " specifikaci "
                        + itemSpecCode + ", která nepatří k prvku popisu " + itemTypeCode, BaseCode.INVALID_STATE);
            }
        } else if (Boolean.TRUE.equals(itemType.getUseSpecification())) {
            throw new SystemException("Skript IMPORT_DA přiřazuje elementu " + className + " prvek popisu "
                    + itemTypeCode + " bez specifikace, kterou prvek vyžaduje", BaseCode.INVALID_STATE);
        }
        return new DidElementConverters.Mapping(itemType, itemSpec);
    }

    private static String elementName(Object element) {
        return element.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    private String getGroovyFilePath() {
        StaticDataProvider sdp = staticDataService.getData();
        StructType structType = sdp.getStructuredTypeByCode(IMPORT_DA);

        List<RulStructureDefinition> structureDefinitions = structType
                .getDefsByType(RulStructureDefinition.DefType.SERIALIZED_VALUE);
        if (structureDefinitions.isEmpty()) {
            throw new SystemException("Strukturovaný typ '" + structType.getCode()
                    + "' nemá žádný script pro výpočet hodnoty", BaseCode.INVALID_STATE);
        }
        RulStructureDefinition structureDefinition = structureDefinitions.get(structureDefinitions.size() - 1);
        RulComponent component = structureDefinition.getComponent();
        RulPackage rulPackage = structureDefinition.getRulPackage();

        return resourcePathResolver.getGroovyDir(rulPackage)
                .resolve(component.getFilename())
                .toString();
    }
}
