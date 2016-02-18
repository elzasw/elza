package cz.tacr.elza.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.repository.ComplementTypeRepository;
import liquibase.util.file.FilenameUtils;


/**
 * Servisní třída pro výpočet v groovy scriptech.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 28.01.2016
 */
@Service
public class GroovyScriptService {

    private Log logger = LogFactory.getLog(this.getClass());

    /**
     * Výchozí groovy script pro vytvoření rejstříkového hesla.
     */
    @Value("classpath:/script/groovy/createRecord.groovy")
    private Resource createRecordDefaultResource;

    /**
     * Načtený pro vytvoření rejstříkového hesla.
     */
    private Resource createRecordResource;

    private static final String CREATE_RECORD_FILE = "createRecord.groovy";

    @Value("${elza.groovy.groovyDir}")
    private String groovyScriptDir;

    @Autowired
    private ScriptEvaluator groovyScriptEvaluator;

    @Autowired
    private ComplementTypeRepository complementTypeRepository;

    /**
     * Zavolá script pro vytvoření rejstříkového hesla z osoby.
     *
     * @param party osoba
     * @return vytvořené rejstříkové heslo s nastavenými variantními hesly
     */
    public RegRecord getRecordFromGroovy(final ParParty party) {
        List<ParComplementType> complementTypes = complementTypeRepository
                .findComplementTypesByPartyType(party.getPartyType());

        Map<Integer, ParComplementType> complementTypeMap = ElzaTools
                .createEntityMap(complementTypes, c -> c.getComplementTypeId());

        Map<String, Object> input = new HashMap<>();
        input.put("PARTY", party);
        input.put("COMPLEMENT_TYPE_MAP", complementTypeMap);

        ScriptSource source = new ResourceScriptSource(createRecordResource);
        return (RegRecord) groovyScriptEvaluator.evaluate(source, input);
    }


    @PostConstruct
    private void initScripts() {
        File dirRules = new File(groovyScriptDir);
        if (!dirRules.exists()) {
            dirRules.mkdir();
        }

        File createRecordFile = new File(FilenameUtils.concat(groovyScriptDir, CREATE_RECORD_FILE));

        try {
            if (!createRecordFile.exists() || createRecordFile.lastModified() < createRecordDefaultResource
                    .lastModified()) {
                Files.copy(createRecordDefaultResource.getInputStream(), createRecordFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);

                File copiedFile = new File(createRecordFile.getAbsolutePath());
                copiedFile.setLastModified(createRecordDefaultResource.lastModified());


                logger.info("Vytvoření souboru " + createRecordFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se vytvořit soubor " + createRecordFile.getAbsolutePath());
        }

        createRecordResource = new PathResource(createRecordFile.toPath());
    }
}
