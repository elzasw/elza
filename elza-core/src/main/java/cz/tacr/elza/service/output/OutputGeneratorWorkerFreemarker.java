package cz.tacr.elza.service.output;

import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.service.DmsService;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.input.ReaderInputStream;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Zajišťuje generování výstupu a jeho uložení do dms na základě vstupní definice - část generování specifická pro freemarker.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 24.6.16
 */

@Component
@Scope("prototype")
class OutputGeneratorWorkerFreemarker extends OutputGeneratorWorkerAbstract {

    private static final String FREEMARKER_TEMPLATE_SUFFIX = ".ftl";
    private static final String FREEMARKER_MAIN_TEMPLATE = MAIN_TEMPLATE_BASE_NAME + FREEMARKER_TEMPLATE_SUFFIX;
    private static final String OUTFILE_SUFFIX_CVS = ".cvs";

    @Override
    protected String getOutfileSuffix() {
        return OUTFILE_SUFFIX_CVS;
    }

    @Override
    protected String getMimeType() {
        return DmsService.MIME_TYPE_TEXT_CVS;
    }

    @Override
    protected InputStream getContent(ArrOutputDefinition arrOutputDefinition, RulTemplate rulTemplate, Output output) {
        try {
            // dohledání šablony
            final String rulTemplateDirectory = rulTemplate.getDirectory();
            final File templateDir = Paths.get(templatesDir, rulTemplateDirectory).toFile();
            Assert.isTrue(templateDir.exists() && templateDir.isDirectory(), "Nepodařilo se najít adresář s definicí šablony: " + templateDir.getAbsolutePath());

            final File mainFreemarkerTemplate = Paths.get(templateDir.getAbsolutePath(), FREEMARKER_MAIN_TEMPLATE).toFile();
            Assert.isTrue(mainFreemarkerTemplate.exists(), "Nepodařilo se najít definici hlavní šablony.");

            Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
            // Export do výstupu - pouze příprava procesu pro renderování - reálně proběhne až při čtení z in v dms
            PipedReader in = new PipedReader();
            PipedWriter out = new PipedWriter(in);

            // inicializace
            FileTemplateLoader templateLoader = new FileTemplateLoader(mainFreemarkerTemplate.getParentFile());
            cfg.setTemplateLoader(templateLoader);
            Template template = cfg.getTemplate(mainFreemarkerTemplate.getName());

            // příparava dat
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("output", output);

            new Thread(() -> {
                try {
                    template.process(parameters, out);
                    out.close();
                } catch (TemplateException | IOException e) {
                    throw new IllegalStateException("Nepodařilo se vyrenderovat výstup ze šablony " + mainFreemarkerTemplate.getAbsolutePath() + ".", e);
                }
            }).start();

            return new ReaderInputStream(in, Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se uložit výstup.", e);
        }
    }

}
