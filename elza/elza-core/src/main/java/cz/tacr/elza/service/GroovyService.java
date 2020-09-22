package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.core.data.StructTypeExtension;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.groovy.GroovyAe;
import cz.tacr.elza.groovy.GroovyItem;
import cz.tacr.elza.groovy.GroovyItems;
import cz.tacr.elza.groovy.GroovyPart;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.repository.ApStateRepository;

@Service
public class GroovyService {

    private static GroovyService _self;

    @Autowired
    private PartService partService;

    @Autowired
    private GroovyScriptService groovyScriptService;

    @Autowired
    private AccessPointItemService accessPointItemService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private ApStateRepository apStateRepository;

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @PostConstruct
    public void setStatic() {
        _self = this;
    }

    public GroovyAe convertAe(@NotNull final ApState state,
                              @NotNull final List<ApPart> parts,
                              @NotNull final List<ApItem> items) {
        StaticDataProvider sdp = staticDataService.getData();
        ApType apType = sdp.getApTypeById(state.getApTypeId());
        List<GroovyPart> groovyParts = new ArrayList<>(parts.size());
        ApPart preferredNamePart = state.getAccessPoint().getPreferredPart();
        for (ApPart part : parts) {
            List<ApPart> childrenParts = new ArrayList<>();
            for (ApPart p : parts) {
                if (p.getParentPart() != null && part.getPartId().equals(p.getParentPart().getPartId())) {
                    childrenParts.add(p);
                }
            }

            boolean preferred = preferredNamePart == null || Objects.equals(preferredNamePart.getPartId(), part.getPartId());
            groovyParts.add(convertPart(state, part, childrenParts, items, preferred));
        }
        return new GroovyAe(apType.getCode(), groovyParts);
    }

    public GroovyResult processGroovy(@NotNull final ApState state,
                                      @NotNull final ApPart part,
                                      @Nullable final List<ApPart> childrenParts,
                                      @NotNull final List<ApItem> items,
                                      final boolean preferred) {
        GroovyPart groovyPart = convertPart(state, part, childrenParts, items, preferred);
        return groovyScriptService.process(groovyPart, getGroovyFilePath(groovyPart));
    }

    public GroovyPart convertPart(@NotNull final ApState state,
                                  @NotNull final ApPart part,
                                  @Nullable final List<ApPart> childrenParts,
                                  @NotNull final List<ApItem> items,
                                  final boolean preferred) {
        StaticDataProvider sdp = staticDataService.getData();

        GroovyItems groovyItems = new GroovyItems();
        for (ApItem item : items) {
            ApPart itemPart = item.getPart();
            if (Objects.equals(itemPart.getPartId(), part.getPartId())) {
                ArrData data = item.getData();
                ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());
                String itemTypeCode = itemType.getCode();
                RulItemSpec itemSpec = item.getItemSpec() == null ? null : sdp.getItemSpecById(item.getItemSpecId());
                String spec = itemSpec == null ? null : itemSpec.getName();
                String specCode = itemSpec == null ? null : itemSpec.getCode();

                DataType dataType = DataType.fromCode(data.getDataType().getCode());
                GroovyItem groovyItem;
                switch (dataType) {
                    case BIT: {
                        ArrDataBit dataTmp = (ArrDataBit) data;
                        groovyItem = new GroovyItem(itemTypeCode, spec, specCode, dataTmp.isValue());
                        break;
                    }
                    case STRING: {
                        ArrDataString dataTmp = (ArrDataString) data;
                        groovyItem = new GroovyItem(itemTypeCode, spec, specCode, dataTmp.getValue());
                        break;
                    }
                    case COORDINATES: {
                        ArrDataCoordinates dataTmp = (ArrDataCoordinates) data;
                        groovyItem = new GroovyItem(itemTypeCode, spec, specCode, dataTmp.getFulltextValue());
                        break;
                    }
                    case TEXT: {
                        ArrDataText dataTmp = (ArrDataText) data;
                        groovyItem = new GroovyItem(itemTypeCode, spec, specCode, dataTmp.getValue());
                        break;
                    }
                    case INT: {
                        ArrDataInteger dataTmp = (ArrDataInteger) data;
                        groovyItem = new GroovyItem(itemTypeCode, spec, specCode, dataTmp.getValue());
                        break;
                    }
                    case UNITDATE: {
                        ArrDataUnitdate dataTmp = (ArrDataUnitdate) data;
                        groovyItem = new GroovyItem(itemTypeCode, spec, specCode, dataTmp);
                        break;
                    }
                    case RECORD_REF: {
                        ArrDataRecordRef dataTmp = (ArrDataRecordRef) data;
                        String value;
                        Integer intValue;
                        if (dataTmp.getRecord() != null) {
                            value = dataTmp.getRecord().getPreferredPart().getValue();
                            intValue = dataTmp.getRecordId();
                        } else {
                            value = dataTmp.getBinding().getValue();
                            // pokud se jedná o pouhý odkaz do externího systému (bez lokálního AP)
                            // nastavuje se id z bindingu jako záporný idnetifikátor pro odlišení
                            // identifikátorů z lokálních AP
                            intValue = dataTmp.getBinding().getBindingId() * -1;
                        }

                        groovyItem = new GroovyItem(itemTypeCode, spec, specCode, value, intValue);
                        break;
                    }
                    case ENUM: {
                        groovyItem = new GroovyItem(itemTypeCode, spec, specCode, spec);
                        break;
                    }
                    case URI_REF: {
                        ArrDataUriRef dataTmp = (ArrDataUriRef) data;
                        groovyItem = new GroovyItem(itemTypeCode, spec, specCode, dataTmp.getFulltextValue());
                        break;
                    }
                    default:
                        throw new NotImplementedException("Neimplementovaný typ: " + dataType);
                }
                groovyItems.addItem(itemTypeCode, groovyItem);
            }
        }

        List<GroovyPart> groovyParts = Collections.emptyList();
        if (childrenParts != null) {
            groovyParts = new ArrayList<>();
            for (ApPart childPart : childrenParts) {
                groovyParts.add(convertPart(state, childPart, null, items, false));
            }
        }

        ApType apType = sdp.getApTypeById(state.getApTypeId());
        RulPartType partType = sdp.getPartTypeById(part.getPartTypeId());

        return new GroovyPart(apType.getCode(),
                preferred,
                partType,
                groovyItems,
                groovyParts);
    }


    public List<GroovyAe> findAllParents(@NotNull final Integer entityId,
                                         @NotNull final ItemType itemType) {
        List<GroovyAe> result = new ArrayList<>();
        ApState state = apStateRepository.findLastByAccessPointId(entityId);
        do {
            List<ApPart> parts = partService.findPartsByAccessPoint(state.getAccessPoint());
            List<ApItem> itemsByParts = accessPointItemService.findItemsByParts(parts);
            result.add(convertAe(state, parts, itemsByParts));

            Integer recordId = null;
            for (ApItem aeItem : itemsByParts) {
                if (aeItem.getItemTypeId().equals(itemType.getItemTypeId())) {
                    ArrDataRecordRef data = (ArrDataRecordRef) aeItem.getData();
                    recordId = data.getRecordId();
                    break;
                }
            }
            state = recordId == null ? null : apStateRepository.findLastByAccessPointId(recordId);

        } while (state != null);

        return result;
    }

    public static List<GroovyAe> findParentAe(final Integer recordId, final ItemType itemType) {
        return _self.findAllParents(recordId, itemType);
    }

    public String getGroovyFilePath(GroovyPart part) {
        StaticDataProvider sdp = staticDataService.getData();

        RulComponent component = null;
        RulPackage rulPackage = null;

        StructType structType = sdp.getStructuredTypeByCode(part.getPartTypeCode());
        ApType apType = sdp.getApTypeByCode(part.getAeType());
        while (apType != null) {
            String extCode = apType.getCode() + "/" + part.getPartTypeCode();
            StructTypeExtension structTypeExt = structType.getExtByCode(extCode);
            if (structTypeExt != null) {
                List<RulStructureExtensionDefinition> structureExtensionDefinitions = structTypeExt
                        .getDefsByType(RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE);
                if (structureExtensionDefinitions.size() > 0) {
                    RulStructureExtensionDefinition structureExtensionDefinition = structureExtensionDefinitions
                            .get(structureExtensionDefinitions.size() - 1);
                    component = structureExtensionDefinition.getComponent();
                    rulPackage = structureExtensionDefinition.getRulPackage();
                    break;
                }
            }
            apType = apType.getParentApType();
        }

        // if not found in extension read from base definition
        if (component == null) {
            // extension not exists -> we will find script in standard definition
            List<RulStructureDefinition> structureDefinitions = structType
                    .getDefsByType(RulStructureDefinition.DefType.SERIALIZED_VALUE);
            if (structureDefinitions.size() > 0) {
                RulStructureDefinition structureDefinition = structureDefinitions.get(structureDefinitions.size() - 1);
                component = structureDefinition.getComponent();
                rulPackage = structureDefinition.getRulPackage();
            } else {
                throw new SystemException("Strukturovaný typ '" + structType.getCode()
                        + "' nemá žádný script pro výpočet hodnoty", BaseCode.INVALID_STATE);
            }
        }

        return resourcePathResolver.getGroovyDir(rulPackage)
                .resolve(component.getFilename())
                .toString();
    }
}
