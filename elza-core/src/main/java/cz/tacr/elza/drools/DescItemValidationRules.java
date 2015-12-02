package cz.tacr.elza.drools;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.drools.model.VOLevel;
import cz.tacr.elza.drools.service.ScriptModelFactory;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;


/**
 * Zpracování pravidel pro validaci parametrů uzlu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 1.12.2015
 */

@Component
public class DescItemValidationRules extends Rules {


    @Autowired
    private ScriptModelFactory scriptModelFactory;
    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    /**
     * Spustí validaci atributů.
     *
     * @param level   level, na kterým spouštíme validaci
     * @param version verze, do které spadá uzel
     * @return seznam validačních chyb nebo prázdný seznam
     */
    public synchronized List<DataValidationResult> execute(final ArrLevel level, final ArrFindingAidVersion version)
            throws Exception {

        StatelessKieSession session = createNewStatelessKieSession(version.getRuleSet());
        List<DataValidationResult> result = new LinkedList<>();

        session.setGlobal("results", result);
        session.setGlobal("arrType", version.getArrangementType());

        VOLevel voLevel = scriptModelFactory.createLevelStructure(level, version);

        execute(session, version.getRuleSet(), Arrays.asList(voLevel));
        finalizeValidationResults(result);

        return result;
    }

    /**
     * Donačte typy a atributy podle jejich id, která se zadávají ve scriptu.
     *
     * @param validationResults seznam validačních chyb
     */
    private void finalizeValidationResults(final List<DataValidationResult> validationResults) {

        for (DataValidationResult validationResult : validationResults) {
            switch (validationResult.getResultType()) {

                case MISSING:
                    String missingTypeCode = validationResult.getTypeCode();
                    if (missingTypeCode == null) {
                        throw new IllegalStateException("Neni vyplnen kod chybejiciho typu.");
                    }
                    validationResult.setType(descItemTypeRepository.getOneByCode(missingTypeCode));
                    break;
                case ERROR:
                    Integer descItemId = validationResult.getDescItemId();
                    if (descItemId == null) {
                        throw new IllegalStateException("Neni vyplneno id chybneho atributu.");
                    }
                    validationResult.setDescItem(descItemRepository.findOne(descItemId));
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Neznamy typ vysledku validace " + validationResult.getResultType().name());
            }
        }
    }

    @Override
    protected String getFileName() {
        return "descItemValidation" + RulesExecutor.FILE_EXTENSION;
    }
}
