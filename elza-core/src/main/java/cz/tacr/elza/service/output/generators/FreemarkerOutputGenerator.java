package cz.tacr.elza.service.output.generators;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.tree.FundTreeProvider;
import cz.tacr.elza.exception.ProcessException;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.cache.NodeCacheService;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreemarkerOutputGenerator extends TemplateOutputGenerator {

    private static final String TEMPLATE_FILE_NAME = "index.ftl";

    FreemarkerOutputGenerator(StaticDataService staticDataService,
                              FundTreeProvider fundTreeProvider,
                              NodeCacheService nodeCacheService,
                              EntityManager em,
                              DmsService dmsService,
                              String rulesDirectory) {
        super(staticDataService, fundTreeProvider, nodeCacheService, em, dmsService, rulesDirectory);
    }

    @Override
    protected void generate(Path templateDir, OutputStream os) throws IOException {
        Template template = getTemplate(templateDir);

        // prepare data model
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("output", outputModel);

        // generate output
        OutputStreamWriter writter = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        try {
            template.process(dataModel, writter);
        } catch (TemplateException e) {
            throw new ProcessException(params.getDefinitionId(), "Failed to generate output from Freemarker template", e);
        }
    }

    private Template getTemplate(Path templateDir) throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);

        FileTemplateLoader loader = new FileTemplateLoader(templateDir.toFile());
        cfg.setTemplateLoader(loader);

        Path templateFile = templateDir.resolve(TEMPLATE_FILE_NAME);
        return cfg.getTemplate(templateFile.getFileName().toString());
    }
}
