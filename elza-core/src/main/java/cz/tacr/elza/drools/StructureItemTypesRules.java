package cz.tacr.elza.drools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureDefinition.DefType;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;


/**
 * Zpracování pravidel typů parametrů pro strukturovaný typ.
 *
 * @since 03.11.2017
 */
@Component
public class StructureItemTypesRules extends Rules {

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;

    /**
     * Spuštění zpracování pravidel.
     *
     * @param structTypeId
     *            typ
     * @param rulDescItemTypeExtList
     *            seznam všech atributů
     * @param structureItems
     *            seznam položek strukturovaného datového typu
     * @return seznam typů atributů odpovídající pravidlům
     * @throws IOException
     */
    public synchronized List<RulItemTypeExt> execute(final Integer structTypeId,
                                                     final List<RulItemTypeExt> rulDescItemTypeExtList,
                                                     final ArrFund fund,
                                                     final List<ArrStructuredItem> structureItems)
            throws IOException {

        LinkedList<Object> facts = new LinkedList<>();
        facts.addAll(ModelFactory.createStructuredItems(structureItems));
        facts.addAll(rulDescItemTypeExtList);

        StaticDataProvider sdp = staticDataService.getData();

        StructType st = sdp.getStructuredTypeById(structTypeId);

        List<RulStructureDefinition> rulStructureDefinitions = st
                .getDefsByType(DefType.ATTRIBUTE_TYPES);

        for (RulStructureDefinition rulStructureDefinition : rulStructureDefinitions) {
            // TODO: Consider using structureType in getDroolsFile?
            Path path = resourcePathResolver.getDroolsFile(rulStructureDefinition);

            KieSession session = createKieSession(path);
            executeSession(session, facts);
        }

        List<RulStructureExtensionDefinition> rulStructureExtensionDefinitions = structureExtensionDefinitionRepository
                .findByStructureTypeAndDefTypeAndFundOrderByPriority(st.getStructuredType(),
                                                                     RulStructureExtensionDefinition.DefType.ATTRIBUTE_TYPES,
                                                                     fund);

        sortDefinitionByPackages(rulStructureExtensionDefinitions);

        for (RulStructureExtensionDefinition rulStructureExtensionDefinition : rulStructureExtensionDefinitions) {
            // TODO: Consider using structureType in getDroolsFile?
            Path path = resourcePathResolver.getDroolsFile(rulStructureExtensionDefinition);

            KieSession session = createKieSession(path);
            executeSession(session, facts);
        }

        return rulDescItemTypeExtList;
    }

}
