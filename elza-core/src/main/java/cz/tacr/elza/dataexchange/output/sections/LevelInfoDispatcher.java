package cz.tacr.elza.dataexchange.output.sections;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;

public class LevelInfoDispatcher implements LoadDispatcher<LevelInfoImpl> {

    private final SectionOutputStream os;

    private final LevelInfoListener levelInfoListener;

    private LevelInfoImpl levelInfo;

    public LevelInfoDispatcher(SectionOutputStream os, LevelInfoListener levelInfoListener) {
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
        os.addLevel(levelInfo);
    }
}
