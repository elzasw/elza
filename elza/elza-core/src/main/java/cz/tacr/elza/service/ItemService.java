package cz.tacr.elza.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.tacr.elza.common.db.HibernateUtils;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.ItemAptypeRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.utils.MapyCzUtils;

/**
 * Serviska pro správu hodnot atributů.
 */
@Service
public class ItemService {

    private final Logger log = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private StructuredObjectRepository structureDataRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    @Autowired
    private ApAccessPointRepository recordRepository;

    @Autowired
    private ItemAptypeRepository itemAptypeRepository;

    @Autowired
    private ApTypeRepository registerTypeRepository;

    @Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private EntityManager em;

    @SuppressWarnings("unchecked")
    @Transactional(TxType.MANDATORY)
    public ArrOutputItem moveItem(ArrOutputItem item, ArrChange change, int position) {
        Validate.isTrue(em.contains(item));
        Validate.notNull(change);

        item.setDeleteChange(change); // save by commit

        em.flush();

        ArrOutputItem newItem = item.makeCopy();
        newItem.setCreateChange(change);
        newItem.setPosition(position);
        newItem.setDeleteChange(null);
        newItem.setItemId(null);

        ArrData newData = ArrData.makeCopyWithoutId(HibernateUtils.unproxy(item.getData()));
        newItem.setData(newData);

        em.persist(newData);
        em.persist(newItem);

        return newItem;
        }

    /**
     * Kontrola typu a specifikace.
     *
     * @param fundContext - kontext fondu
     * @param arrItem     - hodnota atributu
     */
    @Transactional(TxType.MANDATORY)
    public void checkValidTypeAndSpec(@NotNull final FundContext fundContext,
                                      @NotNull final ArrItem arrItem) {

        Integer itemTypeId = arrItem.getItemTypeId();
        Validate.notNull(itemTypeId, "Invalid description item type: " + itemTypeId);

        ItemType itemType = fundContext.getSdp().getItemTypeById(itemTypeId);
        Validate.notNull(itemType, "Invalid description item type: " + itemTypeId);

        // extra check for data
        ArrData data = HibernateUtils.unproxy(arrItem.getData());
        RulItemType rulItemType = itemType.getEntity();

        // check if defined specification
        Integer itemSpecId = arrItem.getItemSpecId();

        RulItemSpec rulItemSpec = null;

        if (itemType.hasSpecifications()) {

            if (data == null && itemType.getDataType() == DataType.ENUM) {
                if (itemSpecId != null) {
                    throw new BusinessException("Při neexistují data specifikaci by neměla být",
                            ArrangementCode.ITEM_SPEC_FOUND).level(Level.WARNING);
                }
                int count = arrItem.getItemId() == null?
                        descItemRepository.countByNodeIdAndItemTypeId(arrItem.getNodeId(), arrItem.getItemTypeId()) :
                        descItemRepository.countByNodeIdAndItemTypeIdAndNotItemId(arrItem.getNodeId(), arrItem.getItemTypeId(), arrItem.getItemId());
                if (count > 0) {
                    throw new BusinessException("V jednom ArrNode může existovat pouze jeden nedefinovaný ArrItem",
                                                ArrangementCode.ALREADY_INDEFINABLE).level(Level.WARNING);
                }
            } else {
                if (itemSpecId == null) {
                    throw new BusinessException("Pro typ atributu je nutné specifikaci vyplnit",
                            ArrangementCode.ITEM_SPEC_NOT_FOUND).level(Level.WARNING);
                }
                rulItemSpec = itemType.getItemSpecById(itemSpecId);
                if (rulItemSpec == null) {
                    throw new SystemException("Specifikace neodpovídá typu hodnoty atributu");
                }
            }
        } else {
            if (itemSpecId != null) {
                throw new BusinessException("Pro typ atributu nesmí být specifikace vyplněná",
                        ArrangementCode.ITEM_SPEC_FOUND).level(Level.WARNING);
            }
        }

        if (data != null && !arrItem.isUndefined()) {
            // check record_ref
            if (itemType.getDataType().equals(DataType.RECORD_REF)) {
                ArrDataRecordRef recordRef = (ArrDataRecordRef) data;
                checkRecordRef(fundContext, recordRef, rulItemType, rulItemSpec);
            }
        }

        // item length control
        checkItemLengthLimit(rulItemType, data);
    }

    /**
     * Kontrola délky řetězce
     *
     * @param rulItemType
     * @param data
     */
    public void checkItemLengthLimit(RulItemType rulItemType, ArrData data) {
        if (DataType.fromId(rulItemType.getDataTypeId()) == DataType.STRING && rulItemType.getStringLengthLimit() != null) {
            ArrDataString dataString = (ArrDataString) data;
            if(dataString.getStringValue().length() > rulItemType.getStringLengthLimit()) {
                throw new BusinessException("Délka řetězce je delší než maximální povolená : " + rulItemType.getStringLengthLimit(), BaseCode.INVALID_LENGTH);
            }
        }
    }

    private void checkRecordRef(FundContext fundContext,
                                ArrDataRecordRef dataRecordRef,
                                RulItemType rulItemType,
                                RulItemSpec rulItemSpec) {
        ApAccessPoint apAccessPoint = dataRecordRef.getRecord();
        ApState apState = stateRepository.findLastByAccessPoint(apAccessPoint);

        // TODO: refactor and use static data
        List<Integer> apTypeIds = null;
        if (rulItemSpec != null) {
            apTypeIds = itemAptypeRepository.findApTypeIdsByItemSpec(rulItemSpec);
        } else {
            apTypeIds = itemAptypeRepository.findApTypeIdsByItemType(rulItemType);
        }
        Set<Integer> apTypeIdTree = registerTypeRepository.findSubtreeIds(apTypeIds);

        // kontrola typu třídy
        if (!apTypeIdTree.contains(apState.getApTypeId())) {
            log.error("Class of archival entity is incorrect, dataId: {}, accessPointId: {}, rulItemType: {}, rulItemSpec: {}, apTypeId: {}",
                      dataRecordRef.getDataId(),
                      apAccessPoint.getAccessPointId(),
                      rulItemType.getCode(),
                      (rulItemSpec != null) ? rulItemSpec.getCode() : null,
                      "apTypeId", apState.getApTypeId());

            throw new BusinessException("Třída přístupového bodu neodpovídá požadavkům prvku popisu.",
                    RegistryCode.FOREIGN_ENTITY_INVALID_SUBTYPE)
                            .set("dataId", dataRecordRef.getDataId())
                            .set("accessPointId", apAccessPoint.getAccessPointId())
                            .set("rulItemType", rulItemType.getCode())
                            .set("rulItemSpec", (rulItemSpec != null) ? rulItemSpec.getCode() : null)
                            .set("apTypeId", apState.getApTypeId())
                            .level(Level.WARNING);
        }

        // kontrola scope entity
        if (!fundContext.getScopes().contains(apState.getScope().getScopeId())) {
            log.error("Archival entity has invalid scope, dataId: {}, accessPointId: {}, scopeIds: {}, AF scopeId: {}",
                      dataRecordRef.getDataId(),
                      apAccessPoint.getAccessPointId(),
                      fundContext.getScopes(),
                      apState.getScope().getScopeId());

            throw new BusinessException("Archivní entita má nevhodné scope.",
                                        RegistryCode.INVALID_ENTITY_SCOPE)
                                                .set("dataId", dataRecordRef.getDataId())
                                                .set("accessPointId", apAccessPoint.getAccessPointId())
                                                .set("scopeIds", fundContext.getScopes())
                                                .set("scopeId", apState.getScope().getScopeId())
                                                .level(Level.WARNING);
        }

    }

    public ApAccessPoint getApProxy(Integer apId) {
        return recordRepository.getOne(apId);
    }

    /**
     * Donačítá položky, které jsou typově jako odkaz, podle ID.
     *
     * @param dataItems seznam položek, které je potřeba donačíst podle ID návazných entit
     */
    public void refItemsLoader(final Collection<? extends ArrItem> dataItems) {

        // mapy pro naplnění ID entit
        Map<Integer, ArrDataStructureRef> structureMap = new HashMap<>();
        Map<Integer, ArrDataFileRef> fileMap = new HashMap<>();
        Map<Integer, ArrDataRecordRef> recordMap = new HashMap<>();

        // prohledávám pouze entity, které mají návazné data
        for (ArrItem dataItem : dataItems) {
            ArrData data = HibernateUtils.unproxy(dataItem.getData());
            if (data != null) {
                if (data instanceof ArrDataStructureRef) {
                    ArrDataStructureRef structDataRef = (ArrDataStructureRef) data;
                    structureMap.put(structDataRef.getStructuredObjectId(), structDataRef);
                } else if (data instanceof ArrDataFileRef) {
                    ArrDataFileRef fileRef = (ArrDataFileRef) data;
                    fileMap.put(fileRef.getFileId(), fileRef);
                } else if (data instanceof ArrDataRecordRef) {
                    ArrDataRecordRef recordRef = (ArrDataRecordRef) data;
                    recordMap.put(recordRef.getRecordId(), recordRef);
                }
            }
        }

        Set<Integer> structureDataIds = structureMap.keySet();
        List<ArrStructuredObject> structureDataEntities = structureDataRepository.findAllById(structureDataIds);
        for (ArrStructuredObject structureDataEntity : structureDataEntities) {
            structureMap.get(structureDataEntity.getStructuredObjectId()).setStructuredObject(structureDataEntity);
        }

        Set<Integer> fileIds = fileMap.keySet();
        List<ArrFile> fileEntities = fundFileRepository.findAllById(fileIds);
        for (ArrFile fileEntity : fileEntities) {
            ArrDataFileRef ref = fileMap.get(fileEntity.getFileId());
            if (ref != null) {
                ref.setFile(fileEntity);
            }
        }

        Set<Integer> recordIds = recordMap.keySet();
        List<ApAccessPoint> recordEntities = recordRepository.findAllById(recordIds);
        for (ApAccessPoint recordEntity : recordEntities) {
            recordMap.get(recordEntity.getAccessPointId()).setRecord(recordEntity);
        }
    }

    /**
     * Normalizuje vstupní hodnotu souřadnic z klienta.
     *
     * @param value původní vstupní řetězec souřadnic
     * @return normalizovaná hodnota souřadnic
     */
    public String normalizeCoordinates(final String value) {
        if (value == null) {
            return null;
        }
        String result = value;
        if (MapyCzUtils.isFromMapyCz(value)) {
            result = MapyCzUtils.transformToWKT(value);
        }
        return result;
    }

    /**
     * Třída pro požadované údaje o fondu (AS)
     */
    public static class FundContext {

        private final Set<Integer> scopes;

        private final ArrFund fund;

        private final ArrFundVersion fundVersion;

        private final StaticDataProvider sdp;

        public FundContext(final Set<Integer> scopes,
                           final ArrFundVersion fundVersion,
                           final StaticDataProvider sdp) {
            this.scopes = scopes;
            this.fundVersion = fundVersion;
            this.fund = fundVersion.getFund();
            this.sdp = sdp;
        }

        public static FundContext newInstance(final ArrFundVersion fundVersion,
                                              final ArrangementService service,
                                              final StaticDataProvider sdp) {
            Set<Integer> scopes = service.findAllConnectedScopeByFund(fundVersion.getFund());
            return new FundContext(scopes, fundVersion, sdp);
        }

        public Set<Integer> getScopes() {
            return scopes;
        }

        public ArrFund getFund() {
            return fund;
        }

        public ArrFundVersion getFundVersion() {
            return fundVersion;
        }

        public StaticDataProvider getSdp() {
            return sdp;
        }
    }
}
