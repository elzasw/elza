package cz.tacr.elza.drools;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrFundVersion;

import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulItemTypeExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;


/**
 * Serviska má na starosti spouštění pravidel přes Drools.
 *
 * @author Martin Šlapa
 * @since 26.11.2015
 */
@Service
public class RulesExecutor implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());


    @Autowired
    private ValidationRules descItemValidationRules;

    @Autowired
    private DescItemTypesRules descItemTypesRules;

    @Autowired
    private OutputItemTypesRules outputItemTypesRules;

    @Autowired
    private ChangeImpactRules impactOfChangesLevelStateRules;

    @Autowired
    private ScenarioOfNewLevelRules scenarioOfNewLevelRules;

    /**
     * Cesta adresáře pro konfiguraci pravidel
     */
    @Value("${elza.rulesDir}")
    private String rootPath;

    /**
     * Přípona souborů pravidel
     */
    public static final String FILE_EXTENSION = ".drl";


    /**
     * Spustí pravidla nad typy atributů a jejich specifikacema.
     *
     * @param level                  checked level
     * @param rulDescItemTypeExtList seznam všech atributů
     * @param version                verze AP
     * @return seznam typů atributů odpovídající pravidlům
     */
    public List<RulItemTypeExt> executeDescItemTypesRules(final ArrLevel level,
                                                          final List<RulItemTypeExt> rulDescItemTypeExtList,
                                                          final ArrFundVersion version) {

        try {
            return descItemTypesRules
                    .execute(level, version, rulDescItemTypeExtList);
        } catch (NoSuchFileException e) {
            logger.warn("Neexistuje soubor pro spuštění scriptu." + e.getMessage(), e);
            return rulDescItemTypeExtList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Spustí pravidla nad typy atributů a jejich specifikacema.
     *
     * @param outputDefinition       definice výstupu
     * @param rulDescItemTypeExtList seznam všech atributů
     * @return seznam typů atributů odpovídající pravidlům
     */
    public List<RulItemTypeExt> executeOutputItemTypesRules(final ArrOutputDefinition outputDefinition,
                                                          final List<RulItemTypeExt> rulDescItemTypeExtList) {
        try {
            return outputItemTypesRules.execute(outputDefinition, rulDescItemTypeExtList);
        } catch (NoSuchFileException e) {
            logger.warn("Neexistuje soubor pro spuštění scriptu." + e.getMessage(), e);
            return rulDescItemTypeExtList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Spustí pravidla pro dopad změn na stavy uzlu.
     *
     * @param createDescItem    hodnoty atributů k vytvoření
     * @param updateDescItem    hodnoty atributů k upravení
     * @param deleteDescItem    hodnoty atributů ke smazání
     * @param nodeTypeOperation typ operace
     * @param version           verze AP
     * @return seznam dopadů
     */
    public Set<RelatedNodeDirection> executeImpactOfChangesLevelStateRules(final List<ArrDescItem> createDescItem,
                                                                           final List<ArrDescItem> updateDescItem,
                                                                           final List<ArrDescItem> deleteDescItem,
                                                                           final NodeTypeOperation nodeTypeOperation,
                                                                           final ArrFundVersion version) {
        try {
            return impactOfChangesLevelStateRules
                    .execute(createDescItem, updateDescItem, deleteDescItem, nodeTypeOperation, version.getRuleSet());
        } catch (NoSuchFileException e) {
            logger.warn("Neexistuje soubor pro spuštění scriptu." + e.getMessage(), e);
            return new HashSet<>();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Vyvolání validace hodnot atributů daného uzlu.
     *
     * @param level   validovaný uzel
     * @param version verze uzlu
     * @return seznam validačních chyb nebo prázdný seznam
     */
    public List<DataValidationResult> executeDescItemValidationRules(final ArrLevel level,
                                                                     final ArrFundVersion version) {
        try {
            return descItemValidationRules.execute(level, version);
        } catch (NoSuchFileException e) {
            logger.warn("Neexistuje soubor pro spuštění scriptu." + e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<ScenarioOfNewLevel> executeScenarioOfNewLevelRules(final ArrLevel level,
                                                                   final DirectionLevel directionLevel,
                                                                   final ArrFundVersion version) {
        try {
            return scenarioOfNewLevelRules.execute(level, directionLevel, version);
        } catch (NoSuchFileException e) {
            logger.warn("Neexistuje soubor pro spuštění scriptu." + e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        File dir = new File(rootPath);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        //copyDefaultFromResources(dir);
    }

    public String getRootPath() {
        return rootPath;
    }
}
