package cz.tacr.elza.dataexchange.output.writer;

/**
 * Reference to a digital archival object connected to a level, already resolved to the
 * values written into the export.
 *
 * The reference can originate either from a native DAO ({@link cz.tacr.elza.domain.ArrDao})
 * or from a digital-archive link to an AIP ({@link cz.tacr.elza.domain.DaAip} / a selected
 * {@link cz.tacr.elza.domain.DaDao} part). Both are normalized to the same triple:
 * repository code, object id and an optional part identifier.
 */
public class DaoInfo {

    private final String repositoryCode;

    private final String objectId;

    private final String part;

    public DaoInfo(String repositoryCode, String objectId, String part) {
        this.repositoryCode = repositoryCode;
        this.objectId = objectId;
        this.part = part;
    }

    /**
     * Code of the repository where the digital object is stored (schema element {@code rep}).
     */
    public String getRepositoryCode() {
        return repositoryCode;
    }

    /**
     * Id of the object in the repository (schema element {@code doid}).
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * Identifier of the relevant part of the digital object (schema element {@code part}),
     * or null when the reference is not scoped to a specific part.
     */
    public String getPart() {
        return part;
    }
}
