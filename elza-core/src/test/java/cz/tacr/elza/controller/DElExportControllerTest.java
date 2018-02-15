package cz.tacr.elza.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

import cz.tacr.elza.controller.vo.ApScopeVO;
import org.junit.Assert;
import org.junit.Test;

import cz.tacr.elza.controller.ArrangementController.FaTreeParam;
import cz.tacr.elza.controller.DEExportController.DEExportParamsVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;

/**
 * Test exportu archivního souboru.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 16. 5. 2016
 */
public class DElExportControllerTest extends AbstractControllerTest {

    protected final static String ALL_IN_ONE_XML = "all-in-one-import.xml";

    @Test
    public void exportTest() throws IOException {
        // import počátečních dat
        ApScopeVO scope = getScope();
        importData(DEImportControllerTest.getResourceFile(ALL_IN_ONE_XML), scope);

        check();

        // export dat
        List<ArrFundVO> funds = getFunds();
        ArrFundVO fund = funds.iterator().next();
        File file = exportData(fund);

        // odstranění dat
        deleteFund(fund.getId());
        checkNoData();

        // import dat
        importData(file, scope);

        check();
    }

    /** Import dat z xml souboru. */
    private void importData(final File importFile, final ApScopeVO scope) {
        importXmlFile(null, scope.getId(), importFile);
    }

    /**
     * Kontrola naimportovaných dat.
     */
    private void check() {
        List<ArrFundVO> funds = getFunds();

        Assert.assertTrue(funds.size() == 1);

        ArrFundVO fund = funds.iterator().next();
        ArrFundVersionVO version = getOpenVersion(fund);

        FaTreeParam treeParam = new FaTreeParam();
        treeParam.setVersionId(version.getId());
        TreeData treeData = getFundTree(treeParam);
        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());

        Assert.assertTrue(nodes.size() == 4);
    }

    /** Export dat. */
    private File exportData(final ArrFundVO fund) throws IOException, FileNotFoundException {
        ArrFundVersionVO version = getOpenVersion(fund);

        Path path = Files.createTempFile("elza-export", ".xml");

        FundSections fundParams = new FundSections();
        fundParams.setFundVersionId(version.getId());
        DEExportParamsVO params = new DEExportParamsVO();
        params.setFundsSections(Collections.singleton(fundParams));

        try (InputStream is = post(spec -> spec.body(params), DE_EXPORT).asInputStream()) {
            Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
        }

        return path.toFile();
    }

    /** Kontrola odstranění archivního souboru. */
    private void checkNoData() {
        List<ArrFundVO> funds = getFunds();

        Assert.assertTrue(funds.size() == 0);
    }

    private ApScopeVO getScope() {
        List<ApScopeVO> scopes = getAllScopes();
        ApScopeVO scope = scopes.iterator().next();
        return scope;
    }
}
