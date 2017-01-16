package cz.tacr.elza.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.ArrangementController.FaTreeParam;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.domain.vo.XmlImportType;

/**
 * Test exportu archivního souboru.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 16. 5. 2016
 */
public class XmlExportControllerTest extends AbstractControllerTest {

    protected final static String ALL_IN_ONE_XML = "all-in-one-import.xml";

    @Autowired
    private XmlExportController xmlExportController;

    @Test
    public void exportTest() throws IOException {
        // import počátečních dat
        RegScopeVO scope = getScope();
        importData(XmlImportControllerTest.getResourceFile(ALL_IN_ONE_XML), scope);

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
    private void importData(final File importFile, final RegScopeVO scope) {
        importXmlFile(null, null, XmlImportType.FUND, null, scope.getId(), importFile, null);
    }

    /**
     * Kontrola naimportovaných dat.
     */
    private void check() {
        List<ArrFundVO> funds = getFunds();

        Assert.isTrue(funds.size() == 1);

        ArrFundVO fund = funds.iterator().next();
        ArrFundVersionVO version = getOpenVersion(fund);

        FaTreeParam treeParam = new FaTreeParam();
        treeParam.setVersionId(version.getId());
        TreeData treeData = getFundTree(treeParam);
        List<ArrNodeVO> nodes = convertTreeNodes(treeData.getNodes());

        Assert.isTrue(nodes.size() == 4);
    }

    /** Export dat. */
    private File exportData(final ArrFundVO fund) throws IOException, FileNotFoundException {
        ArrFundVersionVO version = getOpenVersion(fund);

        File file = File.createTempFile("elza-export", ".xml");

        try (ServletOutputStream servletOutputStream = createOutputStream(file)) {
            MockHttpServletResponse response = createHttpResponse(servletOutputStream);
            xmlExportController.exportFund(response, version.getId(), null);
        }

        return file;
    }

    /** Kontrola odstranění archivního souboru. */
    private void checkNoData() {
        List<ArrFundVO> funds = getFunds();

        Assert.isTrue(funds.size() == 0);
    }

    private MockHttpServletResponse createHttpResponse(final ServletOutputStream servletOutputStream) {
        MockHttpServletResponse response = new MockHttpServletResponse() {

            @Override
            public ServletOutputStream getOutputStream() {
                return servletOutputStream;
            }
        };
        return response;
    }

    private ServletOutputStream createOutputStream(final File file) throws FileNotFoundException {
        FileOutputStream os = new FileOutputStream(file);
        ServletOutputStream servletOutputStream = new ServletOutputStream() {

            @Override
            public void write(final int b) throws IOException {
                os.write(b);

            }

            @Override
            public void setWriteListener(final WriteListener listener) {

            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void close() throws IOException {
                super.close();
                os.close();
            }
        };
        return servletOutputStream;
    }

    private RegScopeVO getScope() {
        List<RegScopeVO> scopes = getAllScopes();
        RegScopeVO scope = scopes.iterator().next();
        return scope;
    }
}
