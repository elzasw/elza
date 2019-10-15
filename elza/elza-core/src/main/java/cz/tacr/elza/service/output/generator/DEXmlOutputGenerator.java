package cz.tacr.elza.service.output.generator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.DEExportParams;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.dataexchange.output.DEExportService;
import cz.tacr.elza.dataexchange.output.sections.RootLevelDecorator;
import cz.tacr.elza.service.DmsService;

public class DEXmlOutputGenerator extends DmsOutputGenerator {

    private final DEExportService exportService;

    protected DEXmlOutputGenerator(EntityManager em, DmsService dmsService, DEExportService exportService) {
        super(em, dmsService);
        this.exportService = exportService;
    }

    @Override
    protected void generate(OutputStream os) throws IOException {
        DEExportParams exportParams = createExportParams();
        exportService.exportXmlData(os, exportParams);
    }

    private DEExportParams createExportParams() {
        List<Integer> rootNodeIds = params.getOutputNodeIds();

        FundSections fundSections = new FundSections();
        fundSections.setFundVersionId(params.getFundVersionId());
        fundSections.setRootNodeIds(rootNodeIds);
        fundSections.setMergeSections(true);
        // add output items to export root
        fundSections.setLevelInfoListener(new RootLevelDecorator(params.getOutputItems()));

        DEExportParams params = new DEExportParams();
        params.setFundsSections(Collections.singleton(fundSections));
        return params;
    }

    @Override
    public void close() throws IOException {

    }
}
