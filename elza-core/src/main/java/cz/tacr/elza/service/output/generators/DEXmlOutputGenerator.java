package cz.tacr.elza.service.output.generators;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import cz.tacr.elza.dataexchange.output.DEExportParams;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.dataexchange.output.DEExportService;
import cz.tacr.elza.dataexchange.output.sections.RootLevelDecorator;
import cz.tacr.elza.domain.ArrNodeOutput;
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
        List<ArrNodeOutput> rootNodes = params.getOutputNodes();
        List<Integer> rootNodeIds = new ArrayList<>(rootNodes.size());
        rootNodes.forEach(n -> rootNodeIds.add(n.getNodeId()));

        FundSections fundSections = new FundSections();
        fundSections.setFundVersionId(params.getFundVersionId());
        fundSections.setRootNodeIds(rootNodeIds);
        fundSections.setMergeSections(true);
        // add output items to export root node
        fundSections.setLevelInfoListener(new RootLevelDecorator(params.getDirectItems()));

        DEExportParams params = new DEExportParams();
        params.setFundsSections(Collections.singleton(fundSections));
        return params;
    }
}
