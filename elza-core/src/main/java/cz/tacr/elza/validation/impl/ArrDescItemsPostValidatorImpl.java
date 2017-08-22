package cz.tacr.elza.validation.impl;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.drools.ValidationRules;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.validation.ArrDescItemsPostValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


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
    private DescItemRepository descItemRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private ValidationRules validationRules;

    @Override
    public List<DataValidationResult> postValidateNodeDescItems(final ArrLevel level,
                                                                final ArrFundVersion version) {

        List<ArrDescItem> descItems;
        if (version.getLockChange() == null) {
            descItems = descItemRepository.findByNodeAndDeleteChangeIsNullFetch(level.getNode());
        } else {
            descItems = descItemRepository.findByNodeAndChange(level.getNode(), version.getLockChange());
        }

        List<RulItemTypeExt> nodeTypes = ruleService.getDescriptionItemTypes(version, level.getNode());

        // Create validator and validate
        Validator validator = new Validator(nodeTypes, descItems, descItemFactory);
        validator.validate();

        List<DataValidationResult> validationResultList = validator.getValidationResultList();
        validationRules.finalizeValidationResults(validationResultList);
        return validationResultList;
    }

}
