package cz.tacr.elza.service.da;

import org.springframework.lang.Nullable;

import cz.tacr.elza.api.AipProblemType;

/**
 * A problem of an AIP that the processing detected and can describe to the user.
 *
 * The kind of problem is the {@link AipProblemType} the exception carries, so a newly
 * detected kind - a broken checksum, an unexpected version - is a new constant of that enum
 * and a new factory method here, not a new exception class.
 *
 * The message is the text the user reads in the problem description of the AIP, so it names
 * what is wrong in their words. Everything the message cannot say is kept in the cause,
 * which {@link AipProblem} records as the technical detail.
 */
public class AipProblemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final AipProblemType problemType;

    /** Path of the file inside the package the problem is about; null when it is about none. */
    private final String file;

    public AipProblemException(AipProblemType problemType, String message, @Nullable String file,
                               @Nullable Throwable cause) {
        super(message, cause);
        this.problemType = problemType;
        this.file = file;
    }

    /** The metadata package could not be processed. */
    public static AipProblemException metadata(String message) {
        return new AipProblemException(AipProblemType.METADATA_ERROR, message, null, null);
    }

    /** The metadata package could not be processed because of one of its files. */
    public static AipProblemException metadata(String message, @Nullable String file, @Nullable Throwable cause) {
        return new AipProblemException(AipProblemType.METADATA_ERROR, message, file, cause);
    }

    public AipProblemType getProblemType() {
        return problemType;
    }

    @Nullable
    public String getFile() {
        return file;
    }
}
