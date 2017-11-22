package cz.tacr.elza.dataexchange.output.sections;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;

public class LevelInfoDispatcher implements LoadDispatcher<ExportLevelInfo> {

    private final SectionOutputStream os;

    private final ExportLevelInfoListener levelInfoListener;

    private ExportLevelInfo levelInfo;

    public LevelInfoDispatcher(SectionOutputStream os, ExportLevelInfoListener levelInfoListener) {
        this.os = os;
        this.levelInfoListener = levelInfoListener;
    }

    @Override
    public void onLoadBegin() {
    }

    @Override
    public void onLoad(ExportLevelInfo result) {
        Validate.isTrue(levelInfo == null);
        levelInfo = result;
    }

    @Override
    public void onLoadEnd() {
        Validate.notNull(levelInfo);

        if (levelInfoListener != null) {
            levelInfoListener.onInit(levelInfo);
        }
        os.addLevel(levelInfo);
    }
}
