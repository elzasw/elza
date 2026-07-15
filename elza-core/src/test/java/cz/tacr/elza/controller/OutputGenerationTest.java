package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrOutputVO;
import cz.tacr.elza.controller.vo.RulTemplateVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.service.output.OutputRequestStatus;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.NodeBase;
import cz.tacr.elza.test.controller.vo.OutputDef;
import cz.tacr.elza.test.controller.vo.OutputNameParam;
import cz.tacr.elza.test.controller.vo.OutputState;
import cz.tacr.elza.test.controller.vo.OutputType;
import io.restassured.response.Response;

/**
 * Test generování výstupu a ověření, že soubor byl vytvořen.
 */
public class OutputGenerationTest extends AbstractControllerTest {

    private static final String GENERATE_OUTPUT = ARRANGEMENT_CONTROLLER_URL + "/output/generate/{outputId}";
    private static final String OUTPUT_RESULTS_DOWNLOAD = "/api/outputResults/{outputId}";

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Vyčištění DMS adresáře - deleteTables() nemaže fyzické soubory
        cleanDmsDirectory();
    }

    /**
     * Smazání obsahu DMS adresáře pro zajištění opakovatelnosti testu.
     */
    private void cleanDmsDirectory() throws IOException {
        Path dmsDir = resourcePathResolver.getDmsDir();
        if (Files.exists(dmsDir)) {
            Files.walkFileTree(dmsDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    @Test
    public void generateOutputTest() {
        // Vytvoření fondu
        Fund fund = createFund("TestFund", null);
        assertNotNull(fund);
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        // Získání typu výstupu SRD_INVENTORY
        List<OutputType> outputTypes = outputApi.outputGetOutputTypes(fundVersion.getId());
        OutputType inventoryType = outputTypes.stream()
                .filter(t -> "SRD_INVENTORY".equals(t.getCode()))
                .findFirst()
                .orElse(null);
        assertNotNull(inventoryType, "Typ výstupu SRD_INVENTORY nebyl nalezen");

        // Získání šablony SRD_INVENTORY_FREEMARKER (CSV)
        List<RulTemplateVO> templates = getTemplates();
        RulTemplateVO freemarkerTemplate = templates.stream()
                .filter(t -> "SRD_INVENTORY_FREEMARKER".equals(t.getCode()))
                .findFirst()
                .orElse(null);
        assertNotNull(freemarkerTemplate, "Šablona SRD_INVENTORY_FREEMARKER nebyla nalezena");

        // Vytvoření výstupu s šablonou
        OutputNameParam param = new OutputNameParam();
        param.setName("Test Output");
        param.setInternalCode("TEST_OUT");
        param.setOutputTypeId(outputTypes.iterator().next().getId());
        param.setTemplateId(freemarkerTemplate.getId());
        OutputDef output = outputApi.outputCreateNamedOutput(fundVersion.getId(), param);
        assertNotNull(output);
        assertEquals(OutputState.OPEN, output.getState());

        // Přidání kořenového uzlu
        ArrangementController.FaTreeParam treeParam = new ArrangementController.FaTreeParam();
        treeParam.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(treeParam);
        List<NodeBase> nodes = convertTreeNodes(treeData.getNodes());
        List<Integer> nodeIds = nodes.stream().map(NodeBase::getId).collect(Collectors.toList());
        assertTrue(!nodeIds.isEmpty(), "Fond musí mít alespoň jeden uzel");

        outputApi.outputAddNodesNamedOutput(output.getId(), fundVersion.getId(), nodeIds);

        // Spuštění generování (forced=true pro přeskočení kontrol bulk action)
        Response generateResponse = get(
                spec -> spec.pathParam("outputId", output.getId()).queryParam("forced", "true"),
                GENERATE_OUTPUT);
        ArrangementController.GenerateOutputResult generateResult = generateResponse.getBody()
                .as(ArrangementController.GenerateOutputResult.class);
        assertEquals(OutputRequestStatus.OK, generateResult.getStatus(),
                "Generování výstupu nebylo úspěšně zahájeno");

        // Čekání na dokončení asynchronní generace
        helperTestService.waitForWorkers();

        // Ověření stavu výstupu
        OutputDef outputDetail = outputApi.outputGetOutput(fundVersion.getId(), output.getId());
        assertEquals(OutputState.FINISHED, outputDetail.getState(),
                "Výstup by měl být ve stavu FINISHED, error: " + outputDetail.getError());
        assertNotNull(outputDetail.getOutputResultIds(), "OutputResultIds nesmí být null");
        assertTrue(!outputDetail.getOutputResultIds().isEmpty(), "Musí existovat alespoň jeden výsledek generování");
        assertNotNull(outputDetail.getGeneratedDate(), "Datum generování musí být vyplněno");

        // Stažení vygenerovaného souboru
        Response downloadResponse = get(
                spec -> spec.pathParam("outputId", output.getId()),
                OUTPUT_RESULTS_DOWNLOAD);
        byte[] fileContent = downloadResponse.getBody().asByteArray();
        assertNotNull(fileContent, "Obsah souboru nesmí být null");
        assertTrue(fileContent.length > 0, "Vygenerovaný soubor nesmí být prázdný");
    }

    @Test
    public void generatePdfOutputTest() {
        // Vytvoření fondu
        Fund fund = createFund("TestFundPdf", null);
        assertNotNull(fund);
        ArrFundVersionVO fundVersion = getOpenVersion(fund);

        // Získání typu výstupu SRD_INVENTORY
        List<OutputType> outputTypes = outputApi.outputGetOutputTypes(fundVersion.getId());
        OutputType inventoryType = outputTypes.stream()
                .filter(t -> "SRD_INVENTORY".equals(t.getCode()))
                .findFirst()
                .orElse(null);
        assertNotNull(inventoryType, "Typ výstupu SRD_INVENTORY nebyl nalezen");

        // Získání šablony SRD_INVENTORY_JASPER (PDF)
        List<RulTemplateVO> templates = getTemplates();
        RulTemplateVO jasperTemplate = templates.stream()
                .filter(t -> "SRD_INVENTORY_JASPER".equals(t.getCode()))
                .findFirst()
                .orElse(null);
        assertNotNull(jasperTemplate, "Šablona SRD_INVENTORY_JASPER nebyla nalezena");

        // Vytvoření výstupu s Jasper šablonou
        OutputNameParam param = new OutputNameParam();
        param.setName("Test Output");
        param.setInternalCode("TEST_OUT");
        param.setOutputTypeId(outputTypes.iterator().next().getId());
        param.setTemplateId(jasperTemplate.getId());
        OutputDef output = outputApi.outputCreateNamedOutput(fundVersion.getId(), param);
        assertNotNull(output);
        assertEquals(OutputState.OPEN, output.getState());

        // Přidání kořenového uzlu
        ArrangementController.FaTreeParam treeParam = new ArrangementController.FaTreeParam();
        treeParam.setVersionId(fundVersion.getId());
        TreeData treeData = getFundTree(treeParam);
        List<NodeBase> nodes = convertTreeNodes(treeData.getNodes());
        List<Integer> nodeIds = nodes.stream().map(NodeBase::getId).collect(Collectors.toList());
        assertTrue(!nodeIds.isEmpty(), "Fond musí mít alespoň jeden uzel");

        outputApi.outputAddNodesNamedOutput(output.getId(), fundVersion.getId(), nodeIds);

        // Spuštění generování PDF (forced=true pro přeskočení kontrol bulk action)
        Response generateResponse = get(
                spec -> spec.pathParam("outputId", output.getId()).queryParam("forced", "true"),
                GENERATE_OUTPUT);
        ArrangementController.GenerateOutputResult generateResult = generateResponse.getBody()
                .as(ArrangementController.GenerateOutputResult.class);
        assertEquals(OutputRequestStatus.OK, generateResult.getStatus(),
                "Generování PDF výstupu nebylo úspěšně zahájeno");

        // Čekání na dokončení asynchronní generace
        helperTestService.waitForWorkers();

        // Ověření stavu výstupu
        OutputDef outputDetail = outputApi.outputGetOutput(fundVersion.getId(), output.getId());
        assertEquals(OutputState.FINISHED, outputDetail.getState(),
                "Výstup by měl být ve stavu FINISHED, error: " + outputDetail.getError());
        assertNotNull(outputDetail.getOutputResultIds(), "OutputResultIds nesmí být null");
        assertTrue(!outputDetail.getOutputResultIds().isEmpty(), "Musí existovat alespoň jeden výsledek generování");
        assertNotNull(outputDetail.getGeneratedDate(), "Datum generování musí být vyplněno");

        // Stažení vygenerovaného PDF souboru
        Response downloadResponse = get(
                spec -> spec.pathParam("outputId", output.getId()),
                OUTPUT_RESULTS_DOWNLOAD);
        byte[] fileContent = downloadResponse.getBody().asByteArray();
        assertNotNull(fileContent, "Obsah PDF souboru nesmí být null");
        assertTrue(fileContent.length > 0, "Vygenerovaný PDF soubor nesmí být prázdný");
    }
}