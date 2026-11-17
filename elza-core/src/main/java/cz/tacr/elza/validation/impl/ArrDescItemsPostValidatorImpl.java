package cz.tacr.elza.validation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.repository.ApStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.projection.DeletedNodeInfo;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.drools.ValidationRules;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.DescriptionItemServiceInternal;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.validation.ArrDescItemsPostValidator;


/**
 * Validátor povinnosti, opakovatelnosti, hodnot atd pro atributy. Validace probíhá až po uložení všech hodnot.
 * Neslouží k validaci při ukládání jedné hodnoty.
 *
 */
@Component
public class ArrDescItemsPostValidatorImpl implements ArrDescItemsPostValidator {

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private ValidationRules validationRules;

	@Autowired
	DescriptionItemServiceInternal arrangementServiceInternal;

	@Autowired
	NodeCacheService nodeCache;

	@Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Override
    public List<DataValidationResult> postValidateNodeDescItems(final ArrLevel level,
                                                                final ArrFundVersion version) {

        List<ArrDescItem> descItems;
        if (version.getLockChange() == null) {
			// read node from cache
			RestoredNode restoredNode = nodeCache.getNode(level.getNodeId());
			if (restoredNode == null) {
				throw new ObjectNotFoundException("Nebyla nalezena JP s ID=" + level.getNodeId(),
				        ArrangementCode.NODE_NOT_FOUND)
				                .set("id", level.getNodeId());
			}
			descItems = restoredNode.getDescItems();
        } else {
			descItems = arrangementServiceInternal.getDescItems(version.getLockChange(), level.getNode());
        }
        
        // Read entities
        Map<Integer, ApState> apStatesMap = Collections.emptyMap();
        Set<Integer> apIds = new HashSet<>();
        Set<String> linkedNodeUuids = new HashSet<>();
        for (ArrDescItem descItem : descItems) {
			if(descItem.getData() == null) {
				continue;
			}
			ArrData data = HibernateUtils.unproxy(descItem.getData());
			if(data instanceof ArrDataRecordRef) {
				apIds.add (  ((ArrDataRecordRef) data).getRecordId() );
			} else if (data instanceof ArrDataUriRef) {
                String linkedNodeUuid = DescItemFactory.getLinkedNodeUuid((ArrDataUriRef) data);
                if (linkedNodeUuid != null) {
                    linkedNodeUuids.add(linkedNodeUuid);
                }
			}
		}
        Map<String, DeletedNodeInfo> deletedLinkedNodes = findDeletedNodes(linkedNodeUuids);
        if(apIds.size()>0) {
        	// read states
			List<ApState> apStates = stateRepository.findLastByAccessPointIds(apIds);
			// create map of apStates with id as key
			apStatesMap = apStates.stream().collect(Collectors.toMap(ApState::getAccessPointId, Function.identity()));
        }

        List<RulItemTypeExt> nodeTypes = ruleService.getDescriptionItemTypes(version, level.getNode());

        // create validator and validate
        Validator validator = new Validator(nodeTypes, descItems, descItemFactory, apStatesMap,
                deletedLinkedNodes);
        validator.validate();

        List<DataValidationResult> validationResultList = validator.getValidationResultList();
		validationRules.finalizeValidationResults(validationResultList, version.getRuleSetId());
        return validationResultList;
    }

    /**
     * Deleted units of description among the link targets, with their last deletion.
     *
     * @param uuids UUIDs of the link targets
     * @return UUID of a deleted unit -> its last deletion
     */
    private Map<String, DeletedNodeInfo> findDeletedNodes(final Set<String> uuids) {
        if (uuids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DeletedNodeInfo> lastDeletions = levelRepository.findLastDeletions(uuids);

        Map<String, DeletedNodeInfo> lastDeletionByUuid = new HashMap<>();
        for (DeletedNodeInfo lastDeletion : lastDeletions) {
            // two rows of one unit are levels deleted by the same change: either describes it
            lastDeletionByUuid.putIfAbsent(lastDeletion.uuid(), lastDeletion);
        }
        return lastDeletionByUuid;
    }

}
