package cz.tacr.elza.dataexchange.output.context;

import cz.tacr.elza.dataexchange.output.aps.AccessPointsReader;
import cz.tacr.elza.dataexchange.output.parties.PartiesReader;
import cz.tacr.elza.dataexchange.output.sections.SectionsReader;

public enum ExportPhase {
    SECTIONS {
        @Override
        public ExportReader createExportReader(ExportContext context, ExportInitHelper initHelper) {
            return new SectionsReader(context, initHelper);
        }
    },
    ACCESS_POINTS {
        @Override
        public ExportReader createExportReader(ExportContext context, ExportInitHelper initHelper) {
            return new AccessPointsReader(context, initHelper);
        }
    },
    PARTIES {
        @Override
        public ExportReader createExportReader(ExportContext context, ExportInitHelper initHelper) {
            return new PartiesReader(context, initHelper);
        }
    };
    // INSTITUTIONS, RELATIONS;

    public abstract ExportReader createExportReader(ExportContext context, ExportInitHelper initHelper);
}
