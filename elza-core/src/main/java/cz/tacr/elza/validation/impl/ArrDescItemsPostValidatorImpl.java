package cz.tacr.elza.validation.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.service.RuleService;
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
    private DescItemFactory descItemFactory;

    @Autowired
    private RuleService ruleService;


    @Override
    public List<DataValidationResult> postValidateNodeDescItems(final ArrLevel level,
                                                                final ArrFundVersion version) {

        List<ArrData> levelData;
        if (version.getLockChange() == null) {
            levelData = arrDataRepository.findByNodeAndDeleteChangeIsNull(level.getNode());
        } else {
            levelData = arrDataRepository.findByNodeAndChange(level.getNode(), version.getLockChange());
        }


        List<RulDescItemTypeExt> nodeTypes = ruleService.getDescriptionItemTypes(version, level.getNode());

        // Create validator and validate
        Validator validator = new Validator(nodeTypes, levelData, descItemFactory);
        validator.validate();

        return validator.getValidationResultList();
    }



}
