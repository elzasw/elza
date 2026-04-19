package cz.tacr.elza.config.export;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("elza.export")
public class ExportConfig {
    MapViewer mapviewer;

    /**
     * Testovací přepínač výstupního formátu pro Jasper generátor.
     * Výchozí PDF zachovává existující chování; ostatní hodnoty (DOCX, RTF, ODT)
     * nahradí PDF exportér a přeskočí slučování PDF příloh (pdfAttProvider).
     */
    private JasperFormat jasperFormat = JasperFormat.PDF;

    public MapViewer getMapviewer() {
        return mapviewer;
    }

    public void setMapviewer(MapViewer mapviewer) {
        this.mapviewer = mapviewer;
    }

    public JasperFormat getJasperFormat() {
        return jasperFormat;
    }

    public void setJasperFormat(JasperFormat jasperFormat) {
        this.jasperFormat = jasperFormat;
    }
}
