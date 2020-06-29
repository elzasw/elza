package cz.tacr.elza.service;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.groovy.*;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructuredTypeRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    private StructuredTypeRepository structuredTypeRepository;

    @Autowired
    private StructureDefinitionRepository structureDefinitionRepository;

    @Autowired
    private StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;

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
        for (ApPart part : parts) {
            List<ApPart> childrenParts = new ArrayList<>();
            for (ApPart p : parts) {
                if (p.getParentPart() != null && part.getPartId().equals(p.getParentPart().getPartId())) {
                    childrenParts.add(p);
                }
            }
            groovyParts.add(convertPart(state, part, childrenParts, items));
        }
        return new GroovyAe(apType.getCode(), groovyParts);
    }

    public GroovyResult processGroovy(@NotNull final ApState state,
                                      @NotNull final ApPart part,
                                      @Nullable final List<ApPart> childrenParts,
                                      @NotNull final List<ApItem> items) {
        GroovyPart groovyPart = convertPart(state, part, childrenParts, items);
        return groovyScriptService.process(groovyPart, getGroovyFilePath(groovyPart));
    }

    public GroovyPart convertPart(@NotNull final ApState state,
                                  @NotNull final ApPart part,
                                  @Nullable final List<ApPart> childrenParts,
                                  @NotNull final List<ApItem> items) {
        StaticDataProvider sdp = staticDataService.getData();
        ApPart preferredNamePart = state.getAccessPoint().getPreferredPart();
        boolean preferred = false;
        if (preferredNamePart == null || Objects.equals(preferredNamePart.getPartId(), part.getPartId())) {
            preferred = true;
        }

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
                            intValue = Integer.parseInt(value);
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
                groovyParts.add(convertPart(state, childPart, null, items));
            }
        }

        ApType apType = sdp.getApTypeById(state.getApTypeId());

        return new GroovyPart(apType.getCode(),
                preferred,
                part.getPartType().getCode(),
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
        RulStructuredType structureType = structuredTypeRepository.findByCode(part.getPartTypeCode());
        List<RulStructureExtensionDefinition> structureExtensionDefinitions = structureExtensionDefinitionRepository
                .findByStructureTypeAndDefTypeOrderByPriority(structureType, RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE);

        RulComponent component;
        RulPackage rulPackage;
        RulStructureExtensionDefinition structureExtensionDefinition = getRulStructureExtensionDefinitionByApType(structureExtensionDefinitions, part);

        if (structureExtensionDefinition != null) {
            component = structureExtensionDefinition.getComponent();
            rulPackage = structureExtensionDefinition.getRulPackage();
        } else {
            List<RulStructureDefinition> structureDefinitions = structureDefinitionRepository
                    .findByStructTypeAndDefTypeOrderByPriority(structureType, RulStructureDefinition.DefType.SERIALIZED_VALUE);
            if (structureDefinitions.size() > 0) {
                RulStructureDefinition structureDefinition = structureDefinitions.get(structureDefinitions.size() - 1);
                component = structureDefinition.getComponent();
                rulPackage = structureDefinition.getRulPackage();
            } else {
                throw new SystemException("Strukturovaný typ '" + structureType.getCode() + "' nemá žádný script pro výpočet hodnoty", BaseCode.INVALID_STATE);
            }
        }

        return resourcePathResolver.getGroovyDir(rulPackage)
                .resolve(component.getFilename())
                .toString();
    }

    private RulStructureExtensionDefinition getRulStructureExtensionDefinitionByApType(List<RulStructureExtensionDefinition> structureExtensionDefinitions,
                                                                                       GroovyPart part) {
        if (CollectionUtils.isEmpty(structureExtensionDefinitions)) {
            return null;
        }

        StaticDataProvider std = staticDataService.getData();
        ApType apType = std.getApTypeByCode(part.getAeType());
        do {
            for (RulStructureExtensionDefinition ed : structureExtensionDefinitions) {
                if (ed.getStructuredTypeExtension().getCode().equals(apType.getCode() + "/" + part.getPartTypeCode())) {
                    return ed;
                }
            }
            if (apType.getParentApType() == null) {
                return null;
            } else {
                apType = std.getApTypeById(apType.getParentApTypeId());
            }
        } while (true);

    }
}
