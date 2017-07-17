package cz.tacr.elza.controller.vo;

/**
 * @since 17.7.2017
 */
public class CopyNodesParams extends CopyNodesValidate {

    private ConflictResolve filesConflictResolve;

    private ConflictResolve packetsConflictResolve;

    public ConflictResolve getFilesConflictResolve() {
        return filesConflictResolve;
    }

    public void setFilesConflictResolve(final ConflictResolve filesConflictResolve) {
        this.filesConflictResolve = filesConflictResolve;
    }

    public ConflictResolve getPacketsConflictResolve() {
        return packetsConflictResolve;
    }

    public void setPacketsConflictResolve(final ConflictResolve packetsConflictResolve) {
        this.packetsConflictResolve = packetsConflictResolve;
    }
}
