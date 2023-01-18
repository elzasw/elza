package cz.tacr.elza.config.export;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("elza.export")
public class ExportConfig {
    MapViewer mapviewer;

    public MapViewer getMapviewer() {
        return mapviewer;
    }

    public void setMapviewer(MapViewer mapviewer) {
        this.mapviewer = mapviewer;
    }
}
