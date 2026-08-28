package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.lightcomp.kads.common.XmlContentException;

import cz.tacr.elza.api.AipProblemType;

/**
 * Unit tests of what is recorded about a failure of the AIP processing: what the user is told,
 * which file it is about, and what an administrator needs to trace it to its root.
 */
public class AipProblemTest {

    @Test
    void theUserIsToldTheDescribedCause() {
        Throwable failure = new IllegalStateException("wrapped",
                AipProblemException.metadata("Balíček neobsahuje soubor METS.xml"));

        AipProblem problem = AipProblem.of(failure);

        assertEquals(AipProblemType.METADATA_ERROR, problem.type());
        assertEquals("Balíček neobsahuje soubor METS.xml", problem.description());
        assertNull(problem.file());
    }

    @Test
    void theProblemKeepsTheFileItIsAbout() {
        Throwable failure = AipProblemException.metadata("Popis se nepodařilo načíst",
                "metadata/descriptive/pruvodka.xml", new XmlContentException("Očekáván <ead>"));

        assertEquals("metadata/descriptive/pruvodka.xml", AipProblem.of(failure).file());
    }

    @Test
    void theOutermostDescribedCauseWins() {
        Throwable failure = AipProblemException.metadata("Inherentní archivní popis se nepodařilo načíst",
                "pruvodka.xml", AipProblemException.metadata("Očekáván kořenový element <ead>"));

        // the outer one carries the context of the inner one
        assertEquals("Inherentní archivní popis se nepodařilo načíst", AipProblem.of(failure).description());
    }

    @Test
    void anUndescribedFailureIsReportedAsUnexpected() {
        Throwable failure = new IllegalStateException("processing failed",
                new NullPointerException("Cannot invoke \"Archdesc.getDid()\" because \"archdesc\" is null"));

        AipProblem problem = AipProblem.of(failure);

        // the kind of a failure nothing described is still the processing of the metadata
        assertEquals(AipProblemType.METADATA_ERROR, problem.type());
        assertTrue(problem.description().startsWith("Neočekávaná chyba při zpracování balíčku"), problem.description());
        assertTrue(problem.description().contains("protokolu aplikace"), problem.description());
        assertNull(problem.file());
    }

    @Test
    void anExceptionWithoutAMessageIsStillNamed() {
        assertTrue(AipProblem.of(new NullPointerException()).description().contains("NullPointerException"));
    }

    @Test
    void theDetailTracesTheFailureToItsRoot() {
        Throwable failure = AipProblemException.metadata("Popis se nepodařilo načíst", "pruvodka.xml",
                new XmlContentException("Očekáván kořenový element <ead>", new IOException("broken pipe")));

        String detail = AipProblem.of(failure).detail();

        assertTrue(detail.contains(AipProblemException.class.getName()), detail);
        assertTrue(detail.contains(XmlContentException.class.getName()), detail);
        assertTrue(detail.contains("java.io.IOException: broken pipe"), detail);
        // the root is last, the failure first
        assertTrue(detail.indexOf("broken pipe") > detail.indexOf("Očekáván"), detail);
    }

    @Test
    void aDerivedProblemHasNoDetailNorFile() {
        AipProblem problem = AipProblem.derived(AipProblemType.UNKNOWN_FUND, "Fond nebyl nalezen.");

        assertEquals("Fond nebyl nalezen.", problem.description());
        assertNull(problem.detail());
        assertNull(problem.file());
    }

    @Test
    void theReasonDoesNotRepeatThatSomethingFailed() {
        String reason = AipProblem.reason(new IllegalStateException("processing failed"));

        assertFalse(reason.startsWith("Neočekávaná chyba"), reason);
        assertTrue(reason.contains("processing failed"), reason);
    }

    @Test
    void theReasonOfAMalformedDocumentDescribesTheDocument() {
        String reason = AipProblem.reason(new IllegalStateException("wrapped",
                new XmlContentException("Očekáván kořenový element <ead>, soubor má <mets>")));

        assertEquals("Očekáván kořenový element <ead>, soubor má <mets>", reason);
    }
}
