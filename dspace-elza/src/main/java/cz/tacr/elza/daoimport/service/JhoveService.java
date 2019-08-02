package cz.tacr.elza.daoimport.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.mcgath.jhove.module.PngModule;

import edu.harvard.hul.ois.jhove.App;
import edu.harvard.hul.ois.jhove.JhoveBase;
import edu.harvard.hul.ois.jhove.JhoveException;
import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.RepInfo;
import edu.harvard.hul.ois.jhove.handler.XmlHandler;
import edu.harvard.hul.ois.jhove.module.AiffModule;
import edu.harvard.hul.ois.jhove.module.AsciiModule;
import edu.harvard.hul.ois.jhove.module.BytestreamModule;
import edu.harvard.hul.ois.jhove.module.GifModule;
import edu.harvard.hul.ois.jhove.module.GzipModule;
import edu.harvard.hul.ois.jhove.module.HtmlModule;
import edu.harvard.hul.ois.jhove.module.Jpeg2000Module;
import edu.harvard.hul.ois.jhove.module.JpegModule;
import edu.harvard.hul.ois.jhove.module.PdfModule;
import edu.harvard.hul.ois.jhove.module.TiffModule;
import edu.harvard.hul.ois.jhove.module.Utf8Module;
import edu.harvard.hul.ois.jhove.module.WarcModule;
import edu.harvard.hul.ois.jhove.module.WaveModule;
import edu.harvard.hul.ois.jhove.module.XmlModule;

@Service
public class JhoveService {

    private static Logger log = Logger.getLogger(JhoveService.class);

    private List<Module> modules = new LinkedList<>();

    public boolean generateMetadata(final Path file, final BufferedWriter protocol, final Path destPath) throws IOException {
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
                    BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
                    repInfo.setCreated(new Date(fileAttributes.creationTime().toMillis()));

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
