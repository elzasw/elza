package cz.tacr.elza.service;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.ws.types.v1.Did;
import liquibase.util.file.FilenameUtils;
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

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
     * groovy script pro vytvoření rejstříkového hesla.
     */
    @Value("classpath:/script/groovy/createRecord.groovy")
    private Resource createRecordDefaultResource; // výchozí
    private Resource createRecordResource; // načtený

    private static final String CREATE_RECORD_FILE = "createRecord.groovy";

    /**
     * Groovy script pro vytvoření DID
     */
    @Value("classpath:/script/groovy/createDid.groovy")
    private Resource createDidDefaultResource; // výchozí
    private Resource createDidResource; // načtený
    private static final String CREATE_DID_FILE = "createDid.groovy";

    /**
     * Adresář pro groovy scripty
     */
    @Value("${elza.groovy.groovyDir}")
    private String groovyScriptDir;

    @Autowired
    private ScriptEvaluator groovyScriptEvaluator;

    @Autowired
    private ComplementTypeRepository complementTypeRepository;

    @PostConstruct
    private void initScripts() {
        File dirRules = new File(groovyScriptDir);
        if (!dirRules.exists()) {
            dirRules.mkdir();
        }

        createRecordResource = getResourceForScriptFile(CREATE_RECORD_FILE, createRecordDefaultResource);
        createDidResource = getResourceForScriptFile(CREATE_DID_FILE, createDidDefaultResource);
    }

    private Resource getResourceForScriptFile(final String createRecordFileName, final Resource defaultResource) {
        File createRecordFile = new File(FilenameUtils.concat(groovyScriptDir, createRecordFileName));
        createScriptFile(createRecordFile, defaultResource);
        return new PathResource(createRecordFile.toPath());
    }

    private void createScriptFile(File createRecordFile, final Resource resource) {
        try {
            if (!createRecordFile.exists() || createRecordFile.lastModified() < resource.lastModified()) {
                Files.copy(resource.getInputStream(), createRecordFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                File copiedFile = new File(createRecordFile.getAbsolutePath());
                copiedFile.setLastModified(resource.lastModified());

                logger.info("Vytvoření souboru " + createRecordFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se vytvořit soubor " + createRecordFile.getAbsolutePath());
        }
    }

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


    /**
     * Zavolá script pro vytvoření did z node.
     *
     * @param arrNode zdrojové node
     * @return vytvořená instance node
     */
    public Did createDid(ArrNode arrNode) {

        Map<String, Object> input = new HashMap<>();
        input.put("NODE", arrNode);

        ScriptSource source = new ResourceScriptSource(createDidResource);
        return (Did) groovyScriptEvaluator.evaluate(source, input);
    }
}
