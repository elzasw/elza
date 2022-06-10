package cz.tacr.elza.dataexchange.output.sections;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.filters.ExportFilter;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.LevelInfo;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;

public class LevelInfoDispatcher implements LoadDispatcher<LevelInfoImpl> {

    private final SectionOutputStream os;

    private final LevelInfoListener levelInfoListener;

    private LevelInfoImpl levelInfo;

    private ExportContext context;

    public LevelInfoDispatcher(ExportContext context,
                               SectionOutputStream os, LevelInfoListener levelInfoListener) {
        this.context = context;
        this.os = os;
        this.levelInfoListener = levelInfoListener;
    }

    @Override
    public void onLoadBegin() {
    }

    @Override
    public void onLoad(LevelInfoImpl result) {
        Validate.isTrue(levelInfo == null);
        levelInfo = result;
    }

    @Override
    public void onLoadEnd() {
        Validate.notNull(levelInfo);

        if (levelInfoListener != null) {
            levelInfoListener.onInit(levelInfo);
        }

        // filter level
        ExportFilter expFilter = context.getExportFilter();
        if (expFilter != null) {
            LevelInfo filteredLevelInfo = expFilter.processLevel(levelInfo);
            if (filteredLevelInfo == null) {
                // skip this level
                return;
            }

            // write filtered values
            os.addLevel(filteredLevelInfo);

        } else {
            // without filter write everything
            os.addLevel(levelInfo);
        }
    }
}
