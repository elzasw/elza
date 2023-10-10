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

    /** User ID */
    userId: integer;

    /** username */
    user: string;

    /** Remote address */
    remoteAddr: string;
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

/**
 * States of ApState
 */
enum ApStateApproval {
    NEW,
    TO_APPROVE,
    APPROVED,
    TO_AMEND
}

/**
 * States of ApRevState
 */
enum RevisionState {
    ACTIVE,
    TO_APPROVE,
    TO_AMEND
}

/**
 * Data for changing state of ApAccessPoint
 */
model ApStateUpdate {
    /** id of type */
    typeId?: integer;
    /** id of scope */
    scopeId?: integer;
    /** comment */
    comment?: string;
    /** state of ApState */
    stateApproval: ApStateApproval;
}

/**
 * Data for changing revision state of ApAccessPoint
 */
model RevStateChange {
    /** id of type */
    typeId?: integer;
    /** comment */
    comment?: string;
    /** state of ApRevision */
    state: RevisionState;
}

/**
 * ItemData classes extensions
 */
model DataBit extends ItemData {
    bitValue: boolean;
}

model DataCoordinates extends ItemData {
    value: string;
}

model DataDate extends ItemData {
    value: offsetDateTime;
}

model DataDecimal extends ItemData {
    value: decimal;
}

model DataFileRef extends ItemData {
    fileId: integer;
}

model DataInteger extends ItemData {
    integerValue: integer;
}

model DataJsonTable extends ItemData {
    value: string;
}

model DataNull extends ItemData {
}

model DataRecordRef extends ItemData {
    recordId: integer;
    bindingId: integer;
}

model DataString extends ItemData {
    stringValue: string;
}

model DataStructureRef extends ItemData {
    structuredObjectId: integer;
}

model DataText extends ItemData {
    textValue: string;
}

model DataUnitdate extends ItemData {
    valueFrom: string;
    valueFromEstimated: boolean;
    valueTo: string;
    valueToEstimated: boolean;
    format: string;
    normalizedFrom: int64;
    normalizedTo: int64;
}

model DataUnitid extends ItemData {
    unitId: string;
}

model DataUriRef extends ItemData {
    schema: string;
    uriRefValue: string;
    description: string;
    nodeId: integer;
    refTemplateId: integer;
}

/**
 * Item data
 */
model ItemData {
    /** data id */
    dataId: integer;
    /** data type id */
    dataTypeId: integer;
}

/**
 * Type of change Item
 */
enum ChangeType {
    NEW,
    UPDATED,
    DELETED,
    ORIGINAL
}

/**
 * Data for item of ApPart
 */
model Item {
    /** id of item */
    itemId: integer;
    /** id of attribute value */
    objectId: integer;
    /** attribute type */
    typeId: integer;
    /** attribute specification */
    specId: integer;
    /** position */
    position: integer;
    /** type of change */
    changeType: ChangeType;
    /** item data */
    data: ItemData;
}

/**
 * Data for create/update ApPart and ReviPart
 */
model ApPartForm {
    /** id of part */
    partId?: integer;
    /** id of parent part */
    parentPartId?: integer;
    /** id of parent part revizion */
    revParentPartId?: integer;
    /** code type of part */
    partTypeCode: string;
    /** list of all attribute values */
    items: Item[];
}

/**
 * Method of replacing ApAccessPoint
 */
enum ReplaceType {
    SIMPLE,
    COPY_ALL
}

/**
 * Data for detele/replace ApAccessPoint
 */
model DeleteAccessPointDetail {
    /** Id of the entity being replaced */
    replacedBy?: string;
    /** method of replacing */
    replaceType?: ReplaceType;
}

/**
 * Automatically generated elements of the description
 */
model AutoValue {
    /** identifier of the type of the description element */
    itemTypeId?: integer;
    /** identifier of the specification of the description element */
    itemSpecId?: integer;
    /** value of the element */
    value?: string;
}

/**
 * List of automatically generated items
 */
model ResultAutoItems {
    items?: AutoValue[];
}

/**
 * Additional copy options
 */
model CopyAccessPointDetail {
    /** code of scope or id */
    scope: string;
    /** if the original AP should be replaced */
    replace?: boolean;
    /** list of description element IDs not to copy */
    skipItems?: integer[];
}

/**
 * Return value when copying ApAccessPoint
 */
model EntityRef {
    /** id of entity */
    id: string;
    /** visible name of entity */
    label: string;
    /** optional description of entity */
    note?: string;
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

@tag("accesspoints")
@route("/accesspoint")
namespace ElzaAPI.AccessPoint {
    /**
     * Invalidation of ApAccessPoint (delete/replace)
     */
    @route("/{id}")
    @delete op deleteAccessPoint(
        /** id of accesspoint */
        @path id: string,
        /** addition for method of invalidation */
        @body body?: DeleteAccessPointDetail
    ): {
         /** Pending, processing, finished */
         @Http.statusCode statusCode: 200;
       } | {
          /** ID does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException;
       };

    /**
     * List of automatically generated elements of the description
     */
    @route("/{id}/autoitems")
    @get op getAutoitems(
        /** id of accesspoint */
        @path id: string
    ): {
         /** Pending, processing, finished */
         @Http.statusCode statusCode: 200;
         @body data: ResultAutoItems;
       } | {
          /** ID does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException;
       };

    /**
     * Copying an archive entity
     */
    @route("{id}/copy")
    @put op copyAccessPoint(
        /** id of accesspoint */
        @path id: string,
        /** additional copy options */
        @body body: CopyAccessPointDetail
    ): {
         /** Pending, processing, finished */
         @Http.statusCode statusCode: 200;
         @body data: EntityRef;
       } | {
          /** ID does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException;
       };

    /**
     * Delete part of ApAccessPoint
     */
    @route("/{id}/part/{partId}")
    @delete op deletePart(
        /** id of accesspoint */
        @path id: integer,
        /** partId of ApAccessPoint */
        @path partId: integer,
        /** actual version of ApAccessPoint */
        @query apVersion?: integer
    ): integer;

    /**
     * Change state of ApAccessPoint
     */
    @route("/{id}/state")
    @put op changeState(
        /** id of accesspoint */
        @path id: integer,
        /** data for change ApState */
        @body body: ApStateUpdate;
        /** actual version of ApAccessPoint */
        @query apVersion?: integer,
    ): integer;

    /**
     * Set the preferred name of ApAccessPoint
     */
    @route("{id}/part/{partId}/prefer-name")
    @put op setPreferName(
        /** id of accesspoint */
        @path id: integer,
        /** partId of ApAccessPoint */
        @path partId: integer,
        /** actual version of ApAccessPoint */
        @query apVersion?: integer
    ): integer;

    /**
     * Change state of revision of ApAccessPoint
     */
    @route("/{id}/revision/state")
    @put op changeStateRevision(
        /** id of accesspoint */
        @path id: integer,
        /** data for change state of revision of ApAccessPoint */
        @body body: RevStateChange;
        /** actual version of ApAccessPoint */
        @query apVersion?: integer;
    ): integer;

    /**
     * Set the preferred name of revision of ApAccessPoint
     */
    @route("/{id}/revision/part/{partId}/prefer-name")
    @put op setPreferNameRevision(
        /** id of accesspoint */
        @path id: integer,
        /** partId of revision */
        @path partId: integer,
        /** actual version of ApAccessPoint */
        @query apVersion?: integer;
    ): integer;

    /**
     * Delete part of revision of ApAccessPoint
     */
    @route("/{id}/revision/part/{partId}")
    @delete op deleteRevisionPart(
        /** id of accesspoint */
        @path id: integer,
        /** partId of revision */
        @path partId: integer,
        /** actual version of ApAccessPoint */
        @query apVersion?: integer;
    ): {
         /** actual version of ApAccessPoint */
         @Http.statusCode statusCode: 200;
         @body data: integer;
       } | {
          /** ID does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException; 
       };

    /**
     * Merge ApRevision & ApAccessPoint
     */
    @route("/{id}/revision/merge")
    @post op mergeRevision(
        /** id of accesspoint */
        @path id: integer,
        /** data for change ApState */
        @body body: ApStateUpdate;
        /** actual version of ApAccessPoint */
        @query apVersion?: integer;
    ): integer;
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
         /** Pending, processing, finished */
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
