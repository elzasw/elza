package cz.tacr.elza.service.output.generator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import cz.tacr.elza.repository.ApIndexRepository;
import org.springframework.context.ApplicationContext;

import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.fund.FundTreeProvider;
import cz.tacr.elza.exception.ProcessException;
import cz.tacr.elza.print.OutputModel;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.output.OutputParams;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreemarkerOutputGenerator extends DmsOutputGenerator {

    private static final String TEMPLATE_FILE_NAME = "index.ftl";

    private final OutputModel outputModel;

    FreemarkerOutputGenerator(ApplicationContext applicationContext,
                              StaticDataService staticDataService,
                              ElzaLocale elzaLocale,
                              FundTreeProvider fundTreeProvider,
                              NodeCacheService nodeCacheService,
                              InstitutionRepository institutionRepository,
                              ApStateRepository apStateRepository,
                              ApBindingRepository bindingRepository,
                              ApPartRepository partRepository,
                              ApItemRepository itemRepository,
                              ApBindingStateRepository bindingStateRepository,
                              ApIndexRepository indexRepository,
                              EntityManager em,
                              DmsService dmsService,
                              DaoLinkRepository daoLinkRepository) {
        super(em, dmsService);

        StructuredObjectRepository structObjRepos = applicationContext.getBean(StructuredObjectRepository.class);
        StructuredItemRepository structItemRepos = applicationContext.getBean(StructuredItemRepository.class);

        outputModel = new OutputModel(staticDataService, elzaLocale,
                fundTreeProvider, nodeCacheService, institutionRepository, apStateRepository,
                bindingRepository, null, structObjRepos, structItemRepos, partRepository, itemRepository,
                bindingStateRepository, indexRepository,
                daoLinkRepository);
    }

    @Override
    public void init(OutputParams params) {
        super.init(params);
        outputModel.init(params);
    }

    @Override
    protected void generate(OutputStream os) throws IOException {
        Template template = loadTemplate();

        // prepare data model
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("output", outputModel);

        // generate output
        OutputStreamWriter writter = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        try {
            template.process(dataModel, writter);
        } catch (TemplateException e) {
            throw new ProcessException(params.getOutputId(), "Failed to generate output from Freemarker template", e);
        }
    }

    private Template loadTemplate() throws IOException {
        Path templateDir = params.getTemplateDir();

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setAPIBuiltinEnabled(true);

        FileTemplateLoader loader = new FileTemplateLoader(templateDir.toFile());
        cfg.setTemplateLoader(loader);

        Path templateFile = templateDir.resolve(TEMPLATE_FILE_NAME);
        return cfg.getTemplate(templateFile.getFileName().toString());
    }

    @Override
    public void close() throws IOException {
    }
}
