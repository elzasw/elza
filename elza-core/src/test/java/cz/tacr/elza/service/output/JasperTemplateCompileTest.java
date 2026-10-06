package cz.tacr.elza.service.output;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.sf.jasperreports.engine.JasperCompileManager;

/**
 * Every Jasper template of the rules packages has to compile with the
 * JasperReports extensions on the classpath (e.g. barcode components).
 */
class JasperTemplateCompileTest {

    /** Copied to the test classpath by elza-core/pom.xml. */
    private static final List<String> PACKAGE_DIRS = List.of("rules-cz-zp2015", "rules-simple-dev");

    static Stream<Path> templates() throws Exception {
        List<Path> templates = new ArrayList<>();
        for (String packageDir : PACKAGE_DIRS) {
            URL url = JasperTemplateCompileTest.class.getClassLoader().getResource(packageDir);
            assertNotNull(url, "Package not found on classpath: " + packageDir);
            try (Stream<Path> files = Files.walk(Path.of(url.toURI()))) {
                List<Path> jrxmls = files.filter(p -> p.toString().endsWith(".jrxml")).toList();
                assertFalse(jrxmls.isEmpty(), "No Jasper templates found in " + packageDir);
                templates.addAll(jrxmls);
            }
        }
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
