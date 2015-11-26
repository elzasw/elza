package cz.tacr.elza.drools;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;ó
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
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

    /**
     * Cesta adresáře pro konfiguraci pravidel
     * TODO: napojit na spring konfiguraci
     */
    private String path = "rules";

    /**
     * Přípona souborů pravidel
     */
    public final String FILE_EXTENSION = ".drl";

    /**
     * Mapa pravidel
     */
    private Map<String, Rules> rulesMap = new HashMap<>();

    /**
     * Spustí pravidla nad typy atributů a jejich specifikacema.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     * @param version                verze AP
     * @return seznam typů atributů odpovídající pravidlům
     */
    public List<RulDescItemTypeExt> executeDescItemTypesRules(final List<RulDescItemTypeExt> rulDescItemTypeExtList,
                                                              final ArrFindingAidVersion version) {

        RulRuleSet ruleSet = version.getRuleSet();

        DescItemTypesRules rules = (DescItemTypesRules) getRules(ruleSet);

        // pokud ještě neexistuje, vytvořit nové
        if (rules == null) {
            rules = new DescItemTypesRules(Paths.get(getPath(ruleSet, "types")));
            addRule(ruleSet, rules);
        }

        try {
            return rules.execute(rulDescItemTypeExtList, version.getArrangementType());
        } catch (NoSuchFileException e) {
            logger.warn("Neexistuje soubor (" + rules.getRuleFile().toAbsolutePath().getFileName() + ") " + e.getMessage(), e);
            return rulDescItemTypeExtList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Vytvoří cestu k souboru pravidel.
     *
     * @param ruleSet pravidla tvorby AP
     * @param type    typ pravidla
     * @return cesta k souboru
     */
    private String getPath(final RulRuleSet ruleSet, final String type) {
        return path + File.separator + ruleSet.getCode() + "_" + type + FILE_EXTENSION;
    }

    /**
     * Vyhledání pravidel.
     *
     * @param ruleSet pravidla tvorby AP
     * @return nalezené pravidlo
     */
    private Rules getRules(final RulRuleSet ruleSet) {
        return rulesMap.get(ruleSet.getCode());
    }

    /**
     * Přidání nových pravidel.
     *
     * @param ruleSet pravidla tvorby AP
     * @param rules   pravidla
     */
    private void addRule(final RulRuleSet ruleSet, final Rules rules) {
        rulesMap.put(ruleSet.getCode(), rules);
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
        for (File file : files) {
            File fileCopy = new File(dir.toString() + File.separator + FilenameUtils.getBaseName(file.getName()) + FILE_EXTENSION);
            try {
                Files.copy(file.toPath(), fileCopy.toPath());
            } catch (FileAlreadyExistsException e) {
                logger.info("Soubor již existuje: " + e.getMessage());
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        File dir = new File(path);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        copyDefaultFromResources(dir);
    }
}
