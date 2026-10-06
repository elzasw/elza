package cz.tacr.elza.service.output;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.sf.jasperreports.engine.JasperCompileManager;

/**
 * Every Jasper template of the ZP2015 rules package has to compile with the
 * JasperReports extensions on the classpath (e.g. barcode components).
 */
class JasperTemplateCompileTest {

    /** Copied to the test classpath by elza-core/pom.xml. */
    private static final String PACKAGE_DIR = "rules-cz-zp2015";

    static Stream<Path> templates() throws Exception {
        URL url = JasperTemplateCompileTest.class.getClassLoader().getResource(PACKAGE_DIR);
        assertNotNull(url, "Package not found on classpath: " + PACKAGE_DIR);
        List<Path> templates;
        try (Stream<Path> files = Files.walk(Path.of(url.toURI()))) {
            templates = files.filter(p -> p.toString().endsWith(".jrxml")).toList();
        }
        assertFalse(templates.isEmpty(), "No Jasper templates found");
        return templates.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templates")
    void compiles(Path template) throws Exception {
        try (InputStream is = Files.newInputStream(template)) {
            assertNotNull(JasperCompileManager.compileReport(is));
        }
    }
}
