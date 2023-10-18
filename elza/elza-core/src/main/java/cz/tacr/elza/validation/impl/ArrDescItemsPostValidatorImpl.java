package cz.tacr.elza.validation.impl;

import java.util.List;

import cz.tacr.elza.repository.ApStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.drools.ValidationRules;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.DescItemRepository;
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
			//Node node = restoredNode.getNode();

			descItems = restoredNode.getDescItems();
        } else {
			descItems = arrangementServiceInternal.getDescItems(version.getLockChange(), level.getNode());
        }

        List<RulItemTypeExt> nodeTypes = ruleService.getDescriptionItemTypes(version, level.getNode());

        // create validator and validate
        Validator validator = new Validator(nodeTypes, descItems, descItemFactory, stateRepository);
        validator.validate();

        List<DataValidationResult> validationResultList = validator.getValidationResultList();
		validationRules.finalizeValidationResults(validationResultList, version.getRuleSetId());
        return validationResultList;
    }

}
