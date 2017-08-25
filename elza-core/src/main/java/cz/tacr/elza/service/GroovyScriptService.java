package cz.tacr.elza.service;

import java.io.File;
import java.io.IOException;
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
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.interpi.service.InterpiFactory;
import cz.tacr.elza.interpi.service.vo.ExternalRecordVO;
import cz.tacr.elza.interpi.ws.wo.EntitaTyp;
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

    // groovy script pro interpi detail v listu
    private final GroovyScriptFile interpiRecordListScriptFile;

    // groovy script pro interpi detail v mapování
    private final GroovyScriptFile interpiRecordDetailScriptFile;

    @Autowired
    public GroovyScriptService(@Value("${elza.groovy.groovyDir}") String groovyScriptDirPath,
                               @Value("classpath:/script/groovy/createRecord.groovy") Resource createRecordScriptSource,
                               @Value("classpath:/script/groovy/createDid.groovy") Resource createDidScriptSource,
                               @Value("classpath:/script/groovy/interpiRecord.groovy") Resource interpiRecordListScriptSource,
                               @Value("classpath:/script/groovy/interpiRecordDetail.groovy") Resource interpiRecordDetailScriptSource) {
        try {
            File workDir = createGroovyScriptDir(groovyScriptDirPath);
            this.createRecordScriptFile = GroovyScriptFile.create(createRecordScriptSource, workDir);
            this.createDidScriptFile = GroovyScriptFile.create(createDidScriptSource, workDir);
            this.interpiRecordListScriptFile = GroovyScriptFile.create(interpiRecordListScriptSource, workDir);
            this.interpiRecordDetailScriptFile = GroovyScriptFile.create(interpiRecordDetailScriptSource, workDir);
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
    public RegRecord getRecordFromGroovy(final ParParty party, final List<ParComplementType> complementTypes) {
        Map<Integer, ParComplementType> complementTypeMap = ElzaTools.createEntityMap(complementTypes,
                ParComplementType::getComplementTypeId);

        Map<String, Object> input = new HashMap<>();
        input.put("PARTY", party);
        input.put("COMPLEMENT_TYPE_MAP", complementTypeMap);

        return (RegRecord) createRecordScriptFile.evaluate(input);
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

    @SuppressWarnings("unchecked")
    public List<ExternalRecordVO> convertListToExternalRecordVO(final List<EntitaTyp> searchResults,
                                                                final boolean generateVariantNames,
                                                                final InterpiFactory interpiFactory) {
        Map<String, Object> input = new HashMap<>();
        input.put("ENTITIES", searchResults);
        input.put("FACTORY", interpiFactory);
        input.put("GENERATE_VARIANT_NAMES", generateVariantNames);

        return (List<ExternalRecordVO>) interpiRecordListScriptFile.evaluate(input);
    }

    public ExternalRecordVO convertToExternalRecordVO(final EntitaTyp entity,
                                                      final boolean generateVariantNames,
                                                      final InterpiFactory interpiFactory) {
        Map<String, Object> input = new HashMap<>();
        input.put("ENTITY", entity);
        input.put("FACTORY", interpiFactory);
        input.put("GENERATE_VARIANT_NAMES", generateVariantNames);

        return (ExternalRecordVO) interpiRecordDetailScriptFile.evaluate(input);
    }

    private static File createGroovyScriptDir(String groovyScriptDirPath) throws IOException {
        File workDir = new File(groovyScriptDirPath);
        workDir.mkdirs();
        return workDir;
    }

    public static class GroovyScriptFile {

        private final File scriptFile;

        private Class<?> scriptClass;

        private long lastModified = -1;

        private GroovyScriptFile(File scriptFile) {
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
                scriptClass = compileClass(scriptFile);
                lastModified = fileLastModified;
                LOG.info("Groovy script recompiled, source:" + scriptFile);
            }
            return InvokerHelper.createScript(scriptClass, variables);
        }

        private static Class<?> compileClass(File scriptFile) throws IOException {
            GroovyShell shell = new GroovyShell();
            GroovyClassLoader loader = shell.getClassLoader();
            GroovyCodeSource source = new GroovyCodeSource(scriptFile);
            return loader.parseClass(source, false);
        }

        private static GroovyScriptFile create(Resource resource, File workDir) throws IOException {
            String fileName = resource.getFilename();
            Assert.hasLength(fileName, "Resource does not have a filename");
            File scriptFile = new File(workDir, fileName);

            long resourceLM = resource.lastModified();
            if (!scriptFile.exists() || resourceLM > scriptFile.lastModified()) {
                FileUtils.copyInputStreamToFile(resource.getInputStream(), scriptFile);
                scriptFile.setLastModified(resourceLM);
            }
            return new GroovyScriptFile(scriptFile);
        }
    }
}
