package cz.tacr.elza.daoimport.service;

import com.mcgath.jhove.module.PngModule;
import cz.tacr.elza.daoimport.schema.dao.Dao;
import cz.tacr.elza.daoimport.service.vo.MetadataInfo;
import edu.harvard.hul.ois.jhove.*;
import edu.harvard.hul.ois.jhove.handler.XmlHandler;
import edu.harvard.hul.ois.jhove.module.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

@Service
public class JhoveService {

    private static Logger log = Logger.getLogger(JhoveService.class);

    private List<Module> modules = new LinkedList<>();

    public boolean generateMetadata(final Path file, final BufferedWriter protocol, final Path destPath, final MetadataInfo metadataInfo) throws IOException {
        boolean moduleFound = false;
        try {
            File f = file.toAbsolutePath().toFile();
            for (Module m : modules) {
                FileInputStream inputStream = new FileInputStream(f);
                RepInfo repInfo = new RepInfo("");
                m.checkSignatures(f, inputStream, repInfo);
                IOUtils.closeQuietly(inputStream);
                if (repInfo.getWellFormed() > 0 && repInfo.isConsistent() && !repInfo.getSigMatch().isEmpty()) {
                    moduleFound = true;

                    protocol.write("Soubor " + f.getAbsolutePath() + " byl identifikován modulem " + m.getName());
                    protocol.newLine();

                    parseMetadata(f, m, repInfo);
                    if (metadataInfo != null) {
                        if (metadataInfo.getMimeType() != null ) {
                            repInfo.setMimeType(metadataInfo.getMimeType() );
                        }
                        if (metadataInfo.getCheckSum()  != null ) {
                            repInfo.setMimeType(metadataInfo.getCheckSum());
                        }
                    }

                    saveMetadataFile(destPath, repInfo);

                    break;
                }
            }

            if (!moduleFound) {
                protocol.write("Soubor " + f.getAbsolutePath() + " se nepodařilo identifikovat a proto nemohla být vygenerována metadata.");
            }
        } catch (JhoveException|IOException e) {
            protocol.write("Při načítání metadat souboru " + file.toAbsolutePath() + " nastala chyba - " + e.getMessage());
            protocol.write(ExceptionUtils.getStackTrace(e));
            protocol.newLine();
        }

        return moduleFound;
    }

    private void parseMetadata(File f, Module m, RepInfo repInfo) throws IOException {
        InputStream is = new FileInputStream(f);
        m.parse(is, repInfo, 0);
        IOUtils.closeQuietly(is);
    }

    private void saveMetadataFile(Path destPath, RepInfo repInfo) throws IOException, JhoveException {
        FileWriter fileWriter = new FileWriter(destPath.toFile());
        PrintWriter printWriter = new PrintWriter(fileWriter);

        XmlHandler xmlHandler = new XmlHandler();
        xmlHandler.setBase(new JhoveBase());
        xmlHandler.setApp(App.newAppWithName("Jhove"));
        xmlHandler.setWriter(printWriter);
        xmlHandler.setEncoding(CharEncoding.UTF_8);
        xmlHandler.showHeader();
        xmlHandler.show(repInfo);
        xmlHandler.showFooter();

        IOUtils.closeQuietly(printWriter);
    }

    @PostConstruct
    private void init() {
        JhoveBase jhoveBase = null;
        try {
            jhoveBase = new JhoveBase();
        } catch (JhoveException e) {
            // nic se neděje Jhove nebude nakonfigurován a nebude se používat při importu
            log.error("Chyba při inicializaci JHOVE.", e);
            return;
        }
        Module m;

        m = new AiffModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new AsciiModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new GifModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new GzipModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new HtmlModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new JpegModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new Jpeg2000Module();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new PdfModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new PngModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new TiffModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new Utf8Module();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new WarcModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new WaveModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new XmlModule();
        m.setBase(jhoveBase);
        modules.add(m);

        m = new BytestreamModule();
        m.setBase(jhoveBase);
        modules.add(m);
    }
}
