package cz.tacr.elza.validation.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.DataValidationResults;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.validation.ArrDescItemsPostValidator;


/**
 * Validátor povinnosti, opakovatelnosti, hodnot atd pro atributy. Validace probíhá až po uložení všech hodnot.
 * Neslouží k validaci při ukládání jedné hodnoty.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 30.11.2015
 */
@Component
public class ArrDescItemsPostValidatorImpl implements ArrDescItemsPostValidator {

    @Autowired
    private DataRepository arrDataRepository;

    @Autowired
    private RuleManager ruleManager;
    @Autowired
    private DescItemFactory descItemFactory;


    @Override
    public List<DataValidationResult> postValidateNodeDescItems(final ArrLevel level,
                                                            final ArrFindingAidVersion version,
                                                                final Set<String> strategies) {    	

        List<ArrData> levelData;
        if (version.getLockChange() == null) {
            levelData = arrDataRepository.findByNodeAndDeleteChangeIsNull(level.getNode());
        } else {
            levelData = arrDataRepository.findByNodeAndChange(level.getNode(), version.getLockChange());
        }

        List<RulDescItemTypeExt> nodeTypes = ruleManager.getDescriptionItemTypesForNode(
                version.getFindingAidVersionId(), level.getNode().getNodeId(), strategies);

        // Create validator and validate
        Validator validator = new Validator(nodeTypes, levelData, descItemFactory);
        validator.validate();

        return validator.getValidationResultList();
    }



}
