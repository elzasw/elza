package cz.tacr.elza.service.da;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.lang.Nullable;

import com.lightcomp.kads.common.XmlContentException;

import cz.tacr.elza.api.AipProblemType;

/**
 * What is recorded about a problem of an AIP, whatever kind of problem it is: what the user
 * is told, what an administrator needs to diagnose it, and which file of the package it is
 * about.
 *
 * @param type what kind of problem it is
 * @param description the problem in the words the user reads
 * @param detail the chain of causes, for the diagnostics; null for a derived problem, which
 *            has no cause to trace
 * @param file path of the file inside the package the problem is about; null when the
 *            problem is not about a single file
 */
public record AipProblem(AipProblemType type, String description, @Nullable String detail, @Nullable String file) {

    /** Separates the causes in the technical detail, from the failure towards its root. */
    private static final String CAUSE_SEPARATOR = "\n  příčina: ";

    /** A problem derived from the state of the AIP - there is no failure behind it to trace. */
    public static AipProblem derived(AipProblemType type, String description) {
        return new AipProblem(type, description, null, null);
    }

    /**
     * The problem the processing of a package failed with.
     *
     * The message of the exception itself is not enough to describe it: only some exceptions
     * describe the defect of the package - the rest carry a message written for a developer,
     * or no message at all - and the exception the processing fails on is usually not the one
     * that says what went wrong, because that one is somewhere down the chain of causes.
     */
    public static AipProblem of(Throwable failure) {
        AipProblemException described = describedCause(failure);
        String description = described != null
                ? described.getMessage()
                : "Neočekávaná chyba při zpracování balíčku: " + shortDescription(rootCause(failure))
                        + ". Podrobnosti jsou v protokolu aplikace.";
        return new AipProblem(
                described != null ? described.getProblemType() : AipProblemType.METADATA_ERROR,
                description,
                causeChain(failure),
                described != null ? described.getFile() : null);
    }

    /**
     * Reason of the failure to be embedded into a message that already names its own context,
     * so it stays a reason and does not repeat that something failed.
     */
    public static String reason(Throwable failure) {
        AipProblemException described = describedCause(failure);
        if (described != null) {
            return described.getMessage();
        }
        Throwable xmlDefect = firstCauseOfType(failure, XmlContentException.class);
        return xmlDefect != null ? xmlDefect.getMessage() : shortDescription(rootCause(failure));
    }

    /**
     * Every cause from the failure towards its root, so a problem can be traced to the place
     * it originated in without the log of the processing.
     */
    private static String causeChain(Throwable failure) {
        List<Throwable> causes = ExceptionUtils.getThrowableList(failure);
        return causes.stream()
                .map(AipProblem::shortDescription)
                .collect(Collectors.joining(CAUSE_SEPARATOR));
    }

    /**
     * The first cause that describes the problem in the words the user reads, or null when no
     * cause in the chain was written to be read by them.
     */
    @Nullable
    private static AipProblemException describedCause(Throwable failure) {
        AipProblemException described = firstCauseOfType(failure, AipProblemException.class);
        return described != null && StringUtils.isNotBlank(described.getMessage()) ? described : null;
    }

    @Nullable
    private static <T extends Throwable> T firstCauseOfType(Throwable failure, Class<T> type) {
        for (Throwable cause : ExceptionUtils.getThrowableList(failure)) {
            if (type.isInstance(cause)) {
                return type.cast(cause);
            }
        }
        return null;
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable rootCause = ExceptionUtils.getRootCause(failure);
        return rootCause == null ? failure : rootCause;
    }

    private static String shortDescription(Throwable t) {
        String message = t.getMessage();
        return StringUtils.isBlank(message)
                ? t.getClass().getName()
                : t.getClass().getName() + ": " + message;
    }
}
