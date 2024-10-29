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
  /** Model DaoVireRequest */
model DaoViewRequestVO {
    /** Identifikátor aipu - aip.code */
    daoId: string;
    /** Identifikátor dao - dao.code */
    entityRef: string;
    /** Url pro zobratení komponenty */
    viewUrl: string;
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

model AipIdParameter {
    /** Aip ID */
    @path
    aipId: integer;
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

/** Model ExplorerTreeNode */
model ExplorerTreeNode {
    /** Identifikátor nodu */
    uuid: string;
    /** Identifikátor da dao */
    daoId: int32;
    /** Popisek nodu */
    label: string;
    /** Seznam podsložek */
    childFolders: ExplorerTreeNode[];
    /** Seznam podsouborů */
    childFiles: ExplorerTreeNodeFile[];
    /** Rodičovská složka */
    parentFolder: ExplorerTreeNode;
    /** Rodičovská složka v logické struktuře */
    parentFolderLogical: ExplorerTreeNode;
    /** Seznam připojených nodů */
    linkedNodes: LinkedNodeVO[];
}

/** Model ExplorerTreeNodeFile */
model ExplorerTreeNodeFile {
    daoFileId: integer;
    checksum: string;
    checksumType: string;
    mimeType: string;
    size: int32;
    imageHeight?: integer;
    imageWidth?: integer;
    sourceXDimensionUnit?: string;
    sourceXDimensionValue?: integer;
    sourceYDimensionUnit?: string;
    sourceYDimensionValue?: integer;
    duration?: string;
    description?: string;
    filename: string;
    /** Identifikátor nodu */
    uuid: string;
    /** Identifikátor da dao */
    daoId: int32;
    /** Popisek nodu */
    label: string;
    /** Seznam podsložek */
    childFolders: ExplorerTreeNode[];
    /** Seznam podsouborů */
    childFiles: ExplorerTreeNodeFile[];
    /** Rodičovská složka */
    parentFolder: ExplorerTreeNode;
    /** Rodičovská složka v logické struktuře */
    parentFolderLogical: ExplorerTreeNode;
    /** Seznam připojených nodů */
    linkedNodes: LinkedNodeVO[];
    /* Identifikátor dao_file_folder */
    daoFileFolderId: int32;
    /** Příznak logické struktury */
    isLogical?: boolean;
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

/**
 * Custom uzel stromu
 */
model TreeNodeCustomGen {
    /**
     * Unikátní identifikátor uzlu
     */
    UUID: string;
    /**
     * Název
     */
    name: string;
    /**
     * Hodnota
     */
    value: integer[];
    /**
     * Hloubka
     */
    depth: integer;
    /*
     * Id daLevelView
     */
    daLeveViewId?: integer;
    /**
     * Příznak, zda udel má potomky
     */
    hasChildren: boolean;
    /**
     * Unikátení identifikátor rodičovského uzlu
     */
    parent: string
}

/**
 * Custom strom
 */
model TreeDataCustomGen {
    /**
     * Seznam uzlů stromu
     */
    nodes: TreeNodeCustomGen[];
    /**
     * Seznam uuid již expandovaných uzlů
     */
    expandedIdsExtension: string[];
}

/**
 * Archivní soubor
 */
model ArrFundGen {
    /**
     * Identifikátor
     */
    id: integer;
    /**
     * Název
     */
    name: string;
    /**
     * Kód
     */
    internalCode: string;
    /**
     * Číslo
     */
    fundNumber: integer;
    /**
     * Značka
     */
    mark: string;
    /**
     * Datum vytvoření
     */
    createDate: string;
    /**
     * ID instituce archivního souboru
     */
    institutionId: integer;
    /**
     * Unitdate
     */
    unitdate: string;
}


    /**
     * Stav outputu
     */
enum OutputState {
    /**
     * Rozpracovaný
     */
    OPEN,
    /**
     * Běží hromadná akce
     */
    COMPUTING,
    /**
     * Generování
     */
    GENERATING,
    /**
     * Vygenerovaný
     */
    FINISHED,
    /**
     * Vygenerovaný neaktuální
     */
    OUTDATED
}

/**
 * Progres synchronizace
 */
enum SyncProgress {
    /**
     * Čeká se
     */
    UPLOAD_PENDING,
    /**
     * Začal
     */
    UPLOAD_STARTED
}

/**
 * Stav synchronizace
 */
enum SyncState {
    /**
     * OK
     */
    SYNC_OK,
    /**
     * Nesynchronizováno
     */
    NOT_SYNCED,
    /**
     * Lokální změna
     */
    LOCAL_CHANGE
}

model AipDetailFilteredResult {
    /**
     * Počet výsledků
     */
    count: integer;
    /**
     * Seznam objektů
     */
    rows: AipDetailVO[];
}

/**
 * Stav rejstříku
 */
enum ApState {
    OK,
    ERROR,
    TEMP,
    INIT
}

/**
 * VO pro instituce
 */
model ParInstitutionGen {
    /**
     * Id instituce
     */
    id: integer;
    /**
     * Typ instituce
     */
    institutionType: BaseCode;
    /**
     * Id {@ApAccessPoint}
     */
    accessPointId: integer;
    /**
     * Název instituce
     */
    name: string;
    /**
     * Kód instituce
     */
    code: string
}

/**
 * Souhrný model pro enumy
 */
model BaseCode {
    /**
     * Identifikátor
     */
    id: integer;
    /**
     * Kód
     */
    code: string;
    /**
     * Název
     */
    name: string;
}

/**
 * Výčet stavů aktualizace AIPu
 */
enum QueueItemState {
    /**
     * K aktualizaci
     */
    UPDATE,
    /**
     * Ke stažení
     */
    IMPORT_NEW,
    /**
     * Aktualizováno/Staženo
     */
    IMPORT_OK,
    /**
     * Chyba importu
     */
    IMPORT_ERROR,
    /**
     * K exportu
     */
    EXPORT_NEW,
    /**
     * Exportováno
     */
    EXPORT_OK,
    /**
     * Chyba exportu
     */
    EXPORT_ERROR
}

model LinkedNodeVO {
    /**
     * Identifikátor linku
     */
    id: integer;
    /**
     * Identifikátor node
     */
    nodeId: integer;
    /**
     * Jméno node
     */
    name: string;
}

/**
 * Vo pro detail AIPu
 */
model AipDetailVO {
    /**
     * Id AIPu
     */
    aipId: integer;
    /**
     * Kód AIPu
     */
    code: string;
    /**
     * Id digitálního repozitáře
     */
    digitalRepositoryId: integer;
    /**
     * Verze AIPu
     */
    aipVersion: string;
    /**
     * Archivní soubor
     */
    fund: ArrFundGen;
    /**
     * Kód archivního souboru
     */
    fundCode: string;
    /**
     * Instituce
     */
    institution: ParInstitutionGen;
    /**
     * Instituce původce
     */
    originatorInstitution: ParInstitutionGen;
    /**
     * Původce
     */
    originator: string;
    /**
     * Datace od
     */
    unitdateFrom: string;
    /**
     * Datace do
     */
    unitdateTo: string;
    /**
     * Kód instituce
     */
    institutionCode: string;
    /**
     * Číslo příjemky
     */
    ingestionCode: string;
    /**
     * Číslo jednací příjemky ve spisovce
     */
    referenceNumber: string;
    /**
     * Identifikátor vnější změny NAD v PEvA
     */
    nadChangeCode: string;
    /**
     * Velikost AIPu v bytech
     */
    aipSize: integer;
    /**
     * Příznak načtených metadat
     */
    metadataLoad: boolean;
    /**
     * Příznak načteného kompletního AIPu
     */
    completeAipLoad: boolean;
    /**
     * Příznak chyby při načítání metadat
     */
    metadataError: boolean;
    /**
     * Chyby při načítání metadat
     */
    metadataErrorException: string;
    /**
     * Verze AIPu s načtenými metadaty
     */
    aipVersionMetadata: string;
    /**
     * Stav importu
     */
    importState: QueueItemState;
    /**
     * Stav exportu
     */
    exportState: QueueItemState;
    /**
     * Seznam napojených nodes
     */
    linkedNodes: LinkedNodeVO[];
}

/**
 * Dao type
 */
enum DaDaoType {
    REPRESENTATION,
    LOGICAL,
    FILE,
    METAAMD,
    METADMDINHERENT,
    METADMDCONTEXTUAL
}

/**
 * Filter aipu
 */
model AipFilterGen {
    /**
     * UUID filtru
     */
    id?: string;
    /**
     * Atribut, podle kterého chceme filtrovat
     */
    attr: string;
    /**
     * filtrovaná hodnota
     */
    value?: string;
    /**
     * Způsob porovnání
     */
    criteria: AipFilterCriteriaGen;
    /**
     * Hodnota od
     */
    from?: string;
    /**
     * Hodnota do
     */
    to?: string;
    /**
     * Cesta
     */
    path: string;
}

/**
 * List of dao links
 */
model DaoLinksResult {
    items?: DaoLink[];
}

/**
 * Dao link
 */
model DaoLink {
    /**
     * Dao link ID
     */
    daoLinkId?: integer;
    /**
     * Dao link UUID
     */
    daoLinkUuid: string;
    /**
     * Aip ID
     */
    aipId: integer;
    /**
     * Dao ID
     */
    daoId?: integer;
    /**
     * Dao code
     */
    daoCode?: string;
    /**
     * Dao type
     */
    daoType?: DaDaoType;
    /**
     * Komponent name
     */
    name: string;
    /**
     * Link on children komponents
     */
    children?: DaoLink[];
}

/**
 * Způsob porovnání hodnot filtru
 */
enum AipFilterCriteriaGen {
    /**
     * Hodnota je null
     */
    IS_NULL,
    /**
     * Hodnota není null
     */
    IS_NOT_NULL,
    /**
     * Hodnota obsahuje
     */
    CONTAINS,
    /**
     * Hodnota neobsahuje
     */
    DOES_NOT_CONTAIN,
    /**
     * Hodnota se rovná
     */
    EQUALS,
    /**
     * Hodnota je v rozmezí
     */
    BETWEEN
}

/**
 * Aip package type
 */
enum AipType {
    PACKAGE_INFO,
    ARCHDESC,
    METADATA_BASE,
    AIP_BASE,
    AIP_RAW;
}

/**
 * Link type
 */
enum LinkType {
    AIP,
    PART_AIP,
    COMPONENT_AIP;
}

/**
 * AIP update type
 */
enum AipUpdateType {
    DB_UPDATE,
    DOWNLOAD_UPDATE,
    FORCE_UPDATE;
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

@tag("aips")
@route("/aip")
namespace ElzaAPI.Aip {
    /**
     * Import AIP package
     */
    @route("/import-package")
    @post op importPackage(
        /** Digital repository ID */
        @query digitalRepositoryId: integer;
        /** Aip package type */
        @query aipType: AipType;
        /** Import request parameters */
        @header contentType: "application/zip";
        @body body: bytes;
	): {
         /** Package successfully imported */
         @Http.statusCode statusCode: 200;
       } | {
         /** Failed to import package */
         @Http.statusCode statusCode: 500;
         @body data: BaseException;
       };

    /**
     * Create dao structure
     */
    @route("/create-dao-structure")
    @post op createDaoStructure(
        /** Aip IDs */
        @body aipIds: integer[];
    ): {
        /** Dao structure successfully created */
        @Http.statusCode statusCode: 200;
       } | {
        /** Failed to create dao structure */
        @Http.statusCode statusCode: 500;
        @body data: BaseException;
       };

    /**
     * Delete dao structure
     */
    @route("/delete-dao-structure")
    @post op deleteDaoStructure(
        /** Aip IDs */
        @body aipIds: integer[];
    ): {
        /** Dao structure successfully deleted */
        @Http.statusCode statusCode: 200;
       } | {
        /** Failed to delete dao structure */
        @Http.statusCode statusCode: 500;
        @body data: BaseException;
       };

    /**
     * Download complete AIP
     */
    @route("/download-complete-aip")
    @post op downloadCompleteAip(
        /** Aip IDs */
        @body aipIds: integer[];
    ): {
        /** Complete AIPs successfully downloaded */
        @Http.statusCode statusCode: 200;
       } | {
        /** Failed to download complete AIPs */
        @Http.statusCode statusCode: 500;
        @body data: BaseException;
       };

    /**
     * Delete complete AIP
     */
    @route("/delete-complete-aip")
    @post op deleteCompleteAip(
        /** Aip IDs */
        @body aipIds: integer[];
    ): {
        /** Complete AIPs successfully deleted */
        @Http.statusCode statusCode: 200;
       } | {
        /** Failed to delete complete AIPs */
        @Http.statusCode statusCode: 500;
        @body data: BaseException;
       };

    /**
     * Update AIP
     */
    @route("/update-aip")
    @post op updateAip(
        /** Aip IDs */
        @body aipIds: integer[];

        /** Type of update */
        @query type: AipUpdateType;
    ): {
        /** AIPs successfully updated */
        @Http.statusCode statusCode: 200;
       } | {
        /** Failed to update AIPs */
        @Http.statusCode statusCode: 500;
        @body data: BaseException;
       };

    /**
     * Export AIP
     */
    @route("/export-aip")
    @post op exportAip(
        /** Aip IDs */
        @body aipIds: integer[];
    ): {
        /** AIPs successfully exported */
        @Http.statusCode statusCode: 200;
       } | {
        /** Failed to export AIPs */
        @Http.statusCode statusCode: 500;
        @body data: BaseException;
       };

    /**
     * Getting the component file by file id
     */
    @route("/download-component/{fileId}")
    @get op downloadComponent(
        /** File ID */
        @path fileId: integer
    ): {
        @header contentType: "*/*";
        @Http.statusCode statusCode: 200;
        @body data: bytes;
       } | {
        /** Processing */
        @Http.statusCode statusCode: 102;
       } | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
       } | {
        /** Failed to download requested component */
        @Http.statusCode statusCode: 500;
        @body data: BaseException;
       };

    /**
     * Create dao link
     */
     @route("/create-dao-link")
     @post op createDaoLink(
        /** AIP ID */
        @query aipId: integer;
        /** DAO ID */
        @query daoId: integer;
        /** Node ID */
        @query nodeId: integer;
        /** Link type */
        @query linkType: LinkType;
     ) : {
         /** Dao link successfully created */
         @Http.statusCode statusCode: 200;
        } | {
         /** Failed to create dao link */
         @Http.statusCode statusCode: 500;
         @body data: BaseException;
        };

     /**
      * Delete dao link
      */
      @route("/delete-dao-link")
      @post op deleteDaoLink(
         /** Dao link ID */
         @query daoLinkId: integer;
      ) : {
          /** Dao link successfully deleted */
          @Http.statusCode statusCode: 200;
         } | {
          /** Failed to delete dao link */
          @Http.statusCode statusCode: 500;
          @body data: BaseException;
         };

     /**
      * Connect to JP
      */
      @route("/connect-to-jp")
      @post op connectToJp(
         /** ArrNode ID */
         @query arrNodeId: integer;
         /** DaAip ID */
         @query daAipId: integer;
      ) : {
          /** Dao link successfully created */
          @Http.statusCode statusCode: 200;
        } | {
          /** Failed to create dao link */
          @Http.statusCode statusCode: 500;
          @body data: BaseException;
        };

     /**
      * Connect Part to JP
      */
      @route("/connect-part-to-jp")
      @post op connectPartToJp(
         /** ArrNode ID */
         @query arrNodeId: integer;
         /** DaAip ID */
         @query daAipId: integer;
         /** DaDao ID List */
        @query({format: "multi"}) daDaoIdList: integer[];
      ) : {
          /** Dao link successfully created */
          @Http.statusCode statusCode: 200;
        } | {
          /** Failed to create dao link */
          @Http.statusCode statusCode: 500;
          @body data: BaseException;
        };

     /**
      * Create JP from selected
      */
      @route("/connect-jp-from-selected")
      @post op connectJpFromSelected(
         /** ArrNode ID */
         @query arrNodeId: integer;
         /** DaAip ID */
         @query daAipId: integer;
         /** DaDao ID List */
        @query({format: "multi"}) daDaoIdList: integer[];
      ) : {
          /** Dao link successfully created */
          @Http.statusCode statusCode: 200;
        } | {
          /** Failed to create dao link */
          @Http.statusCode statusCode: 500;
          @body data: BaseException;
        };

     /**
      * Create JP and link from selected
      */
      @route("/connect-jp-link-from-selected")
      @post op connectJpLinkFromSelected(
         /** ArrNode ID */
         @query arrNodeId: integer;
         /** DaAip ID */
         @query daAipId: integer;
         /** DaDao ID List */
        @query({format: "multi"}) daDaoIdList: integer[];
      ) : {
          /** Dao link successfully created */
          @Http.statusCode statusCode: 200;
        } | {
          /** Failed to create dao link */
          @Http.statusCode statusCode: 500;
          @body data: BaseException;
        };

     /**
      * Connect selected to JP
      */
      @route("/connect-selected-to-jp")
      @post op connectSelectedToJp(
         /** ArrNode ID */
         @query arrNodeId: integer;
         /** DaAip ID */
         @query daAipId: integer;
         /** DaDao ID List */
        @query({format: "multi"}) daDaoIdList: integer[];
      ) : {
          /** Dao link successfully created */
          @Http.statusCode statusCode: 200;
        } | {
          /** Failed to create dao link */
          @Http.statusCode statusCode: 500;
          @body data: BaseException;
        };

     /**
      * Bulk connect to JP
      */
      @route("/bulk-connect-to-jp")
      @post op bulkConnectToJp(
         /** ArrNode ID */
         @query arrNodeId: integer;
         /** DaAip ID list */
         @query({format: "multi"}) daAipIdList: integer[];
      ) : {
          /** Dao link successfully created */
          @Http.statusCode statusCode: 200;
        } | {
          /** Failed to create dao link */
          @Http.statusCode statusCode: 500;
          @body data: BaseException;
        };

     /**
      * Bulk create from selected
      */
      @route("/bulk-create-from-selected")
      @post op bulkCreateFromSelected(
         /** ArrNode ID */
         @query arrNodeId: integer;
         /** DaAip ID list */
         @query({format: "multi"}) daAipIdList: integer[];
      ) : {
          /** Dao link successfully created */
          @Http.statusCode statusCode: 200;
        } | {
          /** Failed to create dao link */
          @Http.statusCode statusCode: 500;
          @body data: BaseException;
        };

     /**
      * Bulk create from selected and connect to JP
      */
      @route("/bulk-create-selected-to-jp")
      @post op bulkCreateSelectedToJp(
         /** ArrNode ID */
         @query arrNodeId: integer;
         /** DaAip ID list */
         @query({format: "multi"}) daAipIdList: integer[];
         /** DaLevelView ID */
         @query daLevelViewId: integer;
      ) : {
          /** Dao link successfully created */
          @Http.statusCode statusCode: 200;
        } | {
          /** Failed to create dao link */
          @Http.statusCode statusCode: 500;
          @body data: BaseException;
        };

     /**
      * Bulk connect logical structure to JP
      */
      @route("/bulk-connect-logic-to-jp")
      @post op bulkConnectLogicToJp(
         /** ArrNode ID */
         @query arrNodeId: integer;
         /** DaAip ID list */
         @query({format: "multi"}) daAipIdList: integer[];
         /** DaLevelView ID */
         @query daLevelViewId: integer;
      ) : {
          /** Dao link successfully created */
          @Http.statusCode statusCode: 200;
        } | {
          /** Failed to create dao link */
          @Http.statusCode statusCode: 500;
          @body data: BaseException;
        };

     /**
      * Get level view tree for aips with ids
      */
     @route("/levelViewTree")
     @post op levelViewTree(
        /** Aip IDs */
        @body aipIds: integer[];
     ) : {
         /** Dao link successfully deleted */
         @Http.statusCode statusCode: 200;
         @body data: TreeDataCustomGen;
        } | {
         /** Failed to get tree */
         @Http.statusCode statusCode: 500;
         @body data: BaseException;
        };

     /**
      * Get aip detail by id
      */
     @route("/{aipId}")
     @get op getAip (
        /** Aip ID */
        ...AipIdParameter
     ) : {
         /** Aip detail retrieved*/
         @Http.statusCode statusCode: 200;
         @body data: AipDetailVO;
        } | {
         /** Failed to get tree */
         @Http.statusCode statusCode: 500;
         @body data: BaseException;
        };

     /**
      * Get dao links for node by id
      */
     @route("/dao-links/{nodeId}")
     @get op getDaoLinks (
        /** Node ID */
        @path nodeId: integer
     ) : {
         /** Aip detail retrieved*/
         @Http.statusCode statusCode: 200;
         @body data: DaoLinksResult;
        } | {
         /** Failed to get tree */
         @Http.statusCode statusCode: 500;
         @body data: BaseException;
        };

     /**
      * Find aips by filters
      */
     @route("/find/filter")
     @post op findByFilter(
        /** Array of filters */
        @body filters: AipFilterGen[];
        @query from: integer;
        @query count: integer;
     ) : {
         /** Dao link successfully deleted */
         @Http.statusCode statusCode: 200;
         @body data: AipDetailFilteredResult;
        } | {
         /** Failed to get tree */
         @Http.statusCode statusCode: 500;
         @body data: BaseException;
        };

        /**
      * Get level view tree for aips with ids
      */
     @route("/tree")
     @post op tree(
        /** Aip IDs */
        @query aipId: integer;
     ) : {
         /** Dao link successfully deleted */
         @Http.statusCode statusCode: 200;
         @body data: TreeDataCustomGen;
        } | {
         /** Failed to get tree */
         @Http.statusCode statusCode: 500;
         @body data: BaseException;
        };

}
@tag("daos")
@route("/daos")
namespace ElzaAPI.Dao {
    /** Získání informací o da dao pro view request na digital repository*/
    @route("/component/{id}")
    @get op getDaoViewRequestInfo(
        /**  Identifikátor da dao */
        @path id: integer
    ) : {
         /** Dao view request data*/
         @Http.statusCode statusCode: 200;
         @body data: DaoViewRequestVO;
        } | {
         /** Failed to get request data */
         @Http.statusCode statusCode: 500;
         @body data: BaseException;
        };

    /** Získání stromu pro průzkumník aipu */
    @route("/aip/{aipId}")
    @get op findByAipIdAndTypeAndDeleteChangeIsNull(
        /** Identifikátor aipu */
        @path aipId: integer;
    ): {
        /** Dao view request data*/
        @Http.statusCode statusCode: 200;
        @body data: ExplorerTreeNode;
        } | {
        /** Failed to get request data */
        @Http.statusCode statusCode: 500;
        @body data: BaseException;
    };
}
@service({
    title: "ELZA API",
})
namespace ElzaAPI {

}
