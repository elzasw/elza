package cz.tacr.elza.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.interpi.service.InterpiFactory;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
import cz.tacr.elza.service.party.ApConvResult;
import cz.tacr.elza.ws.types.v1.Did;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * Servisní třída pro vykonávání groovy scriptů.
 */
@Service
public class GroovyScriptService {

    private static final Logger LOG = LoggerFactory.getLogger(GroovyScriptService.class);

    // groovy script pro vytvoření rejstříkového hesla
    private final GroovyScriptFile createRecordScriptFile;

    // groovy script pro vytvoření DID
    private final GroovyScriptFile createDidScriptFile;

    // groovy script pro import z Interpi
    private final GroovyScriptFile interpiScriptFile;

    @Autowired
    public GroovyScriptService(ResourcePathResolver resourcePathResolver,
                               @Value("classpath:/script/groovy/createRecord.groovy") Resource createRecordScriptSource,
                               @Value("classpath:/script/groovy/createDid.groovy") Resource createDidScriptSource,
                               @Value("classpath:/script/groovy/interpiImport.groovy") Resource interpiFile) {
        try {
            Path groovyDir = resourcePathResolver.getGroovyDir(); // TODO: Move initialization to startup service
            Files.createDirectories(groovyDir);

            this.createRecordScriptFile = GroovyScriptFile.create(createRecordScriptSource, groovyDir);
            this.createDidScriptFile = GroovyScriptFile.create(createDidScriptSource, groovyDir);
            this.interpiScriptFile = GroovyScriptFile.create(interpiFile, groovyDir);
        } catch (Throwable t) {
            throw new SystemException("Failed to initialize groovy scripts", t);
        }
    }

    /**
     * Zavolá script pro vytvoření rejstříkového hesla z osoby.
     *
     * @param party osoba
     * @param complementTypes available complement types for party type
     * @return vytvořené rejstříkové heslo s nastavenými variantními hesly
     */
    public ApConvResult convertPartyToAp(final ParParty party, 
                                         final Collection<ParComplementType> complementTypes) {
        Map<Integer, ParComplementType> complementTypeMap = ElzaTools.createEntityMap(complementTypes,
                ParComplementType::getComplementTypeId);

        Map<String, Object> input = new HashMap<>();
        input.put("PARTY", party);
        input.put("COMPLEMENT_TYPE_MAP", complementTypeMap);

        return (ApConvResult) createRecordScriptFile.evaluate(input);
    }

    /**
     * Zavolá script pro vytvoření did z node.
     *
     * @param arrNode zdrojové node
     * @return vytvořená instance node
     */
    public Did createDid(final ArrNode arrNode) {
        Map<String, Object> input = new HashMap<>();
        input.put("NODE", arrNode);

        return (Did) createDidScriptFile.evaluate(input);
    }

    public List<ExternalRecordVO> convertListToExternalRecordVO(final List<EntitaTyp> searchResults,
                                                                final boolean generateVariantNames,
                                                                final InterpiFactory interpiFactory) {

        List<ExternalRecordVO> resultList = new ArrayList<>(searchResults.size());
        for (EntitaTyp et : searchResults) {
            ExternalRecordVO result = convertToExternalRecordVO(et, generateVariantNames, interpiFactory);
            resultList.add(result);
        }
        return resultList;
    }

    public ExternalRecordVO convertToExternalRecordVO(final EntitaTyp entity,
                                                      final boolean generateVariantNames,
                                                      final InterpiFactory interpiFactory) {
        Map<String, Object> input = new HashMap<>();
        input.put("ENTITY", entity);
        input.put("FACTORY", interpiFactory);
        input.put("GENERATE_VARIANT_NAMES", generateVariantNames);

        return (ExternalRecordVO) interpiScriptFile.evaluate(input);
    }

    public static class GroovyScriptFile {

        private final File scriptFile;

        private Class<?> scriptClass;

        private long lastModified = -1;

        public GroovyScriptFile(File scriptFile) {
            this.scriptFile = Validate.notNull(scriptFile);
        }

        public Object evaluate(Map<String, Object> variables) {
            return evaluate(new Binding(variables));
        }

        public Object evaluate(Binding variables) {
            Script script = null;
            try {
                script = createScript(variables);
            } catch (Throwable t) {
                throw new SystemException("Failed to create groovy script, source:" + scriptFile, t);
            }
            Object result = script.run();
            return result;
        }

        private synchronized Script createScript(Binding variables) throws IOException {
            long fileLastModified = scriptFile.lastModified();
            if (lastModified < fileLastModified) {
                scriptClass = parseClass(scriptFile);
                lastModified = fileLastModified;
                LOG.info("Groovy script recompiled, source:" + scriptFile);
            }
            return InvokerHelper.createScript(scriptClass, variables);
        }

        private static Class<?> parseClass(File scriptFile) throws IOException {
            GroovyShell shell = new GroovyShell();
            GroovyClassLoader loader = shell.getClassLoader();
            GroovyCodeSource source = new GroovyCodeSource(scriptFile);
            return loader.parseClass(source, false);
        }

        public static GroovyScriptFile create(Resource resource, Path destDir) throws IOException {
            String fileName = resource.getFilename();
            Validate.notBlank(fileName);

            File scriptFile = destDir.resolve(fileName).toFile();

            long resourceLM = resource.lastModified();
            if (!scriptFile.exists() || resourceLM > scriptFile.lastModified()) {
                FileUtils.copyInputStreamToFile(resource.getInputStream(), scriptFile);
                scriptFile.setLastModified(resourceLM);
            }
            return new GroovyScriptFile(scriptFile);
        }
    }
}
