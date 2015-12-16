package cz.tacr.elza.drools;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.vo.DataValidationResult;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import liquibase.util.file.FilenameUtils;


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
    private DescItemValidationRules descItemValidationRules;

    @Autowired
    private DescItemTypesRules descItemTypesRules;

    @Autowired
    private ImpactOfChangesLevelStateRules impactOfChangesLevelStateRules;

    @Autowired
    private ScenarioOfNewLevelRules scenarioOfNewLevelRules;

    /**
     * Cesta adresáře pro konfiguraci pravidel
     * TODO: napojit na spring konfiguraci
     */
    public static String ROOT_PATH = "rules";

    /**
     * Přípona souborů pravidel
     */
    public static final String FILE_EXTENSION = ".drl";


    /**
     * Spustí pravidla nad typy atributů a jejich specifikacema.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     * @param version                verze AP
     * @param strategies             strategie vyhodnocování
     * @return seznam typů atributů odpovídající pravidlům
     */
    public List<RulDescItemTypeExt> executeDescItemTypesRules(final List<RulDescItemTypeExt> rulDescItemTypeExtList,
                                                              final ArrFindingAidVersion version,
                                                              final Set<String> strategies) {

        try {
            return descItemTypesRules
                    .execute(rulDescItemTypeExtList, version.getArrangementType(), version.getRuleSet(), strategies);
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
                                                                           final ArrFindingAidVersion version) {
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
     * @param strategies
     * @return seznam validačních chyb nebo prázdný seznam
     */
    public List<DataValidationResult> executeDescItemValidationRules(final ArrLevel level,
                                                                     final ArrFindingAidVersion version,
                                                                     final Set<String> strategies) {
        try {
            return descItemValidationRules.execute(level, version, strategies);
        } catch (NoSuchFileException e) {
            logger.warn("Neexistuje soubor pro spuštění scriptu." + e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<ScenarioOfNewLevel> executeScenarioOfNewLevelRules(final ArrLevel level,
                                                                   final DirectionLevel directionLevel,
                                                                   final ArrFindingAidVersion version) {
        try {
            return scenarioOfNewLevelRules.execute(level, directionLevel, version);
        } catch (NoSuchFileException e) {
            logger.warn("Neexistuje soubor pro spuštění scriptu." + e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Zkopíruje výchozí pravidla.
     *
     * @param dir složka pro uložení
     */
    private void copyDefaultFromResources(final File dir) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File defaultDir = new File(classLoader.getResource("rules").getFile());
        File[] files = defaultDir.listFiles((dir1, name) -> name.endsWith(FILE_EXTENSION));
        if (files != null) {
            for (File file : files) {
                File fileCopy = new File(
                        dir.toString() + File.separator + FilenameUtils.getBaseName(file.getName()) + FILE_EXTENSION);
                try {
                    Files.copy(file.toPath(), fileCopy.toPath());
                } catch (FileAlreadyExistsException e) {
                    logger.info("Soubor již existuje: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        File dir = new File(ROOT_PATH);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        //copyDefaultFromResources(dir);
    }
}
