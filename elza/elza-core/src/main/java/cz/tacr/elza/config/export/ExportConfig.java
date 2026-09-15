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

    /**
     * Počet stran výstupu držených v paměti při generování Jasper šablony.
     *
     * Ostatní strany jsou průběžně odkládány do odkládacího souboru na disku,
     * takže spotřeba paměti neroste s délkou výstupu. Hodnota 0 (nebo méně)
     * virtualizaci vypne a celý dokument zůstane v paměti - pro rozsáhlé
     * výstupy (tisíce stran) to vede k OutOfMemoryError.
     */
    private int jasperPageCacheSize = 100;

    /**
     * Velikost bloku odkládacího souboru v kB.
     */
    private int jasperSwapBlockSizeKb = 1024;

    /**
     * O kolik bloků se odkládací soubor rozšiřuje, když je potřeba místo.
     */
    private int jasperSwapMinGrowCount = 100;

    /**
     * Počet archivních entit držených v paměti při generování jednoho výstupu.
     *
     * Prvky popisu se převádějí hned při načtení jednotky popisu, takže každý odkaz na
     * archivní entitu ji do výstupu přinese, i když ji šablona nevytiskne. Bez omezení
     * by jejich počet rostl s počtem odkazovaných entit ve fondu. Hodnota 0 (nebo méně)
     * omezení vypne.
     */
    private int outputRecordCacheSize = 1000;

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

    public int getJasperPageCacheSize() {
        return jasperPageCacheSize;
    }

    public void setJasperPageCacheSize(int jasperPageCacheSize) {
        this.jasperPageCacheSize = jasperPageCacheSize;
    }

    public int getJasperSwapBlockSizeKb() {
        return jasperSwapBlockSizeKb;
    }

    public void setJasperSwapBlockSizeKb(int jasperSwapBlockSizeKb) {
        this.jasperSwapBlockSizeKb = jasperSwapBlockSizeKb;
    }

    public int getJasperSwapMinGrowCount() {
        return jasperSwapMinGrowCount;
    }

    public void setJasperSwapMinGrowCount(int jasperSwapMinGrowCount) {
        this.jasperSwapMinGrowCount = jasperSwapMinGrowCount;
    }

    public int getOutputRecordCacheSize() {
        return outputRecordCacheSize;
    }

    public void setOutputRecordCacheSize(int outputRecordCacheSize) {
        this.outputRecordCacheSize = outputRecordCacheSize;
    }
}
