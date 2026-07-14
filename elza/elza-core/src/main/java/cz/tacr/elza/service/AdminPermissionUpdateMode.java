package cz.tacr.elza.service;

/**
 * Strategy for updating fonds administrator permissions (users and groups)
 * when a fonds is modified, primarily through the web service.
 */
public enum AdminPermissionUpdateMode {

    /**
     * Supplied users/groups are added and any existing administrator permissions
     * not present in the supplied list are removed, so the resulting permissions
     * exactly match the supplied list.
     */
    FULL_SYNC,

    /**
     * Supplied users/groups are added when missing; existing administrator
     * permissions are kept. This allows an administrator to grant extra group or
     * user permissions on a fonds without a web service update removing them.
     */
    ADD_ONLY,
    
    /**
     * Do not synchronize permissions.
     */
    NO_SYNC
}
