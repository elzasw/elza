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

/**
 * Archivní soubor
 */
model Fund {
    /** Identifikátor */
    id: integer;
    /** UUID - identifikátor AS */ 
    uuid: string;
    /** Název */
    name: string;
    /** Identifikátor instituce (vždy kód instituce) */
    institutionIdentifier: string;
    /** Interní kód */
    internalCode?: string;
    /** Datum založení */
    createDate?: offsetDateTime;
    /** Číslo archivního souboru */
    fundNumber?: integer;
    /** Datace */
    unitdate?: string;
    /** Značka archivního souboru */
    mark?: string;
}

/**
 * Zakládaný archivní souboru
 */
model FundDetail {
    /** Identifikátor */
    id: integer;
    /** UUID - identifikátor AS */ 
    uuid: string;
    /** Název */
    name: string;
    /** Identifikátor instituce (vždy kód instituce) */
    institutionIdentifier: string;
    /** Interní kód */
    internalCode?: string;
    /** Datum založení */
    createDate?: offsetDateTime;
    /** Číslo archivního souboru */
    fundNumber?: integer;
    /** Datace */
    unitdate?: string;
    /** Značka archivního souboru */
    mark?: string;
    /** Seznam oblastí entit */
    scopes: string[];
}

/**
 * Zakládaný archivní souboru
 */ 
model CreateFund {
    /** Název */
    name: string;
    /** Kód pravidel */
    ruleSetCode: string;
    /** Identifikátor instituce (podpora pro kód instituce i UUID přístupového bodu) */
    institutionIdentifier: string;
    /** Interní kód */
    internalCode?: string;
    /** UUID - identifikátor AS (pokud není vyplněno, bude vygenerováno) */
    uuid?: string;
    /** Číslo archivního souboru */
    fundNumber?: integer;
    /** Datace */
    unitdate?: string;
    /** Značka archivního souboru */
    mark?: string;
    /** Seznam uživatelů, kteří mají administrátorská oprávnění pro zakládaný AS */
    adminUsers?: integer[];
    /** Seznam skupin, kteří mají administrátorská oprávnění pro zakládaný AS */
    adminGroups?: integer[];
    /** Seznam oblastí entit */
    @minItems(1) scopes: string[];
}

/**
 * Data k importu
 */ 
model ImportFundData {
    /** Import type. Only 'CSV' is supported */
    importType: string;
    /** File to upload */
    dataFile: bytes;
}

/**
 * Změny archivního souboru
 */
model UpdateFund {
    /** Název */
    name?: string;
    /** Kód pravidel */
    ruleSetCode?: string;
    /** Identifikátor instituce (podpora pro kód instituce i UUID přístupového bodu) */
    institutionIdentifier?: string;
    /** Interní kód */
    internalCode?: string;
    /** Číslo archivního souboru */
    fundNumber?: integer;
    /** Datace */
    unitdate?: string;
    /** Značka archivního souboru */
    mark?: string;
    /** Seznam oblastí entit */
    scopes?: string[];
}

/**
 * Výsledek vyhledávání AS
 */
model FindFundsResult {
    funds: Fund[];
    totalCount: integer;
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

model FundVersionIdParameter {
    /** Fund ID Version*/
    @path
    fundVersionId: integer;
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
 * Fund node item type id and quantity 
 */
model UsedItemType {
	rulItemTypeId: integer;
	count: integer;
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
    type: "DataBit";
    bitValue: boolean;
}

model DataCoordinates extends ItemData {
    type: "DataCoordinates";
    value: string;
}

model DataDate extends ItemData {
    type: "DataDate";
    value: plainDate;
}

model DataDecimal extends ItemData {
    type: "DataDecimal";
    value: decimal;
}

model DataFileRef extends ItemData {
    type: "DataFileRef";
    fileId: integer;
}

model DataInteger extends ItemData {
    type: "DataInteger";
    integerValue: integer;
}

model DataJsonTable extends ItemData {
    type: "DataJsonTable";
    value: string;
}

model DataNull extends ItemData {
    type: "DataNull";
}

model DataRecordRef extends ItemData {
    type: "DataRecordRef";
    value: integer;
}

model DataString extends ItemData {
    type: "DataString";
    stringValue: string;
}

model DataStructureRef extends ItemData {
    type: "DataStructureRef";
    structuredObjectId: integer;
}

model DataText extends ItemData {
    type: "DataText";
    textValue: string;
}

model DataFormattedText extends ItemData {
    type: "DataFormattedText";
    value: string;
}

model DataUnitdate extends ItemData {
    type: "DataUnitdate";
    value: string;
}

model DataUnitid extends ItemData {
    type: "DataUnitid";
    unitId: string;
}

model DataUriRef extends ItemData {
    type: "DataUriRef";
    nodeId?: integer;
    value: string;
    description?: string;
    refTemplateId?: integer;
}

/**
 * Item data
 */
@discriminator("type")
model ItemData {
    /** data id */
    dataId?: integer;
    /** data type id */
    dataTypeId?: integer;
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
 * Výsledek vytvoření nebo aktualizace ArrDescItem
 */
model ItemDataResult {
    /** parent node */
    parent?: Node;
    /** item */
    item?: NodeItem;
}

/**
 * Náhrada za třídu ArrNodeVO
 */
model Node {
    /** id of node */
    id: integer;
    /** node version */
    version: integer;
    /** node uuid */
    uuid: string;
}

/**
 * Náhrada za třídu ArrItemVO
 */
model NodeItem {
    /** id of item */
    id?: integer;
    /** id value of item */
    itemObjectId?: integer;
    /** pozition */
    position?: integer;
    /** no value */
    undefined?: boolean;
    /** id type of item */
    itemTypeId?: integer;
    /** id spec of item */
    itemSpecId?: integer;
    /** value cannot be changed */
    readOnly?: boolean;
    /** id node */
    nodeId?: integer;
    /** node version */
    nodeVersion?: integer;
    /** if inheritance is prohibited - true */
    inhibited?: boolean;
    /** item data */
    data?: ItemData;
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

/**
 * Pairs name-value for external system and user
 */
model ExtSystemProperty {
    /** ext system property id */
    id?: integer;
    /** user id */
    userId?: integer;
    /** external system id */
    extSystemId: integer,
    /** name of property */
    name: string;
    /** value of property */
    value: string;
}

@tag("admin")
@route("/admin")
namespace ElzaAPI.Admin {

    /** Creating missing items in arr_cached_node */
    @route("arrangement/missing-cached-nodes")
    op syncNodeCache(): { 
        @Http.statusCode statusCode: 200;
    };

    model CopyPermissionParams {
        fromUserId: integer
    };

    /**
     * Copy user permissions from another user
     * 
     * Method will append all permissions to given user
     * @param userId ID of target user
     */
    @route("user/{userId}/copy-permissions")
    @post op copyPermissions(
        @path userId: integer,
        @body params: CopyPermissionParams
    ): {
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

@route("/daorepository")
namespace ElzaAPI.DaoRepository {
    op fsRepos(): FsRepo[];
}

@tag("externalsystems")
@route("/extsystem")
namespace ElzaAPI.ExternalSystem {
    /**
     * Return list of all properties of external system
     */
    @route("property")
    op allProperties(
        /** external system id */
        @query extSystemId?: integer,
        /** user id */
        @query userId?: integer
    ): ExtSystemProperty[];

    /**
     * Store one or more property of external system
     */
    @route("property")
    @post op storeProperties(
        /** request body */
        @body body: ExtSystemProperty[]
    ): {
         /** Pending, processing, finished */
         @Http.statusCode statusCode: 200;
       } | {
          /** Id does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException;
       };

    /**
     * Delete one property of external system
     */
    @route("property")
    @delete op deleteProperties(
        /** external system property id */
        @body extSysPropertyId: integer[]
    ): {
         /** Pending, processing, finished */
         @Http.statusCode statusCode: 200;
       } | {
          /** Id does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException;
       };
}

@tag("funds")
@route("/fund")
namespace ElzaAPI.fund {
    /**
     * Created a new archive file (AS)
     */
    @post op createFund(
        @body body: CreateFund;
    ): Fund | {
          /** Invalid data for AS creation */
          @Http.statusCode statusCode: 400;
    };

    /**
     * Search for archive files (AS)
     */
    @get op findFunds(
        /** vyhledávaný výraz */
        @query fulltext?: string;
        /** identifikátor instituce (podpora pro kód instituce i UUID přístupového bodu) */ 
        @query institutionIdentifier?: string;
        /** maximální počet výsledků na stránku */
        @query max?: integer = 200;
        /** Od kolikátého záznamu se mají výsledky vracet (0 = žádné se nepřeskočí) */
        @query from?: integer = 0;
    ): FindFundsResult;

    /**
     * Getting the AS detail
     */
    @route("/{id}")
    @get op getFund(
        /** fund ID. Might be ID or fund UUID */
        @path id: string;
    ): FundDetail | {
          @Http.statusCode statusCode: 404;
    };

    /**
     * Update of archive file (AS)
     */
    @route("/{id}")
    @put op updateFund(
        /** fund ID. Might be ID or fund UUID */
        @path id: string;
        @body body: UpdateFund;
    ): FundDetail | {
          @Http.statusCode statusCode: 400;
    } | {
          @Http.statusCode statusCode: 404;
    };

    /**
     * Import additional data to the existing fund
     */
    @route("/{id}/import")
    @put op importFundData(
        /** Fund ID. Might be ID or fund UUID */
        @path id: string;
        /** Doplnění způsobu importu */
        @body body: ImportFundData;
        @header contentType: "multipart/form-data";
    ): void | {
          @Http.statusCode statusCode: 400;
    } | {
          @Http.statusCode statusCode: 406;
    };

    /**
     * Smazání seznamu hodnot strukturovaného datového typu
     */
    @route("/{id}/structuredObject")
    @delete op deleteStructureData(
        /** Identifikátor AS */
        @path id: integer;
        /** Seznam id hodnot strukturovaného datového typu */
        @body data: integer[];
    ): integer[];

    /**
     * Return list of item type id & count
     */
    @route("{fundId}/usedItemtypes/{fundVersionId}") 
    @get op usedItemTypes(
        ...FundIdParameter,
		...FundVersionIdParameter): UsedItemType[];

    /**
     * Return list of filesystem repositories
     */
    @route("{fundId}/fsrepos")
    @get op fsRepos(...FundIdParameter): FsRepo[];

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

@tag("descitems")
@route("/descItems")
namespace ElzaAPI.DescItem {
    /**
     * Create attribute value
     */
    @route("/{fundVersionId}/create")
    @put op createDescItem(
        /** id AP version */
        @path fundVersionId: integer,
        /** new attribute value */
        @body body: NodeItem
    ): ItemDataResult;

    /**
     * Update attribute value
     */
    @route("/{fundVersionId}/{createNewVersion}/update")
    @put op updateDescItem(
        /** id AP version */
        @path fundVersionId: integer,
        /** create a new version or not? */ 
        @path createNewVersion: boolean,
        /** new attribute value */
        @body body: NodeItem
    ): ItemDataResult | {
          /** ID does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException;
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
    ): integer | {
          /** requested resource does not exists */
          @Http.statusCode statusCode: 404;
       };

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
    ): integer | {
          /** requested resource does not exists */
          @Http.statusCode statusCode: 404;
       };

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
    ): integer | {
          /** requested resource does not exists */
          @Http.statusCode statusCode: 404;
       };

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
})
namespace ElzaAPI {

}
