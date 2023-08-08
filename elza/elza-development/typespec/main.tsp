import "@typespec/rest";
import "@typespec/openapi";
import "@typespec/openapi3";

using TypeSpec.Http;
using TypeSpec.Rest;

/** Basic statistics about Elza instance */
model AdminInfo {
    /** Number of funds */
    funds: int32;

    /** Number of valid levels */
    levels: int32;

    /** Number of valid access points */
    accessPoints: int32;

    /** Number of users */
    users: int32;

    /** Number of logged users */
    loggedUsers: int32;
}

/** Logged user */
model LoggedUser {

    /** username */
    user: string;
}

/** List of logged users */
model LoggedUsers {

    /** User list */
    users: LoggedUser[];

    /** Total number of logged users */
    totalCount: int32;
}

model FsRepo {
    /** Repository ID */
    fsRepoId: int32;
    /** Optional code of repository */
    code?: string;
    /** Repository name */
    name: string;
    /** Repository path */
    path: string;
}

model FundIdParameter {
    /** Fund ID*/
    @path
    fundId: integer;
}

model NodeIdParameter {
    /** Node ID*/
    @path
    nodeId: integer;
}

model FsRepoIdParameter {
    /** FileSystem repository ID*/
    @path
    fsrepoId: integer;
}

/**
 * File system item type
 */
enum FsItemType {
    /** Folder/directory */
    FOLDER,
    /** Standard file */
    FILE,
}

/** Single file system item */
model FsItem {
    /** Type of item: file or folder */
    itemType: FsItemType;
    /** File or folder name */
    name: string;
    /** File size */
    size?: integer;
    /** Time of last change */
    lastChange: offsetDateTime;
}

/**
 * Collection of file system items
 */
model FsItems {
    items: FsItem[];
    /** Optional string to request next items. It is used if there are more items. */
    lastKey?: string;
}

/**
 * Parameters of archive file export
 */
model ExportParams {
    apIds?: integer[];
    fundsSections?: FundSections[];
    exportFilterId?: integer;
    includeUUID?: boolean = true;
    includeAccessPoints?: boolean = true;
}

enum ExportRequestState {
    PENDING,
    PREPARING,
    FINISHED
}

/**
 * Status of export
 */
model ExportRequestStatus {
    state: ExportRequestState;
    progress?: integer;
}

/**
 * Specifies multiple sections for fund version
 */
model FundSections {
	fundVersionId: integer;
	mergeSections?: boolean = false;
	rootNodeIds?: integer[];
}

/**
 * Error specification for the client
 */
model BaseException {
	type: string;
	code: string;
	message?: string;
	level?: string;
	properties?: {};
	stackTrace?: string;
}

@route("/daorepository")
namespace ElzaAPI.DaoRepository {
    op fsRepos(): FsRepo[];
}

@tag("admin")
@route("/admin")
namespace ElzaAPI.Admin {

    /** Creating missing items in arr_cached_node */
    @route("admin/arrangement/missing-cached-nodes")
    op syncNodeCache(): { 
        @Http.statusCode statusCode: 200;
    };

    /**
     * Return basic statistical info
     */
    @route("info")
    op info(): AdminInfo;

    /** Return list of logged users */
    @route("info/logged-users")
    op loggedUsers(): LoggedUsers;    
}

@tag("funds")
@route("/fund")
namespace ElzaAPI.Fund {
    /**
     * Return list of filesystem repositories
     */
    @route("{fundId}/fsrepos")
    op fsRepos(...FundIdParameter): FsRepo[];

    @route("{fundId}/fsrepo/{fsrepoId}/linkitem/{nodeId}")
    @put op fsCreateDAOLink (
        ...FundIdParameter,
        ...FsRepoIdParameter,
        ...NodeIdParameter,
        @query path?: string
    ): integer;

    @route("{fundId}/fsrepo/{fsrepoId}/items")
    op fsRepoItems(
        ...FundIdParameter,
        ...FsRepoIdParameter,
        /** Optional filter to return only folders or files. If not specified all items are returned */
        @query filterType?: FsItemType,
        /** Optional path in the repository */
        @query path?: string,
        /** Optional parameter to return next batch of items. See FsItems for more details. */
        @query lastKey?: string
    ): FsItems;

    @route("{fundId}/fsrepo/{fsrepoId}/item-data")
    op fsRepoItemData(
        ...FundIdParameter,
        ...FsRepoIdParameter,
        /** file path in the repository */
        @query path: string
    ): {
        @header contentType: "*/*";
        @body data: bytes;
    };
}

@tag("io")
@route("/io")
namespace ElzaAPI.Io {
    /**
     * Create export file of funds or access points
     */ 
    @route("/export")
    @post op exportRequest(
        /** Export request parameters */
        @body body: ExportParams
	): integer;

    /**
     * Getting the export status by request id
     */
	@route("/export-status/{requestId}")
	@get op getExportStatus(
		@path requestId: integer
	): {
       } | {
         /** Processing */
         @Http.statusCode statusCode: 102;
         @body data: ExportRequestStatus;
       } | {
         /** Finishing */
         @Http.statusCode statusCode: 200;
         @body data: ExportRequestStatus;
       } | {
          /** ID does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException;
       } | {
          /** Failed to export requested data */
          @Http.statusCode statusCode: 500;
          @body data: BaseException;
       };

    /**
     * Getting the generated file by request id
     */
	@route("/file/{requestId}")
	@get op getExportFile(
		@path requestId: integer
	): {
        @header contentType: "*/*";
        @body data: bytes;
       } | {
         /** Processing */ 
         @Http.statusCode statusCode: 102;
       } | {
          /** ID does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException; 
       } | { 
          /** Failed to export requested data */
          @Http.statusCode statusCode: 500;
          @body data: BaseException; 
       };
}

@service({
    title: "ELZA API",
    version: "1.0.0",
})
namespace ElzaAPI {

}
