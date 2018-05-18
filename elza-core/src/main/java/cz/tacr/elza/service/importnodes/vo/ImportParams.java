package cz.tacr.elza.service.importnodes.vo;

/**
 * Parametry vyřešení konfliktů pro importu.
 *
 * @since 19.07.2017
 */
public interface ImportParams {

    ConflictResolve getFileConflictResolve();

    ConflictResolve getStructuredConflictResolve();

}
