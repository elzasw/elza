package cz.tacr.elza.service.output.generators;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.tree.FundTreeProvider;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.print.OutputModel;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.output.OutputParams;

public abstract class TemplateOutputGenerator extends DmsOutputGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TemplateOutputGenerator.class);

    private static final String PACKAGE_TEMPLATES_DIR = "templates";

    private final StaticDataService staticDataService;

    private final String rulesDirectory;

    protected final OutputModel outputModel;

    protected TemplateOutputGenerator(StaticDataService staticDataService,
                                      FundTreeProvider fundTreeProvider,
                                      NodeCacheService nodeCacheService,
                                      EntityManager em,
                                      DmsService dmsService,
                                      String rulesDirectory) {
        super(em, dmsService);
        this.staticDataService = staticDataService;
        this.rulesDirectory = rulesDirectory;
        this.outputModel = new OutputModel(staticDataService, fundTreeProvider, nodeCacheService);
    }

    @Override
    public void init(OutputParams params) {
        logger.info("Output model initialization started, outputDefinitionId:{}", params.getDefinitionId());
        outputModel.init(params);
        logger.info("Output model initialization ended, outputDefinitionId:{}", params.getDefinitionId());
    }

    @Override
    protected final void generate(OutputStream os) throws IOException {
        Validate.isTrue(outputModel.isInitialized());

        Path templateDir = getTemplateDirectory(params.getTemplate());
        generate(templateDir, os);
    }

    protected abstract void generate(Path templateDir, OutputStream os) throws IOException;

    private Path getTemplateDirectory(RulTemplate template) {
        RulPackage rulPackage = staticDataService.getData().getPackageById(template.getPackageId());

        String packageDir = rulPackage.getCode();
        String templateSubDir = template.getDirectory();

        Path dirPath = Paths.get(rulesDirectory, packageDir, PACKAGE_TEMPLATES_DIR, templateSubDir);
        if (!Files.isDirectory(dirPath)) {
            throw new SystemException("Template directory not found, path:" + dirPath);
        }
        return dirPath.toAbsolutePath();
    }
}
