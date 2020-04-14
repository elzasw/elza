package cz.tacr.elza.service;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.ActionEvent;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.party.ApConvResult;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Servisní třídy pro osoby.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@Service
public class PartyService {

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ApTypeRepository apTypeRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ItemAptypeRepository itemAptypeRepository;

    private Set<Integer> find(final Integer itemSpecId) {

        Set<Integer> apTypeIds = new HashSet<>();
        if (itemSpecId != null) {
            RulItemSpec spec = itemSpecRepository.getOneCheckExist(itemSpecId);
            apTypeIds.addAll(itemAptypeRepository.findApTypeIdsByItemSpec(spec));
        }
        return apTypeRepository.findSubtreeIds(apTypeIds);
    }

    /**
     * Vytvoří instituci.
     *
     * @param internalCode kód
     * @param institutionType typ instituce
     * @param accessPoint přístupový bod
     * @return neuložená instituce
     */
    public ParInstitution createInstitution(final String internalCode, final ParInstitutionType institutionType, final ApAccessPoint accessPoint) {
        ParInstitution institution = new ParInstitution();
        institution.setInternalCode(internalCode);
        institution.setInstitutionType(institutionType);
        institution.setAccessPoint(accessPoint);
        return institution;
    }

    /**
     * Uloží instituci.
     *
     * @param institution instituce
     * @return uložená instituce
     */
    public ParInstitution saveInstitution(final ParInstitution institution, final boolean notification) {
        Assert.notNull(institution, "Instituce musí být vyplněny");
        if (notification) {
            eventNotificationService.publishEvent(new ActionEvent(EventType.INSTITUTION_CHANGE));
        }
        return institutionRepository.save(institution);
    }

    /**
     * Replace party replaced by party replacement in all usages in JP, Party creators and ParRelation
     */
    /*@Transactional(value = TxType.MANDATORY)
    public void replace(final ParParty replaced, final ParParty replacement) {

        final ApState replacedState = accessPointService.getState(replaced.getAccessPoint());
        final ApState replacementState = accessPointService.getState(replacement.getAccessPoint());

        // Arr
        final List<ArrDescItem> arrItems = descItemRepository.findArrItemByParty(replaced);
        if (!arrItems.isEmpty()) {

            final Collection<Integer> funds = arrItems.stream().map(ArrDescItem::getFundId).collect(Collectors.toSet());
            // Oprávnění
            if (!userService.hasPermission(UsrPermission.Permission.FUND_ARR_ALL)) {
                funds.forEach(i -> {
                    if (!userService.hasPermission(UsrPermission.Permission.FUND_ARR, i)) {
                        throw new SystemException("Uživatel nemá oprávnění na AS.", BaseCode.INSUFFICIENT_PERMISSIONS).set("fundId", i);
                    }
                });
            }

            final Map<Integer, ArrFundVersion> fundVersions = arrangementService.getOpenVersionsByFundIds(funds).stream()
                    .collect(Collectors.toMap(ArrFundVersion::getFundId, Function.identity()));
            // fund to scopes
            final Map<Integer, Set<Integer>> fundIdsToScopes = funds.stream()
                    .collect(Collectors.toMap(Function.identity(), scopeRepository::findIdsByFundId));

            final ArrChange change = arrangementService.createChange(ArrChange.Type.REPLACE_PARTY);
            arrItems.forEach(i -> {
                final ArrDataPartyRef data = new ArrDataPartyRef();
                data.setParty(replacement);
                ArrDescItem im = new ArrDescItem();
                im.setData(data);
                im.setNode(i.getNode());
                im.setCreateChange(i.getCreateChange());
                im.setDeleteChange(i.getDeleteChange());
                im.setDescItemObjectId(i.getDescItemObjectId());
                im.setItemId(i.getDescItemObjectId());
                im.setItemSpec(i.getItemSpec());
                im.setItemType(i.getItemType());
                im.setPosition(i.getPosition());


                Integer fundId = i.getNode().getFundId();
                Set<Integer> fundScopes = fundIdsToScopes.get(fundId);
                if (fundScopes == null) {
                    throw new SystemException("Pro AS neexistují žádné scope.", BaseCode.INVALID_STATE)
                            .set("fundId", fundId);
                } else {
                    if (!fundScopes.contains(replacementState.getScopeId())) {
                        throw new BusinessException("Nelze nahradit osobu v AS jelikož AS nemá scope osoby pomcí které nahrazujeme.", BaseCode.INVALID_STATE)
                                .set("fundId", fundId)
                                .set("scopeId", replacementState.getScopeId());
                    }
                }
                descriptionItemService.updateDescriptionItem(im, fundVersions.get(i.getFundId()), change);
            });
        }

        // Registry replace
        accessPointService.replace(replacedState, replacementState);

        // we have to replace relations
        replaceRecordInRelations(replacedState, replacementState);
    }*/
}
