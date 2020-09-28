package cz.tacr.elza.service;

import com.google.common.eventbus.Subscribe;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.groovy.GroovyPart;
import cz.tacr.elza.groovy.GroovyResult;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.ws.types.v1.Did;
import groovy.lang.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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

    private final NodeCacheService nodeCacheService;

    private static final String PART = "PART";
    private static final String EXT_SYSTEM_TYPE = "EXT_SYSTEM_TYPE";
    private static final String ITEM_TYPE_CODE = "ITEM_TYPE_CODE";
    private static final String ITEM_SPEC_CODE = "ITEM_SPEC_CODE";

    private Map<File, GroovyScriptFile> groovyScriptMap = new HashMap<>();

    private final Object lock = new Object();

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.GROOVY)) {
            groovyScriptMap = new HashMap<>();
        }
    }

    @Autowired
    public GroovyScriptService(ResourcePathResolver resourcePathResolver,
                               NodeCacheService nodeCacheService,
                               @Value("classpath:/script/groovy/createRecord.groovy") Resource createRecordScriptSource,
                               @Value("classpath:/script/groovy/createDid.groovy") Resource createDidScriptSource) {
        this.nodeCacheService = nodeCacheService;
        try {
            Path groovyDir = resourcePathResolver.getGroovyDir(); // TODO: Move initialization to startup service
            Files.createDirectories(groovyDir);

            this.createRecordScriptFile = GroovyScriptFile.create(createRecordScriptSource, groovyDir);
            this.createDidScriptFile = GroovyScriptFile.create(createDidScriptSource, groovyDir);
        } catch (Throwable t) {
            throw new SystemException("Failed to initialize groovy scripts", t);
        }
    }

    /**
     * Zavolá script pro vytvoření did z node.
     *
     * @param arrNode zdrojové node
     * @return vytvořená instance node
     */
    public Did createDid(final ArrNode arrNode) {
        Map<String, Object> input = new HashMap<>();
        RestoredNode cachedNode = nodeCacheService.getNode(arrNode.getNodeId());
        input.put("CACHED_NODE", cachedNode);
        input.put("NODE", arrNode);

        return (Did) createDidScriptFile.evaluate(input);
    }

    public List<Did> createDids(final Collection<ArrNode> arrNodes) {
        Map<String, Object> input = new HashMap<>();

        Set<Integer> nodeIds = arrNodes.stream().map(ArrNode::getNodeId).collect(Collectors.toSet());
        Map<Integer, RestoredNode> nodes = nodeCacheService.getNodes(nodeIds);
        List<Did> result = new ArrayList<>();
        for (RestoredNode value : nodes.values()) {
            input.put("CACHED_NODE", value);
            input.put("NODE", value.getNode());
            result.add((Did) createDidScriptFile.evaluate(input));
        }
        return result;
    }

    public GroovyResult process(GroovyPart part, String groovyFilePath) {
        GroovyScriptFile groovyScriptFile = getGroovyScriptFile(groovyFilePath);

        Map<String, Object> input = new HashMap<>();
        input.put(PART, part);

        return (GroovyResult) groovyScriptFile.evaluate(input);
    }

    public String findItemTypeCode(String extSystemType, String itemTypeCode, String groovyFilePath) {
        GroovyScriptFile groovyScriptFile = getGroovyScriptFile(groovyFilePath);

        Map<String, Object> input = new HashMap<>();
        input.put(EXT_SYSTEM_TYPE, extSystemType);
        input.put(ITEM_TYPE_CODE, itemTypeCode);

        return (String) groovyScriptFile.evaluate(input);
    }

    public String findItemSpecCode(String extSystemType, String itemSpecCode, String groovyFilePath) {
        GroovyScriptFile groovyScriptFile = getGroovyScriptFile(groovyFilePath);

        Map<String, Object> input = new HashMap<>();
        input.put(EXT_SYSTEM_TYPE, extSystemType);
        input.put(ITEM_SPEC_CODE, itemSpecCode);

        return (String) groovyScriptFile.evaluate(input);
    }

    private GroovyScriptFile getGroovyScriptFile(String groovyFilePath) {
        GroovyScriptFile groovyScriptFile;
        File groovyFile = new File(groovyFilePath);
        try {
            synchronized (lock) {
                groovyScriptFile = groovyScriptMap.get(groovyFile);
                if (groovyScriptFile == null) {
                    groovyScriptFile = new GroovyScriptFile(groovyFile);
                    groovyScriptMap.put(groovyFile, groovyScriptFile);
                }
            }
        } catch (Throwable t) {
            throw new SystemException("Failed to initialize groovy scripts", t);
        }
        return groovyScriptFile;
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
