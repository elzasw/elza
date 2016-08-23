package cz.tacr.elza.service.output;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.input.ReaderInputStream;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.print.Output;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

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

    @Override
    protected InputStream getContent(final ArrOutputDefinition arrOutputDefinition, final RulTemplate rulTemplate, final Output output) {
        try {
            // dohledání šablony
            final File mainFreemarkerTemplate = getTemplate(rulTemplate, FREEMARKER_MAIN_TEMPLATE);

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
