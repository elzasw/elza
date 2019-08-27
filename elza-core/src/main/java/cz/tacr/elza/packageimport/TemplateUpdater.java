package cz.tacr.elza.packageimport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutput.OutputState;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.RulTemplate.Engine;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.TemplateXml;
import cz.tacr.elza.packageimport.xml.Templates;
import cz.tacr.elza.packageimport.xml.common.OtherCode;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.repository.TemplateRepository;

public class TemplateUpdater {

    /**
     * templaty outputů
     */
    public static final String TEMPLATE_XML = "rul_template.xml";

    private List<RulOutputType> outputTypes;

    /**
     * Lookup helper
     */
    private Map<String, RulOutputType> outputTypesMap = new HashMap<>();

    private final TemplateRepository templateRepository;
    private final OutputRepository outputRepository;
    private final OutputResultRepository outputResultRepository;

    /**
     * List of final templates
     */
    List<RulTemplate> templates = new ArrayList<>();

    private RulPackage rulPackage;

    private RuleUpdateContext ruc;

    /**
     * Map of other codes
     */
    private final Map<RulTemplate, RulTemplate> otherTemplMapping = new HashMap<>();

    public TemplateUpdater(final TemplateRepository templateRepository,
            final OutputRepository outputRepository,
            final OutputResultRepository outputResultRepository,
            final List<RulOutputType> rulOutputTypesNew) {
        this.outputTypes = rulOutputTypesNew;
        this.templateRepository = templateRepository;
        this.outputRepository = outputRepository;
        this.outputResultRepository = outputResultRepository;

        // initialize lookup
        for (RulOutputType outputType : outputTypes) {
            RulOutputType oldItem = outputTypesMap.put(outputType.getCode(), outputType);
            if (oldItem != null) {
                throw new SystemException("Multiple templates with same code exists", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("code", outputType.getCode());
            }
        }
    }

    public void run(RuleUpdateContext ruc) {
        this.ruc = ruc;
        PackageContext pkgCtx = ruc.getPackageUpdateContext();
        this.rulPackage = ruc.getRulPackage();

        List<RulTemplate> oldDBTemplates = templateRepository.findByRulPackage(ruc.getRulPackage());
        Map<String, RulTemplate> oldDBTemplatesMap = new HashMap<>();
        oldDBTemplates.forEach(t -> {
            RulTemplate oldItem = oldDBTemplatesMap.put(t.getCode(), t);
            if (oldItem != null) {
                throw new SystemException("Multiple templates with same code exists", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("code", oldItem.getCode());
            }
        });

        Templates templatesXml = pkgCtx.convertXmlStreamToObject(Templates.class,
                                                                 ruc.getKeyDirPath() + TEMPLATE_XML);
        if (templatesXml != null && !CollectionUtils.isEmpty(templatesXml.getTemplates())) {
            for (TemplateXml srcXmlTempl : templatesXml.getTemplates()) {
                processTempl(srcXmlTempl, oldDBTemplatesMap);
            }
        }

        templateRepository.save(this.templates);

        // Run mapping
        for (Entry<RulTemplate, RulTemplate> itemMapping : otherTemplMapping.entrySet()) {
            outputResultRepository.updateTemplateByTemplate(itemMapping.getKey(), itemMapping.getValue());
            outputRepository.updateTemplateByTemplate(itemMapping.getKey(), itemMapping.getValue());
        }

        List<RulTemplate> rulTemplateToDelete = new ArrayList<>(oldDBTemplates);
        rulTemplateToDelete.removeAll(this.templates);
        if (!rulTemplateToDelete.isEmpty()) {
            // Check if there exists non deleted templates
            List<ArrOutput> byTemplate = outputRepository
                    .findNonDeletedByTemplatesAndStates(rulTemplateToDelete, Arrays
                            .asList(OutputState.OPEN, OutputState.GENERATING, OutputState.COMPUTING));
            if (!byTemplate.isEmpty()) {
                StringBuilder sb = new StringBuilder()
                        .append("Existuje výstup(y), který nebyl vygenerován či smazán a je navázán na šablonu, která je v novém balíčku smazána.");
                byTemplate.forEach((o) -> {
                    ArrFund fund = o.getFund();
                    sb.append("\noutputId: ").append(o.getOutputId())
                            .append(", outputName: ").append(o.getName())
                            .append(", fundId: ").append(fund.getFundId())
                            .append(", fundName: ").append(fund.getName()).toString();

                });
                throw new IllegalStateException(sb.toString());
            }
            templateRepository.updateDeleted(rulTemplateToDelete, true);
        }

        try {
            importTemplatesFiles(ruc.getTemplatesDir(), this.templates);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    private void processTempl(TemplateXml srcXmlTempl, Map<String, RulTemplate> oldDBTemplatesMap) {
        // find if exists
        RulTemplate templ = oldDBTemplatesMap.get(srcXmlTempl.getCode());

        if (templ == null) {
            templ = new RulTemplate();
        }

        // store mapping
        if (srcXmlTempl.getOtherCodes() != null && srcXmlTempl.getOtherCodes().getOtherCodes()!=null) {
            for (OtherCode otherCode : srcXmlTempl.getOtherCodes().getOtherCodes()) {
                // check if code exists
                RulTemplate otherTempl = oldDBTemplatesMap.get(otherCode.getCode());
                if (otherTempl != null) {
                    otherTemplMapping.put(otherTempl, templ);
                }
            }
        }

        convertRulTemplate(srcXmlTempl, templ);

        this.templates.add(templ);
    }

    /**
     * Převod VO na DAO Templaty outputů
     *
     * @param rulPackage
     *            balíček
     * @param template
     *            VO template
     * @param rulTemplate
     *            DAO template
     * @param rulOutputTypes
     *            seznam typů outputů
     */
    private void convertRulTemplate(final TemplateXml template,
                                    final RulTemplate rulTemplate) {
        rulTemplate.setName(template.getName());
        rulTemplate.setCode(template.getCode());
        rulTemplate.setEngine(Engine.valueOf(template.getEngine()));
        rulTemplate.setPackage(rulPackage);
        rulTemplate.setDirectory(template.getDirectory());
        rulTemplate.setMimeType(template.getMimeType());
        rulTemplate.setExtension(template.getExtension());
        rulTemplate.setDeleted(false);

        // Find output type
        List<RulOutputType> findItems = outputTypes.stream()
                .filter((r) -> r.getCode().equals(template.getOutputType()))
                .collect(Collectors.toList());

        RulOutputType item;

        if (findItems.size() > 0) {
            item = findItems.get(0);
        } else {
            throw new IllegalStateException("Kód " + template.getOutputType() + " neexistuje v RulOutputType");
        }

        rulTemplate.setOutputType(item);
    }

    private void importTemplatesFiles(
                                      final File dirTemplates,
                                      final List<RulTemplate> rulTemplateActual)
            throws IOException {
        String rulsetCode = ruc.getRulSetCode();
        for (RulTemplate template : rulTemplateActual) {
            final String templateDir = PackageService.ZIP_DIR_RULE_SET + "/" + rulsetCode + "/"
                    + PackageService.ZIP_DIR_TEMPLATES + "/"
                    + template.getDirectory();
            final String templateZipKeyDir = templateDir + "/";
            List<String> templateFileKeys = ruc.getPackageUpdateContext().getByteStreamKeys()
                    .stream()
                    .filter(key -> key.startsWith(templateZipKeyDir) && !key.equals(templateZipKeyDir))
                    .map(key -> key.replace(templateZipKeyDir, ""))
                    .collect(Collectors.toList());
            String path = dirTemplates + File.separator + template.getDirectory();
            File dirFile = new File(path);
            if (!dirFile.exists() && !dirFile.mkdirs()) {
                throw new IOException("Nepodařilo se vytvořit složku: " + path);
            }
            for (String file : templateFileKeys) {
                ruc.getPackageUpdateContext().saveFile(dirFile, templateDir, file);
            }
        }
    }
}
