import "@typespec/rest";
import "@typespec/openapi";
import "@typespec/openapi3";

using TypeSpec.Http;
using TypeSpec.Rest;
using TypeSpec.OpenAPI;

/**
 * Basic statistics about Elza instance
 */
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

/**
 * Logged user
 */
model LoggedUser {
    /** User ID */
    userId: integer;
    /** username */
    user: string;
    /** Remote address */
    remoteAddr: string;
}

/**
 * List of logged users
 */
model LoggedUsers {
    /** User list */
    users: LoggedUser[];
    /** Total number of logged users */
    totalCount: int32;
}

/**
 * Archival fonds described by basic information
 */
model Fund {
    /** Fonds identifier */
    id: integer;
    /** Fonds UUID */
    uuid: string;
    /** Fonds name */
    name: string;
    /** Archival institution identifier */
    institutionIdentifier: string;
    /** Internal organization fonds code */
    internalCode?: string;
    /** Creation fonds date */
    createDate?: offsetDateTime;
    /** Archival fonds number */
    fundNumber?: integer;
    /** Date range for the fonds */
    unitdate?: string;
    /** Fonds mark */
    mark?: string;
}

/**
 * Archivní soubor pro výsledky vyhledávání
 */
model FundSearchResult extends Fund {
    /** počet JP nalezených v AS */
    count: integer;
    /** identifikátor otevřené verze AS */
    fundVersionId: integer;
}

/**
 * Seznam archivních souborů jako výsledek vyhledávání
 */
model NodeSearchResult {
    /** seznam archivních souborů */
    fonds: FundSearchResult[];
    /** celkový počet */
    totalCount: int64;
    /** znak částečného výsledku */
    partialResult: boolean;
}

/**
 * Details about archival funds
 */
model FundDetail extends Fund {
    /** Seznam oblastí entit */
    scopes: string[];
    /** Active funds version */
    fundVersionId: integer;
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
    importType: HttpPart<string>;
    /** File to upload */
    dataFile: HttpPart<bytes>;
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
    /** Zákaz změny vlastností */
    managed?: boolean;
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
    /** Fund ID */
    @path
    fundId: integer;
}

model AipIdParameter {
    /** Aip ID */
    @path
    aipId: integer;
}

model FundVersionIdParameter {
    /** Fund ID Version */
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

/**
 * Single file system item
 */
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
    /** kód nebo ID filtru */
    exportFilter?: string;
    includeUUID?: boolean = true;
    includeAccessPoints?: boolean = true;
    /** zahrnout do exportu připojené digitální archivní objekty (DAO) */
    includeDaos?: boolean = true;
}

/**
 * Status of async request
 */
enum RequestProcessState {
    PENDING,
    PROCESSING,
    FINISHED,
    ERROR
}

/**
 * Status of request
 */
model ExportRequestStatus {
    state: RequestProcessState;
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
 * Data types for ItemData classes
 */
enum DataType {
    INT,
    STRING,
    TEXT,
    UNITDATE,
    UNITID,
    FORMATTED_TEXT,
    COORDINATES,
    RECORD_REF,
    DECIMAL,
    STRUCTURED,
    ENUM,
    FILE_REF,
    JSON_TABLE,
    DATE,
    URI_REF,
    BIT
}

/**
 * Item data parent
 */
@discriminator("dataType")
model ItemData {
    /** data id */
    dataId?: integer;
    /** data type */
    dataType: DataType;
}

/**
 * ItemData classes extensions
 */
model DataBit extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.BIT;
    bitValue: boolean;
}

model DataCoordinates extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.COORDINATES;
    value: string;
}

model DataDate extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.DATE;
    value: plainDate;
}

model DataDecimal extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.DECIMAL;
    value: decimal;
}

model DataFileRef extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.FILE_REF;
    fileId: integer;
}

model DataInteger extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.INT;
    integerValue: integer;
}

model DataJsonTable extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.JSON_TABLE;
    value: string;
}

model DataEnum extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.ENUM;
}

model DataRecordRef extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.RECORD_REF;
    value: integer;
}

model DataString extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.STRING;
    stringValue: string;
}

model DataStructureRef extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.STRUCTURED;
    structuredObjectId: integer;
}

model DataText extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.TEXT;
    textValue: string;
}

model DataFormattedText extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.FORMATTED_TEXT;
    value: string;
}

model DataUnitdate extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.UNITDATE;
    value: string;
}

model DataUnitid extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.UNITID;
    unitId: string;
}

model DataUriRef extends ItemData {
    @invisible(Lifecycle)
    dataType: DataType.URI_REF;
    nodeId?: integer;
    value: string;
    description?: string;
    refTemplateId?: integer;
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
    parent?: NodeBase;
    /** item */
    item?: NodeItem;
}

/**
 * Node information without item values, used for parent and sibling nodes in NodeData
 */
model NodeBase {
    /** id of node */
    id: integer;
    /** node version */
    version: integer;
    /** node uuid */
    uuid: string;
}

/**
 * Basic information about a node — its identifiers, fund placement,
 * and deletion state relative to the most recent version that contained it.
 *
 * Lightweight alternative to the full node-data load for clients that
 * only need to resolve a node identifier to fund/version context.
 */
model NodeInfo {
    /** Node ID */
    id: integer;
    /** Node DB row version (for optimistic concurrency on subsequent writes) */
    version: integer;
    /** Node UUID */
    uuid: string;
    /** Fund ID the node belongs to */
    fundId: integer;
    /**
     * ID of the most recent fund version in which the node was available
     * (i.e. had an active level). Not necessarily the open version —
     * if the node has been deleted in the open version, this points to
     * the latest version that still contained it.
     */
    fundVersionId: integer;
    /**
     * ID of the ArrChange that deleted the node's level after `fundVersionId`
     * was locked. Absent if the node's level is still active in `fundVersionId`
     * (i.e. the node is still reachable in that version).
     */
    deleteChangeId?: integer;
}

/**
 * Type of update Item
 */
enum UpdateOp {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Náhrada za třídu `ArrUpdateItemVO`
 */
model NodeUpdateItem {
    /** operation */
    updateOp: UpdateOp;
    /** item to update */
    item: NodeItem;
}

model BaseItem {
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
    /** item data */
    data?: ItemData;
}

/**
 * Náhrada za `ArrItemVO`
 */
model NodeItem extends BaseItem {
    /** id node */
    nodeId?: integer;
    /** node version */
    nodeVersion?: integer;
    /** if inheritance is prohibited - true */
    inhibited?: boolean;
}

model StructuredObjectItem extends BaseItem {
}

model OutputItem extends BaseItem {
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
     * Id ApAccessPoint
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
 * Náhrada za `ApScopeVO`
 */
model Scope extends BaseCode {
    language: string;
    ruleSetCode: string;
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
     * Cesta k dao
     */
    path?: string;
    /**
     * Number of children components
     */
    childrenCount?: integer;
    /**
     * Link on children components
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

/**
 * Access point list
 */
model DeleteAccessPointsDetail {
    /** list of id or uuid */
    ids: string[];
}

/**
 * Deleted archive entity
 */
model DeletedEntity {
    /** id of access point */
    accessPointId: integer;
    /** name of access point */
    name: string;
    /** pt_body + brief_desc */
    description?: string;
    /** id type of acces point */
    apTypeId: integer;
    /** id in external system */
    bindingValue?: string;
    /** id external system */
    extSystemId?: integer;
    /** id of deleteChange */
    deleteChangeId: integer;
    /** date of deletion */
    deleteDate: offsetDateTime;
    /** id replaced access point */
    replacedBy?: integer;
}

/**
 * Deleted archive entities
 */
model InvalidatedEntities {
   totalCount: integer;
   page: DeletedEntity[];
}

/**
 * Data for create/update a part (external)
 */
model ApPartFormVO {
    /** this is an external class */
}

/**
 * Skupina zapouzdrující typy hodnot atributů pro UI
 *
 * Náhrada `ItemTypeGroupVO`
 */
model ItemTypeGroup {
    code: string;
    name: string;
    types?: FormItemType[];
}

/**
 * Item mandatory type
 */
enum MandatoryType {
    REQUIRED, RECOMMENDED, POSSIBLE, IMPOSSIBLE
}

/**
 * Náhrada `DescItemSpecLiteVO`
 */
model FormItemSpec {
    itemSpecId: integer;
    type: MandatoryType;
    repeatable: boolean;
}

/**
 * Odlehčená verze specifikace hodnoty atributu
 *
 * Náhrada `ItemTypeLiteVO`
 */
model FormItemType {
    /** identifikator typu */
    itemTypeId: integer;
    /** typ */
    type: MandatoryType;
    /** opakovatelnost */
    repeatable: boolean;
    /** počítaný */
    //?cal: integer;
    /** stav počítanýho atributu: 0/1 vypnutý/zapnutý */
    //?calSt: integer;
    /** atribut se může nastavit jako nedefinovaný 0/1 - nemůže/může */
    undefinable: boolean;
    /** seznam specifikací atributu */
    specs?: FormItemSpec[];
    /** seznam identifikátorů oblíbených specifikací u typu */
    favoriteSpecIds: integer[];
    /** šířka atributu (0 - maximální počet sloupců, 1..N - počet sloupců) */
    //?width: integer;
}

/**
 * Display hint for items of `dataType == INT`.
 */
enum ItemDisplayType {
    NUMBER,
    DURATION,
}

/**
 * Column definition for items of `dataType == JSON_TABLE`.
 */
model ItemTableColumn {
    /** Column code. */
    code: string;
    /** Human-readable column name (localized). */
    name: string;
    /** Column data type — limited subset; see ELZA package XSD. */
    dataType: string;
    /** Suggested column width. */
    width?: integer;
}

/**
 * Optional rendering hints for an `ItemType`.
 *
 * Discriminated by `dataType`. Item types whose data type has no rendering
 * hint (DATE, COORDINATES, RECORD_REF, …) simply omit `viewDefinition` on
 * `ItemType`.
 */
@discriminator("dataType")
model ItemViewDefinition {
    dataType: DataType;
}

/**
 * Display hint for INT items: numeric vs. duration formatting.
 */
model IntViewDefinition extends ItemViewDefinition {
    @invisible(Lifecycle)
    dataType: DataType.INT;
    displayType: ItemDisplayType;
}

/**
 * Input mask for STRING items (e.g. "###-##"). `#` denotes a value
 * character, `*` denotes a variable-length run, other characters are
 * literals automatically inserted by the UI.
 */
model StringViewDefinition extends ItemViewDefinition {
    @invisible(Lifecycle)
    dataType: DataType.STRING;
    mask: string;
}

/**
 * Column definitions for JSON_TABLE items.
 */
model JsonTableViewDefinition extends ItemViewDefinition {
    @invisible(Lifecycle)
    dataType: DataType.JSON_TABLE;
    tableColumns: ItemTableColumn[];
}

/**
 * A specification (enumerated value) of an item type.
 * Display order is given by the order of the parent `specs[]` array.
 */
model ItemTypeSpec {
    /** Database identifier of the underlying `rul_item_spec` row. Not stable across deployments — use `code` for cross-system references. */
    id: integer;
    /** Stable public code. */
    code: string;
    /** Human-readable name (localized). */
    name: string;
    /** Short label for compact UI (localized). */
    shortcut: string;
    /** Longer description (localized). */
    description?: string;
}

/**
 * Definition of one item type registered in the rules.
 *
 * Replaces the legacy `RulDescItemTypeExtVO` exposed via
 * `GET /api/rule/descItemTypes`. Display order is given by the order of
 * the response `itemTypes[]` array.
 *
 * Per-form behaviour (whether the item is required/recommended, whether it
 * is repeatable in a given context, etc.) is **not** part of this registry
 * — that information lives in `FormItemType` / `FormItemSpec`, returned by
 * the node-form endpoints.
 */
model ItemType {
    /** Database identifier of the underlying `rul_item_type` row. Not stable across deployments — use `code` for cross-system references. */
    id: integer;
    /** Stable public code (e.g. "ZP2015_UNIT_ID"). */
    code: string;
    /** Human-readable name (localized). */
    name: string;
    /** Short label for compact UI such as column headers (localized). */
    shortcut: string;
    /** Longer description (localized). */
    description?: string;
    /** Underlying value type. */
    dataType: DataType;
    /** Whether items of this type can be used as a sort key. */
    sortable: boolean;
    /**
     * For `dataType == STRUCTURED` only: code of the structure type whose
     * template defines this item's nested fields.
     */
    structureTypeCode?: string;
    /** Optional rendering hints; variant determined by `dataType`. */
    viewDefinition?: ItemViewDefinition;
    /** Available specifications, in display order. Empty/omitted ⇒ no specifications. */
    specs?: ItemTypeSpec[];
}

/**
 * Response payload for `GET /itemTypes`.
 */
model ItemTypeList {
    /** Item types defined across all loaded rule sets, in display order. */
    itemTypes: ItemType[];
}

/*
 * Náhrada 'RulPartTypeVO'
 */
model PartType extends BaseCode {
    childPartId: integer;
    repeatable: boolean;
}

/**
 * Náhrada `DescFormDataNewVO`
 */
model NodeFormData {
    /** rodič */
    parent: NodeBase;
    /** seznam hodnot atributu */
    descItems: NodeItem[];
    /** typy atributů */
    itemTypes: FormItemType[];
    /** oprávnění pořádat v dané JP */
    arrPerm: boolean;
}

/**
 * Data uzlu stromu odesílané klientovi pro strom fa.
 *
 * Náhrada `TreeNodeVO`
 */
model NodeTreeData {
    /** node id */
    id: integer;
    /** hloubka zanoření ve stromu */
    depth: integer;
    /** název uzlu */
    name: string;
    /** ikonka */
    icon?: string;
    /** true - uzel má další potomky, false - uzel nemá další potomky */
    hasChildren: boolean;
    /** referenční označení. Od kořene k uzlu */
    referenceMark: string[];
    /** verze uzlu */
    version: integer;
    /** oprávnění pořádat v dané JP */
    arrPerm?: boolean;
}

/**
 * Stav JP
 */
enum NodeConformityState {
    OK, ERR
}

/**
 * Náhrada `NodeConformityMissingVO`
 */
model NodeConformityMissing {
    /** identifikátor typu */
    descItemTypeId: integer;
    /** identifikátor specifikace */
    descItemSpecId: integer;
    /** popis chybějící položky */
    description: string;
    /** identifikátor typu oprávnění */
    policyTypeId: integer;
}

/**
 * Náhrada `NodeConformityErrorVO`
 */
model NodeConformityError {
    /** identifikátor hodnoty atributu */
    descItemObjectId: integer;
    /** popis chyby */
    description: string;
    /** identifikátor typu oprávnění */
    policyTypeId: integer;
}

/**
 * Pro validace JP
 *
 * Náhrada `NodeConformityVO`
 */
model NodeConformity {
    state: NodeConformityState;
    /** datum validace */
    date: plainDate;
    /** seznam chybějících hodnot */
    missingList: NodeConformityMissing[];
    /** seznam chybných hodnot */
    errorList: NodeConformityError[];
    /** seznam uplatnenych politik pro zobrazeni */
    viewPolicyTypeIds: integer[];
}

/**
 * Request state
 */
enum RequestState {
    OPEN, QUEUED, SENT, PROCESSED, REJECTED
}

/**
 * Náhrada `ArrRequestVO`
 */
model Request {
    id: integer;
    code: string;
    state: RequestState;
    rejectReason: string;
    username: string;
    create: plainDate;
    send: plainDate;
    responseExternalSystem: plainDate;
    externalSystemCode: string;
}

/**
 * Náhrada `ArrDigitizationRequestVO`
 */
model DigitizationRequest extends Request {
    description: string;
    digitizationFrontdeskId: integer;
    nodesCount: integer;
    nodes: NodeTreeData[];
}

/**
 * Náhrada `WfSimpleIssueVO`
 */
model WfSimpleIssue {
    /** indentifikátor připomínky */
    id: integer;
    /** číslo připomínky v rámci protokolu */
    number: integer;
    /** text připomínky */
    description: string;
}

/**
 * Brief view of a description unit — id, version, accordion-row presentation fields, and per-node status indicators.
 * Used wherever a list of nodes is presented (e.g. siblings).
 *
 * Náhrada `AccordionNodeVO`
 */
model NodeBrief {
    /** nodeid uzlu */
    id: integer;
    /** popis levé části accordionu */
    accordionLeft: string;
    /** popis pravé části accordionu */
    accordionRight: string;
    /** označení JP */
    referenceMark: string[];
    /** Informace o stavu JP */
    nodeConformity?: NodeConformity;
    /** seznam otevřených požadavků na digitalizaci */
    digitizationRequests?: DigitizationRequest[];
    /** db verze JP */
    version: integer;
    /** seznam otevřených připomínek */
    issues?: WfSimpleIssue[];
}

/**
 * Status information about a single description unit (the node referenced by request.nodeId).
 * Returned in response.node when the request flag `nodeStatus` is true.
 *
 * Designed to be additive-only: new optional fields may be appended without breaking clients.
 */
model NodeStatus {
    /** Node identifier — echoes request.nodeId for self-describing responses. */
    id: integer;
    /** DB version of the node, for optimistic concurrency on subsequent writes. */
    version: integer;
    /** Validation conformity (errors, missing items, visible policy types). Absent if no conformity record exists for this node. */
    nodeConformity?: NodeConformity;
}

/**
 * Received data of the unit of description
 *
 * Náhrada `NodeDataVO`
 */
model NodeData {
    /** data formuláře požadované JP */
    formData: NodeFormData;
    /** Status of the focus node. Returned only when request.nodeStatus is true. */
    node?: NodeStatus;
    /** Parents chain up to the root. Returned only when request.parents is true. */
    parents?: NodeTreeData[];
    /** Direct children. Returned only when request.children is true. */
    children?: NodeTreeData[];
    /** Siblings window. Returned only when request.siblingsFrom is set. */
    siblings?: NodeBrief[];
    /** index požadované JP */
    nodeIndex: integer;
    /** celkový počet JP v úrovni požadované JP */
    nodeCount: integer;
}

/**
 * Parameters for get information about the description unit and its "surroundings"
 */
model NodeDataParam {
    /** version fund */
    fundVersionId: integer;
    /** id of node */
    nodeId: integer;
    /** load description unit form data as well? */
    formData: boolean;
    /** load (if not null) siblings of the description unit? */
    siblingsFrom?: integer;
    /** max number of siblings returned */
    siblingsMaxCount: integer;
    /** full-text filtering in siblings */
    siblingsFilter?: string;
    /** Load parents description units to root? */
    parents: boolean;
    /** Load children description units? */
    children: boolean;
    /** Load focus-node status (response.node)? Default false. */
    nodeStatus?: boolean;
}

/**
 * Result of creating a part
 */
model CreatedPart {
    partId: integer;
    apVersion: integer;
}

/**
 * Part validation issues/errors
 */
model PartValidationIssues {
    id: integer;
    errors: string[];
}

/**
 * Entity validation issues/errors
 */
model ApValidationIssues {
    errors: string[];
    partErrors: PartValidationIssues[];
}

/**
 * Operation type for FieldValueFilter
 */
enum OperationCompareType {
    EQ, NEQ, GT, LT, GTE, LTE, STARTWITH, ENDWITH, CONTAINS, INTERSECT, IS_IN, IS_NULL, NOT_NULL
}

/**
 * Operation type for LogicalFilter
 */
enum OperationLogicalType {
    AND, OR 
}

/**
 * Type of field for FieldValueFilter
 */
enum FieldType {
    /** Field for archival fonds */
    FONDS_FIELD,

    /** Node specific field */
    NODE_FIELD,

    /** Description item field */
    DESC_ITEM
}

/**
 * Used as a parent for filter fields
 */
@discriminator("fieldType")
model AbstractField {
    fieldType: FieldType;
}

/**
 * Funds specific field names
 * 
 * Fields are used for FundsFilterField.
 */
enum FondsFieldName {
    fondsId, name, createDate, internalCode, fondsNumber, unitdate, mark, institutionId, institutionCode
}

/**
 * Fund field names for filter
 */
model FondsField extends AbstractField {
    @invisible(Lifecycle)
    fieldType: FieldType.FONDS_FIELD;
    fieldName: FondsFieldName;
}

/**
 * Node specific field names
 * 
 * Node fields are used for FieldValueFilter.
 */
enum NodeFieldName {
    conformityError, 
    conformityMissing,
    uuid
}

/**
 * Node specifici search field
 */
model NodeField extends AbstractField {
    /**
     * Type of field
     */
    @invisible(Lifecycle)
    fieldType: FieldType.NODE_FIELD;

    /**
     * Name of field (from enum NodeFieldName)
     */
    fieldName: NodeFieldName;
}

/**
 * Node (descItem) field names for filter
 */ 
model DescItemField extends AbstractField {
    @invisible(Lifecycle)
    fieldType: FieldType.DESC_ITEM;
    typeCode: string;
    specCode?: string;
}

/**
 * Types of filters
 */
enum FilterType {
    fieldValue,
    contains,
    logical
}

/**
 * Used as a parent for all other filters
 */
@discriminator("filterType")
model AbstractFilter {
    filterType: FilterType
}

/**
 * Compares specified attribute of the entity with specified value using specified operation
 */
model FieldValueFilter extends AbstractFilter {
    @invisible(Lifecycle)
    filterType: FilterType.fieldValue;
    /** searched field */
    field: AbstractField;
    /** searched value */
    value?: string;
    /** operation type */
    operation: OperationCompareType;
}

/**
 * Performs fulltext search over the whole entity
 */
model MultimatchContainsFilter extends AbstractFilter {
    @invisible(Lifecycle)
    filterType: FilterType.contains;
    /** searched value */
    value: string;
}

/**
 * Join subfilters using logical operator, result may be used in other LogicalFilter as one of subfilter
 */
model LogicalFilter extends AbstractFilter {
    @invisible(Lifecycle)
    filterType: FilterType.logical;
    /** collection of filters */
    filters: AbstractFilter[];
    /** operation type */
    operation: OperationLogicalType;
}

/**
 * Order of sorting
 */
enum SortingOrder {
    asc,
    desc
}

/**
 * Allows to define sorting
 */
model Sorting {
    /** attribute name to sort on */
    field: string;
    /** order of sorting */
    order?: SortingOrder;
}

/**
 * Search parameters
 */
model SearchParams {
    /** array of conditions, joined by AND operator */
    filters?: AbstractFilter[];
    /** page offset */
    offset?: integer;
    /** page size */
    size?: integer;
    /** array of sort conditions */
    sort?: Sorting[];
}

/**
 * Search result 
 */
model ResultEntityRef {
    count: int64;
    items?: EntityRef[];
}

/**
 * Reference to a user account.
 *
 * Carries enough information for the client to display the user
 * (name, username) without an extra lookup, while still keeping the
 * stable identifier.
 */
model UserRef {
    /** User identifier */
    userId: integer;
    /** Full display name */
    name: string;
    /** Username (login) */
    username: string;
}

/**
 * Entity processor (user)
 */
model Participant extends UserRef {
    /** if user has ability to save and modify new entity */
    canWrite: boolean;
    /** if user has ability to approve entity */
    canConfirm: boolean;
    /** if user has ability to change approved entity */
    canEditConfirmed: boolean;
}

// =============================================================================
// CAMv2 — external entity binding (issues, revisions, participants)
// =============================================================================

/** Binding of one part or item of an access point to an external entity. */
model ExtItemBinding {
    partId?: integer;
    itemId?: integer;
    sync: boolean;
}

/**
 * One binding of an access point to an external system. Represents the
 * entity's identity in that system together with sync/state metadata and
 * (since CAMv2) per-revision metadata and issue summary.
 */
model ExtEntityBinding {
    id: integer;
    externalSystemId: integer;
    /**
     * Code of the external system. Marked deprecated on the Java VO, but
     * still load-bearing for several action calls — kept until the callers
     * migrate to externalSystemId.
     */
    externalSystemCode: string;
    /** Identifier of the entity in the external system. */
    value: string;
    extState?: string;
    extRevision?: string;
    extUser?: string;
    extReplacedBy?: string;
    detailUrlExtReplacedBy?: string;
    syncState?: SyncState;
    syncProgress?: SyncProgress;
    bindingItemList?: ExtItemBinding[];

    // CAMv2 additions
    extMetadataRevision?: string;
    extPrevRevision?: string;
    /** Creation time of this revision in the external system. */
    extCreatedAt?: utcDateTime;
    /** Pre-computed badge for the warning icon. Absent = no issues. */
    issueSummary?: ExtIssueSummary;
}

/** Severity of a CAM-side issue attached to an entity binding. */
enum ExtIssueSeverity {
    WARNING,
    ERROR,
}

/**
 * Resolution state of an issue. Absent on the wire = new issue not yet
 * evaluated by the user.
 */
enum ExtIssueStatus {
    IR_ALL_REVISIONS_OK,
    IR_FIX_NEEDED,
    IR_REVISION_OK,
}

/**
 * Aggregated badge state computed by the server from all issues on a binding.
 *
 *   NONE      no issues
 *   NEUTRAL   only WARNING issues with status IR_ALL_REVISIONS_OK / IR_REVISION_OK
 *   NEW       at least one WARNING with status absent
 *   ATTENTION any ERROR, or any status = IR_FIX_NEEDED — single most prominent icon
 */
enum ExtIssueIconState {
    NONE,
    NEUTRAL,
    NEW,
    ATTENTION,
}

/**
 * Pre-computed badge for a binding, embedded in ExtEntityBinding so the client
 * renders the warning icon without a second round-trip.
 */
model ExtIssueSummary {
    /** total number of issues attached to the binding */
    count: integer;
    iconState: ExtIssueIconState;
}

/**
 * One issue/finding reported by CAM about an entity binding. Issues are not
 * versioned in Elza — only the current state synced from CAM is kept. Issue
 * references never cross external systems: relatedBindingId always points to
 * a binding within the same external system as this entity.
 */
model ExtIssue {
    /** binding_issue_id */
    id: integer;
    /** stable identifier from CAM, when provided */
    uuid?: string;
    severity: ExtIssueSeverity;
    status?: ExtIssueStatus;
    ruleCode?: string;
    issueCode?: string;
    message?: string;
    source?: string;
    detail?: string;
    note?: string;
    /** valid-from timestamp from CAM */
    issueFrom?: utcDateTime;
    /** external revision in which the issue first appeared */
    extFromRev?: string;
    /** referenced part of the AP, when applicable */
    partId?: integer;
    /** referenced item of the AP, when applicable */
    itemId?: integer;
    /** related binding within the same external system, when applicable */
    relatedBindingId?: integer;
    /** value of the related binding in the external system */
    relatedBindingExtValue?: string;
}

/** Role of a participant on an entity revision. */
enum ExtParticipantRole {
    AUTHOR,
    APPROVAL,
}

/** One person who participated on a binding revision. */
model ExtParticipant {
    /** binding_participant_id */
    id: integer;
    role: ExtParticipantRole;
    name: string;
    institutionCode?: string;
    lastChange: utcDateTime;
}

/**
 * One revision in the binding's history, assembled from ap_binding_state with
 * embedded participants from ap_binding_participant. Participants are returned
 * ordered by lastChange (ascending).
 */
model ExtRevision {
    /** binding_state_id */
    bindingStateId: integer;
    extRevision?: string;
    extPrevRevision?: string;
    extMetadataRevision?: string;
    /** time of revision creation in the external system */
    extCreatedAt?: utcDateTime;
    /** fallback timestamp from arr_change when extCreatedAt is missing */
    createChangeAt?: utcDateTime;
    /** sender of the revision — value of ApBindingState.extUser */
    sender?: string;
    participants: ExtParticipant[];
}

/** Paginated history of a binding, newest revision first. */
model ExtHistory {
    revisions: ExtRevision[];
    /** total number of revisions known to Elza for this binding */
    totalCount: integer;
    /**
     * True when the chain has gaps (some extPrevRevision points to a revision
     * Elza does not have). The UI then suggests consulting the source system.
     */
    incomplete: boolean;
}

/**
 * Administrativní funkce
 */
@tag("admin")
@route("/admin")
namespace ElzaAPI.Admin {
    /**
     * Creating missing items in arr_cached_node
     */
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

@tag("daos")
@route("/daos")
namespace ElzaAPI.Dao {
    /**
     * Changing the dao connection scenario
     */
    @route("/{id}/change-scenario")
    @post op changeLinkScenario(
        /** dao id */
        @path id: integer,
        /** new scenario */
        @body body: string;
        @header contentType: "application/json";
    ): {
        /** The scenario has been changed */
        @Http.statusCode statusCode: 200;
    } | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };
}

@route("/daorepository")
namespace ElzaAPI.DaoRepository {
    op fsRepos(): FsRepo[];
}

@tag("externalsystems")
@route("/extsystem")
namespace ElzaAPI.ExternalSystem {
    /**
     * External system resync
     */
    @route("/resync/{id}")
    @post op externalSystemResync(
        /** external system ID or CODE */
        @path id: string;
    ): {
        /** Pending, processing, finished */
        @Http.statusCode statusCode: 200;
    } | {
        /** Id does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

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

/**
 * One display block of an AI response — THE RENDERING CONTRACT between the
 * server and the client. The server (per task type) transforms the provider's
 * schema-validated output into an ordered list of blocks; the client renders
 * each block solely by its `type`:
 *
 * - `markdown` — `content` holds CommonMark + GFM tables. The server MUST NOT
 *   emit raw HTML; the client MUST render through a sanitizing Markdown
 *   renderer (raw HTML is never interpreted). Use for free-text answers,
 *   explanations, drafts.
 * - `table` — structured tabular data: `columns` define order, keys and
 *   labels; `rows` hold one object per row keyed by column key, cell values
 *   are plain text (no markup). The client renders a native table widget.
 *   Use whenever the data IS tabular — never serialize tables into markdown
 *   when the server can emit this block instead.
 * - `citations` — the knowledge/documentation sources the answer relied on:
 *   `citations` holds one entry per cited section (title, source document and,
 *   when the provider resolved one, a web URL). The server emits NO label text;
 *   the client renders a "Sources" list and supplies the heading itself, in the
 *   user's language. Use instead of appending sources as markdown.
 *
 * Evolution is additive: new block types may appear (e.g. `findings`). A
 * client MUST tolerate an unknown `type` by rendering a generic "unsupported
 * content" placeholder (never dropping the block silently). Server-side
 * fallback rule: a task output with no block mapper is rendered as one
 * `markdown` block containing the output JSON in a fenced code block.
 */
model AiDisplayBlock {
    /** Block type discriminator: `markdown` | `table` | `citations` (open set). */
    type: string;
    /** `markdown` only: CommonMark + GFM tables source; no raw HTML. */
    content?: string;
    /** `table` only: optional caption above the table. */
    caption?: string;
    /** `table` only: ordered column definitions. */
    columns?: AiTableColumn[];
    /** `table` only: rows as objects keyed by column `key`; values are plain text. */
    rows?: Record<string>[];
    /** `citations` only: the cited sources, in first-referenced order. */
    citations?: AiCitation[];
}

model AiTableColumn {
    /** Key of the column in `rows` objects. */
    key: string;
    /** Column header shown to the user. */
    label: string;
}

/**
 * One cited source in a `citations` display block. Carries only data — the
 * client owns all human-readable chrome (the "Sources" heading, the localized
 * label when a field is absent).
 */
model AiCitation {
    /**
     * Stable citation id of the cited section — the client's list key and the
     * fallback label shown when no `title` is known.
     */
    fragment: string;
    /** Deep link to the cited section in the published source, when resolvable. */
    url?: string;
    /** Human-readable title of the cited section (the primary label). */
    title?: string;
    /** Human-readable name of the source document the section belongs to. */
    source?: string;
    /** Id of the source document — lets the client group citations by source. */
    sourceId?: string;
}

/** Metered usage of one exchange (informative; costUnits is the billable quantity). */
model AiUsage {
    inputTokens: int64;
    outputTokens: int64;
    costUnits: float64;

    /**
     * Credits charged to the account the signing key selects (costUnits ×
     * the account's multiplier — a shared organizational account may burn
     * credits faster). What the user sees as the price of the exchange.
     */
    chargedCredits?: float64;
}

/**
 * One exchange with the AI provider within a conversation. `state` is an open
 * set (`queued`, `running`, `awaiting_tools`, `done`, `error`, `cancelled`);
 * a client MUST treat an unknown state as "in progress". `blocks` is present
 * when `done` (see AiDisplayBlock for the rendering contract).
 */
model AiRequest {
    id: integer;
    taskType: string;
    state: string;
    /** The user's message of this exchange. */
    userInstructions?: string;
    /** Render-ready response (present when state = done). */
    blocks?: AiDisplayBlock[];
    errorCode?: string;
    errorMessage?: string;
    promptVersion?: string;
    /** Profile/model used for this exchange (an `AiProfile.code`); absent = provider default. */
    profile?: string;
    usage?: AiUsage;
    createDate: utcDateTime;
    finishDate?: utcDateTime;
}

/** One AI conversation (topic/thread) as shown in the panel history. */
model AiConversation {
    id: integer;
    /** Code of the AI external system the conversation runs against. */
    externalSystemCode: string;
    title: string;
    /** Context type code: `fund-version`, `fund-list`, `access-point`, … */
    contextType?: string;
    /** Context detail (JSON string); shape depends on contextType. */
    context?: string;
    createDate: utcDateTime;
    lastChangeDate: utcDateTime;
}

/** Full conversation detail: the thread = requests in create order. */
model AiConversationDetail {
    conversation: AiConversation;
    requests: AiRequest[];
}

/** Starts a conversation: creates the thread and submits its first exchange. */
model AiConversationCreate {
    /** Code of the AI external system to use. */
    externalSystemCode: string;
    /** Task type, e.g. `elza.echo`, `elza.revision`. */
    taskType: string;
    /** Profile/model to use (an `AiProfile.code` from GET /info); omitted = provider default. */
    profile?: string;
    /** Thread title; when omitted the server derives it from the instructions. */
    title?: string;
    contextType?: string;
    /** Context detail (JSON string). */
    context?: string;
    /** The user's message. */
    userInstructions?: string;
    /** Structured task parameters (JSON string), shape depends on taskType. */
    parameters?: string;
}

/** Follow-up exchange submitted into an existing conversation. */
model AiRequestCreate {
    /** Profile/model for this exchange (an `AiProfile.code`); omitted = reuse the conversation's last. */
    profile?: string;
    userInstructions?: string;
    /** Structured task parameters (JSON string). */
    parameters?: string;
}

/** One step of an exchange's log — the transparency view ("what was sent/received"). */
model AiRequestEvent {
    id: integer;
    /** SUBMIT | TOOL_CALLS | TOOL_RESULTS | OUTPUT | ERROR | CANCEL (open set). */
    eventType: string;
    /** Event payload (JSON string); shape depends on eventType. */
    data?: string;
    createDate: utcDateTime;
}

/**
 * The current user's personal AI signing key of one provider — presence only,
 * the secret is never returned. With a personal key the user's tasks bill to
 * their personal account (seat); without one the instance-wide key (the shared
 * organizational account) is used.
 */
model AiMyKey {
    /** KeyId of the personal key; absent = none set (the instance-wide key applies). */
    keyId?: string;
}

/** Sets/replaces the current user's personal AI signing key. */
model AiMyKeyUpdate {
    /** KeyId issued by the provider's account authority (an opaque string). */
    keyId: string;
    /** The HMAC secret belonging to the KeyId (stored server-side, never returned). */
    secret: string;
}

/**
 * The AI provider's capabilities — the client's typed view of the provider's
 * `/info` catalog. Drives the panel: which tasks to offer and, per task, which
 * typed parameters to gather. Grows additively (new task types, new object
 * types); a client tolerates unknown parameter/result `type` codes.
 */
model AiProviderInfo {
    /** Protocol version reported by the provider (`major.minor`). */
    protocolVersion: string;
    /** Human-readable provider/service name. */
    providerName?: string;
    /** Task types the provider currently offers. */
    taskTypes: AiTaskType[];
    /** Quality/cost tiers the provider offers. */
    profiles: AiProfile[];
}

/** One AI task the provider offers, with the typed parameters it declares. */
model AiTaskType {
    /** Task type code, e.g. `elza.chat`, `elza.echo`, `elza.revision`. */
    code: string;
    name?: string;
    description?: string;
    /** Typed parameters the task declares; the client fills these from context. */
    parameters: AiTaskParameter[];
    /** Object type codes the task's result may contain (client rendering readiness). */
    resultTypes: string[];
}

/** One typed parameter a task declares. */
model AiTaskParameter {
    /** Parameter name — the key under which the client supplies its value. */
    name: string;
    /** Object type code of the value, e.g. `elza.text`, `elza.archivalDescription`. */
    type: string;
    description?: string;
    /** Whether the caller must supply it. */
    required?: boolean;
}

/** A quality/cost tier the provider offers. */
model AiProfile {
    code: string;
    name?: string;
    description?: string;
    /** True for the tier used when a submission names none. */
    default?: boolean;
}

@tag("aiprovider")
@route("/aiprovider")
namespace ElzaAPI.AiProvider {
    /**
     * Reads the AI provider's service info (protocol version, supported task
     * types, profiles, limits) via the provider's GET /info. Doubles as the
     * connection test of the configured AI external system. The response is
     * the provider's info document passed through as-is.
     */
    @route("/{id}/info")
    @get op getInfo(
        /** AI external system ID or CODE */
        @path id: string;
    ): AiProviderInfo | {
        /** Id does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Starts a new AI conversation: creates the thread, stores and submits its
     * first exchange to the configured provider. State changes are then
     * announced over the WebSocket (event AI_REQUEST carrying conversation and
     * request ids + new state); the client refetches the conversation detail.
     */
    @route("/conversation")
    @post op createConversation(
        @body body: AiConversationCreate;
    ): AiConversationDetail | {
        /** External system or task type does not exist */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Full refresh of one conversation: the thread with all its exchanges,
     * states and render-ready blocks.
     */
    @route("/conversation/{id}")
    @get op getConversation(
        /** conversation id */
        @path id: integer;
    ): AiConversationDetail | {
        /** Id does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Lists the current user's conversations, newest activity first;
     * optionally filtered by context.
     */
    @route("/conversation")
    @get op listConversations(
        /** context type code filter */
        @query contextType?: string;
    ): AiConversation[];

    /**
     * Submits a follow-up exchange into an existing conversation (continues
     * the provider-side conversation context).
     */
    @route("/conversation/{id}/request")
    @post op createRequest(
        /** conversation id */
        @path id: integer;
        @body body: AiRequestCreate;
    ): AiConversationDetail | {
        /** Id does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Cancels a running exchange (best effort; usage already incurred is kept).
     */
    @route("/request/{id}/cancel")
    @post op cancelRequest(
        /** request id */
        @path id: integer;
    ): AiRequest | {
        /** Id does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Reads whether the current user has a personal signing key for the
     * provider (the secret is never returned).
     */
    @route("/{id}/my-key")
    @get op getMyKey(
        /** AI external system ID or CODE */
        @path id: string;
    ): AiMyKey | {
        /** Id does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Sets/replaces the current user's personal signing key for the provider.
     * The user's subsequent tasks then bill to their personal account.
     */
    @route("/{id}/my-key")
    @put op setMyKey(
        /** AI external system ID or CODE */
        @path id: string;
        @body body: AiMyKeyUpdate;
    ): AiMyKey | {
        /** Id does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Removes the current user's personal signing key; tasks fall back to the
     * instance-wide key (the shared organizational account).
     */
    @route("/{id}/my-key")
    @delete op deleteMyKey(
        /** AI external system ID or CODE */
        @path id: string;
    ): {
        /** Removed (or none was set) */
        @Http.statusCode statusCode: 204;
    } | {
        /** Id does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * The exchange log of one request — each step with its payload (what
     * exactly was sent to the provider / what came back).
     */
    @route("/request/{id}/event")
    @get op listRequestEvents(
        /** request id */
        @path id: integer;
    ): AiRequestEvent[] | {
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
     * Search for funds
     */
    @route("/search")
    @post op searchFunds(
        /** search parameters */
        @body content: SearchParams;
    ): FindFundsResult | {
        /** Bad Request, query with incorrect parameters */
        @Http.statusCode statusCode: 400;
        @body data: BaseException;
    } | {
        /** Unauthorized */
        @Http.statusCode statusCode: 401;
        @body data: BaseException;
    } | {
        /** Forbidden */
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    };

    /**
     * Search for archive files (AS)
     */
    #deprecated "this request should replace by POST /search"
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
        @body data: BaseException;
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
        @multipartBody body: ImportFundData;
        @header contentType: "multipart/form-data";
    ): void | {
        @Http.statusCode statusCode: 400;
    } | {
        @Http.statusCode statusCode: 406;
    };

    /**
     * Export fonds
     */
    @route("/export")
    @post op exportFunds(
        /** search parameters */
        @body content: SearchParams;
    ): {
         @Http.statusCode statusCode: 200;
         @header contentType: "application/json";
         @body data: integer;
    } | {
        /** Bad Request, query with incorrect parameters */
        @Http.statusCode statusCode: 400;
        @body data: BaseException;
    } | {
        /** Unauthorized */
        @Http.statusCode statusCode: 401;
        @body data: BaseException;
    } | {
        /** Forbidden */
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    };

    /**
     * Return list of item type id & count
     */
    @route("{fundId}/usedItemtypes/{fundVersionId}")
    @get op usedItemTypes(
        ...FundIdParameter,
		...FundVersionIdParameter
    ): UsedItemType[];

    /**
     * Return list of filesystem repositories
     */
    @route("{fundId}/fsrepos")
    @get op fsRepos(
        ...FundIdParameter
    ): FsRepo[];

    /**
     * Create DAO link
     */
    @route("{fundId}/fsrepo/{fsrepoId}/linkitem/{nodeId}")
    @put op fsCreateDAOLink (
        ...FundIdParameter,
        ...FsRepoIdParameter,
        ...NodeIdParameter,
        @query path?: string
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/json";
        @body data: integer;
    };

    /**
     * Return list of filesystem items
     */
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

    /**
     * Get file of data
     */
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

    /**
     * Set mode (TREE/LIST) of ArrLevel
     */
    @route("{fundId}/node/{nodeId}/mode")
    @post op setLevelMode(
        ...FundIdParameter,
        ...NodeIdParameter,
        /** TREE (-> list = false), LIST (-> list = true) */
        @query list: boolean
    ): {
        /** The mode has changed */
        @Http.statusCode statusCode: 200;
    } | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };
}

/**
 * Hromadná akce, kterou lze spustit nad fondem.
 */
model BulkAction {
    /** Kód akce */
    code: string;
    /** Název akce */
    name: string;
    /** Popis akce */
    description?: string;
    /** Příznak rychlé akce */
    fastAction: boolean;
}

/**
 * Stav běhu hromadné akce.
 */
enum BulkActionRunState {
    WAITING,
    PLANNED,
    RUNNING,
    FINISHED,
    ERROR,
    INTERRUPTED,
    OUTDATED,
}

/**
 * Požadavek na seskupení vybraných fondů podle pravidel pro spuštění hromadné akce nad více fondy.
 * Vyplní se buď `fundIds` (explicitní výběr, má přednost), nebo `filters` (všechny fondy
 * odpovídající filtru — stejný filtr jako `/fund/search`; prázdný filtr znamená všechny fondy).
 * Fondy se na klienta nikdy nestahují — filtr se vyhodnocuje až na serveru.
 */
model FundsActionGroupRequest {
    /** Identifikátory vybraných fondů */
    fundIds?: integer[];
    /** Filtr pro výběr všech odpovídajících fondů (stejný jako u `/fund/search`) */
    filters?: AbstractFilter[];
}

/**
 * Přeskočený fond při přípravě nebo spuštění hromadné akce nad více fondy.
 */
model FundsActionSkipped {
    /** Identifikátor fondu */
    id: integer;
    /** Důvod přeskočení (např. NO_OPEN_VERSION) */
    reason: string;
}

/**
 * Skupina fondů sdílejících stejná pravidla spolu s dostupnými hromadnými akcemi.
 */
model FundsActionGroup {
    /** Identifikátor pravidel */
    ruleSetId: integer;
    /** Kód pravidel */
    ruleSetCode: string;
    /** Název pravidel */
    ruleSetName: string;
    /** Počet fondů ve skupině */
    fundCount: integer;
    /** Hromadné akce dostupné pro tato pravidla ve vícefondovém režimu */
    actions: BulkAction[];
}

/**
 * Výsledek seskupení vybraných fondů pro spuštění hromadné akce nad více fondy.
 */
model FundsActionGroupResult {
    /** Skupiny fondů podle pravidel */
    groups: FundsActionGroup[];
    /** Přeskočené fondy */
    skipped: FundsActionSkipped[];
}

/**
 * Požadavek na spuštění hromadné akce nad více fondy najednou.
 * Výběr fondů se předává stejně jako u seskupení: buď `fundIds`, nebo `filters`
 * (vyhodnocuje se znovu na serveru v okamžiku spuštění). Akce se spustí jen nad fondy,
 * jejichž otevřená verze používá pravidla `ruleSetId` (skupina zvolená v dialogu).
 */
model MultiFundActionRequest {
    /** Kód hromadné akce */
    code: string;
    /** Identifikátor pravidel zvolené skupiny */
    ruleSetId: integer;
    /** Identifikátory vybraných fondů */
    fundIds?: integer[];
    /** Filtr pro výběr všech odpovídajících fondů (stejný jako u `/fund/search`) */
    filters?: AbstractFilter[];
}

/**
 * Výsledek spuštění hromadné akce nad více fondy.
 */
model MultiFundActionResult {
    /** Identifikátor seskupení (arr_funds_change); chybí, pokud nebyl spuštěn žádný běh */
    fundsChangeId?: integer;
    /** Počet naplánovaných běhů */
    queuedCount: integer;
    /** Přeskočené fondy */
    skipped: FundsActionSkipped[];
}

/**
 * Stav jednoho per-fond běhu v rámci vícefondového seskupení (pro sledování průběhu).
 */
model FundsChangeRun {
    /** Identifikátor běhu */
    id: integer;
    /** Identifikátor verze fondu */
    fundVersionId: integer;
    /** Kód hromadné akce */
    code: string;
    /** Stav běhu */
    state: BulkActionRunState;
    /** Čas naplánování */
    datePlanned?: offsetDateTime;
    /** Čas zahájení */
    dateStarted?: offsetDateTime;
    /** Čas dokončení */
    dateFinished?: offsetDateTime;
    /** Chybové hlášení */
    error?: string;
}

/**
 * Hromadné akce nad více archivními soubory najednou (pouze pro administrátory, oprávnění FUND_BA_ALL).
 */
@tag("funds")
@route("/action")
namespace ElzaAPI.BulkAction {
    /**
     * Rozdělí vybrané fondy podle pravidel a vrátí dostupné hromadné akce pro každou skupinu.
     * Vstupem je buď explicitní seznam fondů, nebo filtr (výběr všech odpovídajících fondů).
     */
    @route("/funds/group")
    @post op groupFundsByRuleSet(
        @body body: FundsActionGroupRequest;
    ): FundsActionGroupResult;

    /**
     * Spustí hromadnou akci nad více fondy najednou. Pro každý fond zvolené skupiny
     * pravidel naplánuje samostatný běh; fondy jiných pravidel se nespouští.
     */
    @route("/queue-multi")
    @post op queueMultiFundAction(
        @body body: MultiFundActionRequest;
    ): MultiFundActionResult;

    /**
     * Vrátí jednotlivé per-fond běhy patřící do jednoho vícefondového seskupení (sledování průběhu).
     */
    @route("/funds-change/{fundsChangeId}")
    @get op getFundsChangeRuns(
        @path fundsChangeId: integer;
    ): FundsChangeRun[];
}

@tag("descitems")
@route("/descItems")
namespace ElzaAPI.DescItem {
    /**
     * Create attribute value
     */
    @route("/{fundVersionId}/create")
    @put op createDescItem(
        /** id version AS */
        @path fundVersionId: integer,
        /** new attribute value */
        @body body: NodeItem
    ): ItemDataResult;

    /**
     * Update attribute value
     */
    @route("/{fundVersionId}/{createNewVersion}/update")
    @put op updateDescItem(
        /** id version AS */
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

    /**
     * Delete attribute value
     */
    @route("/{fundVersionId}")
    @delete op deleteDescItem(
        /** id version AS */
        @path fundVersionId: integer,
        /** attribute value */
        @body body: NodeItem;
    ): ItemDataResult | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };
}

/**
 * Public API for archival description rules — item types, their
 * specifications and (in the future) data types, rule sets and policy
 * types.
 *
 * Endpoints live under `/rules` within `/api/v1` (the `/api/v1` prefix is
 * applied by the Spring controller; see `PublicationController` for the
 * same pattern). The legacy `RuleController` at `/api/rule/*` remains
 * available during migration.
 */
@tag("rules")
@route("/rules")
namespace ElzaAPI.Rules {
    /**
     * Returns all item types defined across loaded rule sets, together with
     * their specifications and rendering hints.
     *
     * Public URL: `GET /api/v1/rules/itemTypes`.
     *
     * Localized strings (`name`, `shortcut`, `description` on `ItemType` and
     * `ItemTypeSpec`, plus `name` on `ItemTableColumn`) honour the
     * `Accept-Language` request header when provided; otherwise the server
     * default language is used.
     *
     * Replaces the legacy `GET /api/rule/descItemTypes`.
     */
    @route("/itemTypes")
    @get op listItemTypes(
        /** Preferred language for localized strings (e.g. "cs", "en"). */
        @header("Accept-Language") acceptLanguage?: string
    ): ItemTypeList;

    /**
     * Returns all types of parts.
     * 
     * Public URL: `GET /api/v1/rules/partTypes`.
     * 
     * Replaces the legacy `GET /api/structure/part-type`.
     */
    @route("/partTypes")
    @get op listPartTypes(): PartType[];
}

/**
 * Area in which the access-point search runs.
 */
enum ApSearchArea {
    PREFER_NAMES,
    ALL_NAMES,
    ALL_PARTS,
    ENTITY_CODE,
}

/**
 * Filter for searching by a relation (item type, specification, target AP id).
 */
model ApSearchByRelation {
    relTypeId?: integer;
    relSpecId?: integer;
    /** Access-point id the relation must point to */
    code?: integer;
}

/**
 * Filter for searching by an item with a specific value.
 * The {@code value} is free-form — its interpretation depends on the item type.
 */
model ApSearchByItemWithValue {
    partTypeCode?: string;
    itemTypeId?: integer;
    itemSpecId?: integer;
    value?: unknown;
}

/**
 * Advanced filter for access-point search; used as the request body of /accesspoint/search.
 */
model ApAdvanceSearchFilter {
    search?: string;
    area?: ApSearchArea;
    onlyMainPart?: boolean;
    aeTypeIds?: integer[];
    /** Identifier of a concrete access point */
    code?: string;
    user?: string;
    /** Id of user the access point is assigned to */
    assignedTo?: integer;
    /** Validation result: ok | error */
    validationResult?: string;
    creation?: string;
    extinction?: string;
    relFilters?: ApSearchByRelation[];
    extFilters?: ApSearchByItemWithValue[];
    syncState?: SyncState;
    /** List of access-point ids to exclude from the result */
    excludeAps?: integer[];
}

/**
 * Search strategy for criteria-based access-point queries (name / username fields).
 */
enum ApSearchType {
    DISABLED,
    JOIN,
    FULLTEXT,
    RIGHT_SIDE_LIKE,
}

/**
 * Access-point view object returned by /accesspoint/search.
 * Externally provided (see cz.tacr.elza.controller.vo.ApAccessPointVO) — kept opaque in
 * the contract (the canonical yml emits `format: ap-access-point-vo` so the OpenAPI generator
 * maps it to the hand-coded Java + TS class).
 */
model ApAccessPointVO {}

/**
 * Paginated access-point search result returned by /accesspoint/search.
 */
model ApAccessPointSearchResult {
    /** Total number of matches (before pagination) */
    count: int64;
    /** One page of results */
    rows: ApAccessPointVO[];
}

/**
 * Body of POST /accesspoint/search.
 */
model AccessPointSearchParams {
    /** Full-text search */
    search?: string;
    /** Offset (zero-based) of the first result to return */
    from?: integer;
    /** Maximum number of rows to return */
    count?: integer;
    /** Single AP-type id; subtree-expanded on the backend */
    apTypeId?: integer;
    /** Fund version id (used to resolve scope context, optional) */
    versionId?: integer;
    /** Scope id; expanded on the backend */
    scopeId?: integer;
    /** Restrict to AP types referenced from this item type */
    itemTypeId?: integer;
    /** Restrict to AP types referenced from this item specification */
    itemSpecId?: integer;
    /** Filter by access-point state */
    state?: ApStateApproval;
    /** Filter by revision state */
    revState?: RevisionState;
    /** Search strategy applied to access-point name (criteria-query path) */
    searchTypeName?: ApSearchType;
    /** Search strategy applied to user / username (criteria-query path) */
    searchTypeUsername?: ApSearchType;
    /** Advanced filter; same precedence rule as AccessPointBatchExportParams */
    searchFilter?: ApAdvanceSearchFilter;
}

/**
 * Body of POST /accesspoint/export.
 *
 * Mirrors the scalar parameters of /accesspoint/search and accepts the same advanced filter
 * via {@link AccessPointBatchExportParams.searchFilter}. Top-level scalar fields win when
 * they conflict with fields inside searchFilter (e.g. top-level search vs. searchFilter.search).
 */
model AccessPointBatchExportParams {
    /** Full-text search */
    search?: string;
    /** Single AP-type id; subtree-expanded on the backend to include descendant types */
    apTypeId?: integer;
    /** Fund version id (used to resolve scope context, optional) */
    versionId?: integer;
    /** Scope id; expanded on the backend for the resolved fund + user-visible scopes */
    scopeId?: integer;
    /** Restrict to AP types referenced from this item type */
    itemTypeId?: integer;
    /** Restrict to AP types referenced from this item specification */
    itemSpecId?: integer;
    /** Filter by access-point state */
    state?: ApStateApproval;
    /** Filter by revision state */
    revState?: RevisionState;
    /** Advanced filter (mirrors the body of /accesspoint/search) */
    searchFilter?: ApAdvanceSearchFilter;
}

@tag("accesspoints")
@route("/accesspoint")
namespace ElzaAPI.AccessPoint {
    /**
     * Search for access points.
     * Accepts the same filter inputs as /accesspoint/export (scalar fields + ApAdvanceSearchFilter)
     * plus pagination (from, count) and the criteria-query SearchType selectors.
     */
    @route("/search")
    @post op search(
        /** filter selecting the access points to search for */
        @body params: AccessPointSearchParams
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/json";
        @body data: ApAccessPointSearchResult;
    };

    /**
     * Asynchronously export access points matching the given filter to CSV.
     *
     * Returns the request id; poll /io/export-status/{id} and download the result via /io/file/{id}.
     * Results are ordered by accessPointId ascending. Empty body / null fields means
     * "all access points visible to the user".
     */
    @route("/export")
    @post op batchExport(
        /** filter selecting the access points to export */
        @body params: AccessPointBatchExportParams
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/json";
        @body data: integer;
    } | {
        @Http.statusCode statusCode: 400;
        @body data: BaseException;
    } | {
        @Http.statusCode statusCode: 401;
        @body data: BaseException;
    } | {
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    };

    /**
     * Invalidation of access points
     */
    @route("")
    @delete op deleteAccessPoints(
        /** seznam ID nebo UUID, které chceme smazat */
        @body body: DeleteAccessPointsDetail
    ): {
        @Http.statusCode statusCode: 200;
    } | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Invalidation of access point (delete/replace)
     */
    @route("/{id}")
    @delete op deleteAccessPoint(
        /** id of accesspoint */
        @path id: string,
        /** addition for method of invalidation */
        @body body?: DeleteAccessPointDetail
   ): {
        @Http.statusCode statusCode: 200;
    } | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Restore deleted entity and return to original state
     */
    @route("/{id}/restore")
    @put op restoreAccessPoint(
        /** id of accesspoint */
        @path id: string
    ): {
        @Http.statusCode statusCode: 200;
    } | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

     /**
     * Get invalidated (deleted) entities
     */
    @route("/invalidated")
    @get op getInvalidatedEntities(
        /** number of the page */
        @query page?: integer = 0,
        /** page size */
        @query pageSize?: integer = 10
    ): InvalidatedEntities;

   /**
     * Validation of an archive entity
     */
    @route("/{id}/validate")
    @get op validateAccessPoint(
        /** id of accesspoint */
        @path id: integer,
        /** include revision */
        @query includeRevision?: boolean = false
    ): ApValidationIssues | {
          /** ID does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException;
       };

    /**
     * Get a list of entity processors
     */
    @route("/{id}/lastParticipants")
    @get op getLastParticipants(
        /** id of accesspoint */
        @path id: integer,
    ): Participant[];

    /**
     * List of automatically generated elements of the description
     */
    @route("/{id}/autoitems")
    @get op getAutoitems(
        /** id of accesspoint */
        @path id: string
    ): ResultAutoItems | {
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
    ): EntityRef | {
          /** ID does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException;
       };

    /**
     * Create new part of ApAccessPoint
     */ 
    @route("/{id}/part")
    @post op createPart(
        /** id of accesspoint */
        @path id: integer,
        /** data for creating a part */
        @body body: ApPartFormVO,
        /** accesspoint version */
        @query apVersion?: integer
    ): CreatedPart | {
          /** id does not exists */
          @Http.statusCode statusCode: 404;
          @body data: BaseException;
       };

    /**
     * Update a part of ApAccessPoint
     */
    @route("{id}/part/{partId}")
    @post op updatePart(
        /** id of accesspoint */
        @path id: integer,
        /** id of part */
        @path partId: integer,
        /** data for updating a part */
        @body body: ApPartFormVO,
        /** accesspoint version */
        @query apVersion?: integer
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/json";
        @body data: integer; 
    } | {
        /** id does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Update a revision part of ApAccessPoint
     */
    @route("{id}/revision/part/{revPartId}")
    @post op updateRevisionPart(
        /** id of accesspoint */
        @path id: integer,
        /** id of revision part */
        @path revPartId: integer,
        /** data for updating a part */
        @body body: ApPartFormVO,
        /** accesspoint version */
        @query apVersion?: integer
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/json";
        @body data: integer; 
    } | {
        /** id does not exists */
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
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/json";
        @body data: integer; 
    }  | {
        /** requested resource does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Change state of ApAccessPoint
     */
    @route("/{id}/state")
    @put op changeState(
        /** id of accesspoint */
        @path id: integer,
        /** data for change ApState */
        @body body: ApStateUpdate,
        /** actual version of ApAccessPoint */
        @query apVersion?: integer,
        /** userId */
        @query assignTo?: integer
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/json";
        @body data: integer; 
    };

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
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/json";
        @body data: integer; 
    };

    /**
     * Creating a new revision of access point
     */
    @route("/{id}/revision")
    @post op createRevision(
        /** id of accesspoint */
        @path id: integer
    ): {
        @Http.statusCode statusCode: 200;
    } | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Deleting a revision of access point 
     */
    @route("/{id}/revision")
    @delete op deleteRevision(
        /** id of accesspoint */
        @path id: integer
    ): {
        @Http.statusCode statusCode: 200;
    } | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

   /**
     * List of automatically generated description elements for revision
     */
    @route("/{id}/revision/autoitems")
    @get op getRevAutoitems(
        /** id of accesspoint */
        @path id: string
    ): ResultAutoItems | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Change state of revision of ApAccessPoint
     */
    @route("/{id}/revision/state")
    @put op changeStateRevision(
        /** id of accesspoint */
        @path id: integer,
        /** data for change state of revision of ApAccessPoint */
        @body body: RevStateChange,
        /** actual version of ApAccessPoint */
        @query apVersion?: integer,
        /** userId */
        @query assignTo?: integer
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/json";
        @body data: integer; 
    } | {
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
        @query apVersion?: integer
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/json";
        @body data: integer; 
    } | {
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
        @query apVersion?: integer
    ): {
         /** actual version of ApAccessPoint */
         @Http.statusCode statusCode: 200;
         @header contentType: "application/json";
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
        @body body: ApStateUpdate,
        /** actual version of ApAccessPoint */
        @query apVersion?: integer
   ): {
         @Http.statusCode statusCode: 200;
         @header contentType: "application/json";
         @body data: integer;
    };

    /**
     * Decision on forced export ELZA -> CAM
     */
    @route("/export/{queueItemId}") 
    @put op exportForceOrNo(
        /** id of queue */
        @path queueItemId: integer,
        /** force export: yes or no */
        @query force: boolean
    ): {
        @Http.statusCode statusCode: 200;
    } | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
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
	): {
         @Http.statusCode statusCode: 200;
         @header contentType: "application/json";
         @body data: integer;
    };

    /**
     * Getting the export status by request id
     */
	@route("/export-status/{requestId}")
	@get op getExportStatus(
		@path requestId: integer
	): ExportRequestStatus | {
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

// ---------------------------------------------------------------------------
// Publications — public integration API for an external publication system.
//
// Elza's internal publication states (NEW, PREPARE_ERROR, INVALIDATED) are
// not part of the public contract. The client never observes them: /available
// does not return such records, and /download collapses them into the more
// general "file not available" responses.
// ---------------------------------------------------------------------------

/**
 * Entry in the list of prepared exports.
 *
 * The archival institution code and NAD are not part of this header — they
 * are contained in the XML.
 */
model AvailablePublication {
    /** Publication identifier (ARR_EXPORT.export_id). */
    publicationId: integer;

    /** Archival fund identifier. */
    fundId: integer;

    /** Identifier of the fund version the export was created from. */
    fundVersionId: integer;

    /** Time the publication request was created. */
    createdTime: offsetDateTime;

    /**
     * ID of the last fund change included in the export (reference to
     * arr_change). May be null if the originating change has been removed
     * from arr_change — the XML content itself is not affected.
     */
    lastChangeId?: integer;

    /**
     * Export data format expressed as the URI of the target XSD namespace.
     * The client should pick the schema to parse the XML with according to
     * this value; unknown values should be ignored.
     *
     * Known values:
     *  - `http://elza.tacr.cz/schema/v2` — Elza native XML schema, version 2.
     *
     * The field is intentionally typed as a free-form string (not an enum)
     * so that new formats can be introduced without a TypeSpec change.
     */
    publicationType: string;
}

/**
 * Page of prepared exports for a given target system.
 *
 * The caller remembers the `nextTransaction` value from the response and
 * sends it back in the next request as `lastTransaction`. The value is an
 * opaque key from the caller's perspective — its internal structure may
 * change in the future and must not be parsed by the caller.
 */
model AvailablePublications {
    /**
     * Transaction identifier the caller should send in the next request as
     * `lastTransaction`. When `items` is empty, the same value as the
     * input is returned.
     */
    nextTransaction: string;

    /**
     * List of prepared exports after `lastTransaction` (exclusive),
     * ordered ascending by internal transaction.
     *
     * At most 100 records are returned. If multiple prepared exports exist
     * for the same fund with the same export parameters (target system
     * and filter), only the most recent one is returned — superseded
     * entries are suppressed so the recipient does not have to download
     * already-obsolete versions.
     *
     * Records that do not currently have a downloadable file (e.g. removed
     * by retention or invalidated) are not returned.
     */
    items: AvailablePublication[];
}

/**
 * Outcome reported back to Elza by the publication system.
 */
enum PublicationReportStatus {
    /** Publication succeeded. */
    OK,
    /** Publication failed. */
    ERROR,
}

/**
 * Body of `POST /publications/status/{id}`.
 */
model PublicationStatusReport {
    /** Publication outcome. */
    status: PublicationReportStatus;

    /**
     * Time at which publication succeeded or failed, as reported by the
     * publication system.
     */
    publishedAt: offsetDateTime;

    /** Error message. Filled only when status = ERROR. */
    errorMessage?: string;
}

/**
 * Public API for the publication system.
 *
 * Endpoints live under `/publications` within `/api/v1` and exchange data
 * between Elza and an external publication system:
 *
 *  - Elza offers the list of prepared exports,
 *  - the publication system downloads the prepared XML,
 *  - the publication system reports the publication outcome back.
 *
 * A dedicated technical account with restricted permissions is recommended
 * for deployment.
 *
 * Error codes returned in `BaseException.code`:
 *
 *  - `PUBLICATION_TYPE_NOT_FOUND` — no publication type with the given code
 *    exists
 *  - `PUBLICATION_TYPE_INACTIVE`  — the publication type has been
 *    deactivated and no longer accepts publication traffic
 *  - `PUBLICATION_NOT_FOUND`     — the publication ID is unknown or is in
 *    an internal state that is not exposed to the client
 *  - `PUBLICATION_FILE_GONE`     — the XML file is no longer available
 *    (publication was invalidated or removed by retention)
 */
@tag("publication")
@route("/publications")
namespace ElzaAPI.Publication {
    /**
     * Get the list of prepared XML exports for a given target system.
     *
     * On the first call `lastTransaction` is omitted. The response carries
     * `nextTransaction`, which the caller stores and sends in the next
     * request as `lastTransaction`. This way only new exports since the
     * previous call are returned.
     *
     * At most 100 records are returned. If multiple prepared exports exist
     * for the same fund, only the most recent one is included.
     *
     * If the publication type (target system) is inactive, the call returns
     * 403 — information about prepared exports is not exposed in that case.
     *
     * @param targetSystem Publication type code (ARR_EXPORT_TYPE.code),
     *   which also identifies the target system.
     * @param lastTransaction Opaque key of the last already-processed
     *   transaction. Omitted on the first call. The caller must not parse
     *   the key — Elza reserves the right to change its format in the
     *   future.
     */
    @route("/available/{targetSystem}")
    @get op getAvailablePublications(
        @path targetSystem: string,
        @query lastTransaction?: string,
    ): AvailablePublications | {
        /** `PUBLICATION_TYPE_NOT_FOUND` — no publication type with this code exists. */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    } | {
        /** `PUBLICATION_TYPE_INACTIVE` — the publication type is inactive. */
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    };

    /**
     * Download the prepared XML.
     *
     * The client should only call this with an ID it received from
     * `/available`. The download succeeds as long as the XML file is
     * physically present on the server; each successful download updates
     * the internal audit trail of the last fetch.
     *
     * The file is returned as `application/xml` with header
     * `Content-Disposition: attachment; filename="{mark}-{fundNumber}-{publicationId}.xml"`.
     * If the fund has neither `mark` nor `fundNumber` set, the fallback
     * `fundId-{fundId}-{publicationId}.xml` is used.
     *
     * HTTP responses:
     *  - 200 + XML — the file is available
     *  - 403       — the publication type has been deactivated
     *  - 404       — unknown ID, or an ID in an internal state that is not
     *                exposed to the client (e.g. preparation in progress —
     *                such IDs are never returned by `/available` and the
     *                client should not encounter them)
     *  - 410       — the file is permanently unavailable to the client
     *                (publication invalidated by the user, or retention
     *                expired)
     *
     * @param id Publication identifier (ARR_EXPORT.export_id).
     */
    @route("/download/{id}")
    @get op downloadPublication(
        @path id: integer,
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/xml";
        @body data: bytes;
    } | {
        /** `PUBLICATION_TYPE_INACTIVE` — the publication type is inactive. */
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    } | {
        /** `PUBLICATION_NOT_FOUND` — the publication ID is not available to the client. */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    } | {
        /** `PUBLICATION_FILE_GONE` — XML file is no longer available (invalidation or retention). */
        @Http.statusCode statusCode: 410;
        @body data: BaseException;
    };

    /**
     * Report the publication outcome from the publication system.
     *
     * The client reports the outcome of a publication it has been
     * working with — typically after a successful `/download/{id}`,
     * but a report from a publication that was prepared and not yet
     * marked as fetched is also accepted (e.g. when the download
     * response was lost in transit but the publication system still
     * processed the file).
     *
     *  - status OK    — publication succeeded
     *  - status ERROR — publication failed
     *
     * The call is last-writer-wins: a publication system that gets
     * resynchronised may report the same publication any number of
     * times — identical replays are no-ops, new values silently
     * overwrite. Rate-limiting / conflict detection may be added in
     * a later revision.
     *
     * @param id Publication identifier (ARR_EXPORT.export_id).
     */
    @route("/status/{id}")
    @post op reportPublicationStatus(
        @path id: integer,
        @body report: PublicationStatusReport,
    ): {
        @Http.statusCode statusCode: 200;
    } | {
        /** `PUBLICATION_NOT_FOUND` — the publication ID is not available to the client. */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };
}

// ---------------------------------------------------------------------------
// Internal publication API — consumed only by the Elza web UI.
//
// Served under /api/internal (separate Spring controller). Not intended for
// external integration; backwards compatibility is not guaranteed.
//
// All seven internal states are exposed here so that admins/users can see
// the operational state of every record they manage. Compare to the public
// API, which deliberately hides NEW / PREPARE_ERROR / INVALIDATED.
// ---------------------------------------------------------------------------

/**
 * Internal-only view of a publication's life-cycle state. Mirrors
 * {@code ARR_EXPORT.state} 1-to-1.
 */
enum PublicationStateInternal {
    NEW,
    PREPARED,
    FETCHED,
    PUBLISHED,
    PREPARE_ERROR,
    PUBLISH_ERROR,
    INVALIDATED,
}

/**
 * Kind of target environment a publication type connects to.
 */
enum ConnectionType {
    DEVELOPMENT,
    TEST,
    PRODUCTION,
}

/**
 * Publication type — target-system configuration.
 *
 * Used as both the read shape (responses) and the write shape (create / update
 * request bodies). The `id` is server-assigned and is absent on create or
 * update requests — on `PUT /{id}` the path parameter is authoritative; any
 * `id` carried in the body is ignored.
 *
 * Changing `code` on an existing type is permitted by the API but will break
 * the cursor for any publication system bound to the old code. The UI
 * surfaces a warning (or disables the field) to prevent accidental changes.
 */
model PublicationType {
    /** Server-assigned identifier. Absent on create / update request bodies. */
    id?: integer;

    /** Human-readable name shown in the UI. */
    name: string;

    /**
     * Stable code identifying the target system; also used as
     * {@code targetSystem} in the public `/publications/available` endpoint.
     */
    code: string;

    /** When false, the type accepts neither new publications nor downloads. */
    active?: boolean = true;

    /**
     * Code of {@code rul_export_filter}. Null means no filter
     * (the entire fund is exported).
     */
    exportFilterCode?: string;

    /**
     * Number of most recent exports retained on disk per type. 0 means
     * unlimited; 1 keeps only the latest; default is 5.
     */
    @minValue(0)
    retentionCount?: integer = 5;

    /** Users holding the FUND_EXPORT permission may publish into this type. */
    allowPermExport: boolean;

    /** Users holding the FUND_PUBLISH permission may publish into this type. */
    allowPermPublication: boolean;

    /** Target environment kind (development / test / production). */
    connectionType: ConnectionType;
}

/**
 * One publication record as shown in the UI.
 */
model PublicationDetail {
    /** Publication identifier (ARR_EXPORT.export_id). */
    id: integer;

    /** Publication type ID. */
    typeId: integer;

    /** Publication type code (target system). */
    typeCode: string;

    /** Publication type name (display). */
    typeName: string;

    /** Fund version the export was created from. */
    fundVersionId: integer;

    /** Current life-cycle state. */
    state: PublicationStateInternal;

    /** User who created the publication request. */
    createdBy: UserRef;

    /** Time the publication request was created. */
    createdAt: offsetDateTime;

    /** Time XML preparation finished. Null until PREPARED. */
    preparedAt?: offsetDateTime;

    /** Time of the last fetch by the publication system. */
    lastFetchedAt?: offsetDateTime;

    /** Time the publication reported completion. */
    publishedAt?: offsetDateTime;

    /** Time the record entered an error state. */
    errorAt?: offsetDateTime;

    /** Time the record was invalidated by a user. */
    invalidatedAt?: offsetDateTime;

    /**
     * ID of the last fund change included in the export snapshot. Null
     * when the originating change has been purged (the XML itself is
     * not affected).
     */
    lastChangeId?: integer;

    /** Error description for PREPARE_ERROR / PUBLISH_ERROR. */
    errorMessage?: string;

    /** Whether a downloadable XML file is currently available on the server. */
    hasDownloadableFile: boolean;
}

/**
 * Paginated list of publications for a fund.
 */
model PublicationList {
    items: PublicationDetail[];
    /** Total number of publications matching the query, not just on this page. */
    totalCount: integer;
}

/**
 * Body of POST /fund/{fundId}/publication — request to enqueue a new
 * publication for the current open version of the fund.
 */
model CreatePublication {
    /** Identifier of the publication type to publish into. */
    publicationTypeId: integer;
}

/**
 * Body of POST /fund/{fundId}/publication/{publicationId}/copy — copy an
 * existing prepared export into another target system. The target type
 * must have a compatible filter setting.
 */
model CopyPublication {
    /** Identifier of the publication type to copy the export into. */
    targetPublicationTypeId: integer;
}

/**
 * Path parameter for publication ID inside fund-scoped routes.
 */
model PublicationIdParameter {
    /** Publication ID (ARR_EXPORT.export_id). */
    @path
    publicationId: integer;
}

/**
 * Publication type management (admin dialog "Správa typů publikace").
 *
 * Requires administrator permission. Type configuration changes do not
 * affect already-issued publications — each export keeps a snapshot of
 * the filter that was active when it was created.
 */
@tag("publication-internal")
@route("/publication/types")
namespace ElzaAPI.Internal.PublicationTypeAdmin {
    /** Return all publication types, including inactive ones. */
    @get op listPublicationTypes(): PublicationType[];

    /** Create a new publication type. */
    @post op createPublicationType(
        @body body: PublicationType,
    ): PublicationType | {
        /** Code already used by another type. */
        @Http.statusCode statusCode: 409;
        @body data: BaseException;
    };

    /** Update an existing publication type. */
    @route("/{id}")
    @put op updatePublicationType(
        @path id: integer,
        @body body: PublicationType,
    ): PublicationType | {
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    } | {
        /** Code conflicts with another existing type. */
        @Http.statusCode statusCode: 409;
        @body data: BaseException;
    };

    /**
     * Remove a publication type.
     *
     * Deactivating via {@code active=false} is preferred so the history of
     * already-issued exports is preserved. A hard delete is rejected when
     * any record in `arr_export` still references this type.
     */
    @route("/{id}")
    @delete op deletePublicationType(
        @path id: integer,
    ): {
        @Http.statusCode statusCode: 204;
    } | {
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    } | {
        /** Cannot delete — exports still reference this type. */
        @Http.statusCode statusCode: 409;
        @body data: BaseException;
    };
}

/**
 * Fund-scoped publication operations (UI page "Publikace" on a fund).
 *
 * Visible to users holding FUND_PUBLISH for the fund (or FUND_PUBLISH_ALL).
 * Enforcement is wired in alongside the service implementation.
 */
@tag("publication-internal")
@route("/fund")
namespace ElzaAPI.Internal.FundPublication {
    /**
     * Return the publications for a fund, newest first.
     *
     * The UI must paginate — the list can grow large.
     */
    @route("/{fundId}/publication")
    @get op listFundPublications(
        ...FundIdParameter,
        /** Optional filter on publication type. */
        @query publicationTypeId?: integer,
        /** Offset for pagination. */
        @query offset?: integer = 0,
        /** Page size. */
        @query @minValue(1) @maxValue(200) limit?: integer = 50,
    ): PublicationList;

    /**
     * Enqueue a new publication for the current open version of the fund.
     *
     * Rejected with 409 if the current fund version already has a
     * publication of the same type in {@link PublicationStateInternal.NEW}
     * state — we avoid racing two identical async generations. A previous
     * publication in {@link PublicationStateInternal.PREPARED} (or later)
     * does NOT block: the fund may have changed and a fresh publication
     * carries different content.
     */
    @route("/{fundId}/publication")
    @post op createFundPublication(
        ...FundIdParameter,
        @body body: CreatePublication,
    ): PublicationDetail | {
        /** Publication type or fund not found. */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    } | {
        /** Publication type is inactive. */
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    } | {
        /** An identical publication is already pending or prepared. */
        @Http.statusCode statusCode: 409;
        @body data: BaseException;
    };

    /**
     * Download the prepared XML from the UI.
     *
     * Unlike the public {@code /publications/download/{id}}, this UI call
     * does not advance the publication's state and does not touch
     * {@code last_fetched_at} — it is for human inspection.
     */
    @route("/{fundId}/publication/{publicationId}/download")
    @get op downloadFundPublication(
        ...FundIdParameter,
        ...PublicationIdParameter,
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "application/xml";
        @body data: bytes;
    } | {
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    } | {
        /** File is not available (INVALIDATED or removed by retention). */
        @Http.statusCode statusCode: 410;
        @body data: BaseException;
    };

    /**
     * Invalidate a publication.
     *
     * The associated DMS file is deleted from disk; the
     * {@code arr_export} row is preserved as an audit trail and its
     * state transitions to {@link PublicationStateInternal.INVALIDATED}.
     *
     * A regular user can only invalidate publications they created that
     * have not yet been fetched (state PREPARED). Administrators can
     * invalidate at any time.
     */
    @route("/{fundId}/publication/{publicationId}")
    @delete op invalidateFundPublication(
        ...FundIdParameter,
        ...PublicationIdParameter,
    ): {
        @Http.statusCode statusCode: 204;
    } | {
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    } | {
        /** Caller is not allowed to invalidate this publication. */
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    };

    /**
     * Copy an existing publication into another target system.
     *
     * The target publication type must have a compatible filter setting
     * (same {@code rul_export_filter} or both unfiltered). A new
     * {@code arr_export} row is created with its own life cycle; the
     * source row is unchanged.
     */
    @route("/{fundId}/publication/{publicationId}/copy")
    @post op copyFundPublication(
        ...FundIdParameter,
        ...PublicationIdParameter,
        @body body: CopyPublication,
    ): PublicationDetail | {
        /** Source publication, target publication type, or fund not found. */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    } | {
        /** Target publication type is inactive. */
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    } | {
        /** Target type has an incompatible filter setting. */
        @Http.statusCode statusCode: 409;
        @body data: BaseException;
    };
}

// ---------------------------------------------------------------------------
// Internal access-point binding API — consumed only by the Elza web UI.
//
// Served from the same Spring prefix as other UI-only endpoints
// (currently /api/v1, mounted on a separate AccessPointBindingInternalController).
// Not intended for external integration; backwards compatibility is not
// guaranteed.
//
// Surfaces CAMv2 metadata that arrives alongside each entity revision:
// warnings/issues and per-revision participant history.
// ---------------------------------------------------------------------------

@tag("accesspoint-internal")
@route("/binding")
namespace ElzaAPI.Internal.AccessPointBinding {
    /**
     * List CAM-side issues attached to a binding.
     */
    @route("/{bindingId}/issues")
    @get op getBindingIssues(
        /** id of binding */
        @path bindingId: integer,
    ): ExtIssue[] | {
        /** binding not found */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Paginated history of binding revisions, newest first, built from
     * ap_binding_state and ap_binding_participant. Participants within each
     * revision are ordered by lastChange (ascending).
     */
    @route("/{bindingId}/history")
    @get op getBindingHistory(
        /** id of binding */
        @path bindingId: integer,
        @query offset?: integer = 0,
        /** server caps at 100 */
        @query limit?: integer = 100,
    ): ExtHistory | {
        /** binding not found */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };
}

@tag("report")
@route("/report")
namespace ElzaAPI.Report {
    /**
     * Types of statistics values
     */
    enum ValueType {
        INT,
        STRING,
        BOOLEAN,
        DATE,
        USER_ID,
        GROUP_ID,
        FUND_ID,
        AP_ID,
        ITEM_TYPE_ID,
        ITEM_SPEC_ID,
        SCOPE_ID,
        AP_TYPE_ID
    }

    /**
     * Types of report output formats
     */
    enum ReportFormat {
        JSON,
        CSV
    }

    /**
     * Statistics parameters
     */
    model ReportParameters {
        /** param values */
        params?: ReportParamValue[];
    }

    /**
     * List of values ​​for the parameter
     */
    model ReportParamValue {
        /** parameter code */
        code: string;
        /** parameter values */
        values: Value[];
    }

    /**
     * Statistical report parameter
     */
    model ReportParamDefinition {
        /** parameter code */
        code: string;
        /** parameter name */
        name: string;
        /** parameter type */
        paramType: ValueType;
        /** parameter required or not */
        required: boolean;
        /** parameter repeatable or not */
        repeatable: boolean;
        /** parameter default value(s) */
        values?: Value[];
    }

    /**
     * Category witl report definitions
     */
    model ReportCategory {
        /** Category code */
        code: string;
        
        /** Category name */
        name: string;

        /**
         * List of definitions
         */
        reportDefinitions: ReportDefinition[];
    }

    /**
     * Element from the statistics list
     */
    model ReportDefinition {
        /** statistics code */
        code: string;
        /** statistics name */
        name: string;
        /** description of statistics */
        description?: string;
        /** statistics parameters */
        params?: ReportParamDefinition[];
    }

    /**
     * Report INTEGER value
     */ 
    model ValueInteger extends Value {
        @invisible(Lifecycle)
        valueType: ValueType.INT;
        intValue: integer;
    }

    /**
     * Report STRING value
     */ 
    model ValueString extends Value {
        @invisible(Lifecycle)
        valueType: ValueType.STRING;
        textValue: string;
    }

    /**
     * Report DATE value
     */ 
    model ValueDate extends Value {
        @invisible(Lifecycle)
        valueType: ValueType.DATE;
        dateValue: offsetDateTime;
    }

    /**
     * Report ACCESSPOINT_ID value
     */
    model ValueAccesspointId extends Value {
        @invisible(Lifecycle)
        valueType: ValueType.AP_ID;
        accesspointId: integer;
    }

    /**
     * Report data FOND_ID value
     */
    model ValueFondId extends Value {
        @invisible(Lifecycle)
        valueType: ValueType.FUND_ID;
        fondId: integer;
    }

    /**
     * Report value
     */
    @discriminator("valueType")
    model Value {
        valueType: ValueType
    }

    /**
     * Row of report in json format
     */
    model ReportRow {
        cols: Value[];
    }

    /**
     * Report in json format
     */
    model ReportData {
        header: string[];
        rows: ReportRow[];
        sourceDataDate: offsetDateTime;
    }

    /**
     * Getting a list of categories 
     * and report definitions
     */
    @route("/definitions")
    @get op getDefinitions(
    ): ReportCategory[];

    /**
     * Run generating statistics
     */
    @route("/generate/{code}")
    @post op generateReport(
        /** statistický kód pro generování */
        @path code: string;
        /** hodnoty zadaných parametrů */
        @body body: ReportParameters;
    ): {
         @Http.statusCode statusCode: 200;
         @header contentType: "application/json";
         @body data: integer;
    };

    /**
     * Get the status of running statistics
     */
    @route("/status/{requestId}")
    @get op getReportStatus(
        @path requestId: integer;
    ): RequestProcessState | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    } | {
        /** Request execution failed */
        @Http.statusCode statusCode: 500;
        @body data: BaseException;
    };

    /**
     * Download statistics results by request id
     */
	@route("/{requestId}")
	@get op getReport(
		@path requestId: integer;
        @query format: ReportFormat;
	): ReportData | {
        @Http.statusCode statusCode: 200;
        @header contentType: "text/csv";
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
        @query daDaoIdList: integer[];
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
        @query daDaoIdList: integer[];
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
        @query daDaoIdList: integer[];
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
        @query daDaoIdList: integer[];
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
         @query daAipIdList: integer[];
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
         @query daAipIdList: integer[];
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
         @query daAipIdList: integer[];
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
         @query daAipIdList: integer[];
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

@tag("node")
@route("/node")
namespace ElzaAPI.Node {
    /**
     * Received quote
     */
    model PlainTextRepresentation {
        /** název generátoru pro uživatel */
        name: string;
        /** interní kód generátoru */
        code: string;
        /** vytvořená citace */
        value: string;
    }

    /**
     * Get basic information about a node by its numeric ID.
     */
    @route("/info/id/{nodeId}")
    @get op getNodeInfoById(
        ...NodeIdParameter;
    ): NodeInfo | {
        /** Node not found */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    } | {
        /** Node found in more than one active level — ambiguous request */
        @Http.statusCode statusCode: 409;
        @body data: BaseException;
    };

    /**
     * Get basic information about a node by its UUID.
     */
    @route("/info/uuid/{nodeUuid}")
    @get op getNodeInfoByUuid(
        /** Node UUID (36 characters) */
        @path nodeUuid: string;
    ): NodeInfo | {
        /** Node not found */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    } | {
        /** Node found in more than one active level — ambiguous request */
        @Http.statusCode statusCode: 409;
        @body data: BaseException;
    };

    /**
     * Get quote by fundVersionId & nodeId
     */
	@route("/plain-text/{fundVersionId}/{nodeId}")
	@get op getPlainText(
        @path fundVersionId: integer;
        @path nodeId: integer;
    ): PlainTextRepresentation[] | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Search for archive files (funds)
     */
    @route("/search")
    @post op search(
        /** search parameters */
        @body body: SearchParams
    ): NodeSearchResult | {
        /** Bad Request, query with incorrect parameters */
        @Http.statusCode statusCode: 400;
        @body data: BaseException;
    } | {
        /** Unauthorized */
        @Http.statusCode statusCode: 401;
        @body data: BaseException;
    } | {
        /** Forbidden */
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    };

    /**
     * Search for nodes in archive file (fund)
     */
    @route("/search/{fundId}")
    @get op getSearchResult(
        ...FundIdParameter;
    ): NodeTreeData[] | {
        /** Bad Request, query with incorrect parameters */
        @Http.statusCode statusCode: 400;
        @body data: BaseException;
    } | {
        /** Unauthorized */
        @Http.statusCode statusCode: 401;
        @body data: BaseException;
    } | {
        /** Forbidden */
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    };

    /**
     * Get data for a unit of description (JP)
     */
    @route("/node-data")
    @post op getNodeData(
        @body param: NodeDataParam;
    ): NodeData;

    /**
     * Copies the attribute of the specified type from the node's parent
     */
    @route("/copyOlderSiblingAttribute")
    @put op copyOlderSiblingAttribute(
        /** version id of tree */
        @query versionId: integer,
        /** id of the attribute we want to copy */
        @query descItemTypeId: integer,
        /** node to assign values from the older brother */
        @body body: NodeBase;
    ): {
        /** The request has succeeded. */
        @Http.statusCode statusCode: 200;
    };
}

/**
 * Value of a structured type
 */
model StructuredObject {
    id: integer;
    value: string;
    typeCode: string;
    complement: string;
    state: "OK" | "ERROR" | "TEMP";
    assignable: boolean;
    errorDescription?: string;
}

/**
 * Form data
 */
model StructuredObjectItems {
    parent: StructuredObject;
    items: StructuredObjectItem[];
    itemTypes: FormItemType[];
}

/**
 * Operations on Structured Data
 */
@tag("structure")
@route("/funds/sdo")
namespace ElzaAPI.Sdo {

    /**
     * Data for bulk value creation
     */
    model CopyObjectParam {
        count: integer;
        incrementedTypeIds: integer[];
    }

    /**
     * Data for bulk value update
     */
    model BatchUpdateParam {
        ids: integer[];
        autoincrementItemTypeIds: integer[];
        deleteItemTypeIds: integer[];
        items: StructuredObjectItem[];
    }

    /**
     * Status of a structured data type
     */
    model ItemResult {
        parent: StructuredObject;
        item: StructuredObjectItem;
    }

    /**
     * Result found
     */
    model FindResult {
        count: int64;
        rows: StructuredObject[];
    }

    /**
     * Structure type
     */
    model Type {
        id: integer;
        name: string;
        code: string;
        anonymous: boolean;
    }

    /**
     * Extention fund
     */
    model ExtensionFund {
        id: integer;
        name: string;
        code: string;
        active: boolean;
    }

    /**
     * Creating object of the structured data type
     */
    @route("/{fundId}")
    @post op createObject(
        /** structured data type code */
        @body body: string;
        /** fund id */
        @path fundId: integer;
        /** value for the structured data */
        @query value?: string;
    ): StructuredObject;

    /**
     * Creating duplicates of a structured data type and an auto-increment field
     */
    @route("/{fundId}/{structuredObjectId}/copy")
    @post op copyObject(
        /** batch of data to create */
        @body body: CopyObjectParam;
        /** fund id */
        @path fundId: integer;
        /** structure data id */
        @path structuredObjectId: integer;
    ): void;

    /**
     * Bulk update of items/values of a structural type
     */
    @route("/{fundId}/batchUpdate/{structureTypeCode}")
    @post op updateObjects(
        /** batch of data to update */
        @body body: BatchUpdateParam;
        /** fund id */
        @path fundId: integer;
        /** structure type code */
        @path structureTypeCode: string;
    ): void;

    /**
     * Confirms the value of a structured data type. Sets the value
     */
    @route("/{fundId}/{structuredObjectId}/confirm")
    @post op confirm(
        /** fund id */
        @path fundId: integer;
        /** structure data id */
        @path structuredObjectId: integer;
    ): StructuredObject | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Assignability settings.
     */
    @route("/{fundId}/assignable/{assignable}")
    @post op setDataAssignable(
        /** value ids for a structured data type */
        @body body: integer[];
        /** fund id */
        @path fundId: integer;
        /** assignable value */
        @path assignable: boolean;
    ): void;

    /**
     * Getting the value of a structured data type
     */
    @route("/{fundId}/{structuredObjectId}")
    @get op getObject(
        /** fund id */
        @path fundId: integer;
        /** structure data id */
        @path structuredObjectId: integer;
        /** fund version id */
        @query fundVersionId?: integer;
    ): StructuredObject | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Deleting a value(s) of a structured data type
     */
    @route("/{fundId}")
    @delete op deleteObjects(
        /** fund id */
        @path fundId: integer;
        /** fund version id */
        @query fundVersionId?: integer;
        /** list of id value(s) of structured data type */
        @body data: integer[];
    ): void | {
        /** ID does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Create item value of a structured data type
     */
    @route("/{fundId}/item/{structuredObjectId}")
    @post op createItem(
        /** node item */
        @body body: StructuredObjectItem;
        /** fund id */
        @path fundId: integer;
        /** structure data id */
        @path structuredObjectId: integer;
    ): ItemResult;

    /**
     * Update item value of a structured data type
     */
    @route("/{fundId}/item/{structuredObjectId}/{createNewVersion}")
    @put op updateItem(
        /** node item */
        @body body: StructuredObjectItem;
        /** fund id */
        @path fundId: integer;
        /** structure data id */
        @path structuredObjectId: integer;
        /** create a new version */
        @path createNewVersion: boolean;
    ): ItemResult | {
        /** item does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Delete item value of a structured data type
     */
    @route("/{fundId}/item/{structuredObjectId}/{itemObjectId}")
    @delete op deleteItem(
        /** fund id */
        @path fundId: integer;
        /** structured object id */
        @path structuredObjectId: integer;
        /** item object id */
        @path itemObjectId: integer;
    ): void | {
        /** item does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * Delete items based on the value of a data type structure by attribute type
     */
    @route("/{fundId}/item/{structuredObjectId}/by-type/{itemTypeId}")
    @delete op deleteItemsByType(
        /** fund id */
        @path fundId: integer;
        /** structured object id */
        @path structuredObjectId: integer;
        /** item type id */
        @path itemTypeId: integer;
    ): void | {
        /** item does not exists */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };

    /**
     * GET Searching for values of a structured data type: findStructObj()
     */
    @route("/{fundId}/search/{structureTypeCode}")
    @get op findStructObj(
        /** fund id */
        @path fundId: integer;
        /** structure type code */
        @path structureTypeCode: string;
        /** text for filtering (optional) */
        @query search?: string;
        /** assignable value */
        @query assignable?: boolean;
        /** from default = 0 */
        @query from?: integer = 0;
        /** max number of items */
        @query count?: integer = 200;
        /** fund version id */
        @query fundVersionId?: integer;
    ): FindResult;

    /**
     * GET Lists the possible data types that can be used in AS: findStructureTypes()
     */
    @route("/type")
    @get op findStructureTypes(
        /** fund version id */
        @query fundVersionId?: integer;
    ): Type[];

    /**
     * GET Getting data for a structured data type form: getFormStructureItems()
     */
    @route("/{fundId}/item/{structuredObjectId}")
    @get op getFormStructureItems(
        /** fund id */
        @path fundId: integer;
        /** structured object id */
        @path structuredObjectId: integer;
        /** fund version id */
        @query fundVersionId?: integer;
    ): StructuredObjectItems;

    /**
     * GET Finds available and enabled AS extensions: findFundStructureExtension()
     */
    @route("/{fundId}/extension/{structureTypeCode}")
    @get op findFundStructureExtension(
        /** fund id */
        @path fundId: integer;
        /** structure type code */
        @path structureTypeCode: string;
        /** fund version id */
        @query fundVersionId?: integer;
    ): ExtensionFund[];

    /**
     * PUT Sets a specific extension on the AS: setFundStructureExtensions()
     */
    @route("/{fundId}/extension/{structureTypeCode}")
    @put op setFundStructureExtensions(
        /** fund id */
        @path fundId: integer;
        /** structure type code */
        @path structureTypeCode: string;
        /** structure ext codes */
        @body body: string[];
    ): void;
}

/**
 * Operations on the printed forms of archive files
 */
@tag("output")
@route("/funds/out")
namespace ElzaAPI.Output {
    /**
     * Output state
     */
    enum State {
        /**Rozpracovaný */
        OPEN,
        /** Běží hromadná akce */
        COMPUTING,
        /** Generování */
        GENERATING,
        /** Vygenerovaný */
        FINISHED,
        /** Vygenerovaný neaktuální */
        OUTDATED
    }

    /**
     * Náhrada za `OutputFormDataNewVO`
     */
    model FormData {
        parent: Def;
        items: OutputItem[];
        itemTypes: FormItemType[];
        unusedItemTypeIds: integer[];
    }

    /**
     * Náhrada za `ArrOutputVO`
     */
    model Def {
        id: integer;
        internalCode: string;
        name: string;
        state: State;
        error?: string;
        nodes?: NodeTreeData[];
        outputTypeId: integer;
        templateIds?: integer[];
        outputResultIds?: integer[];
        generatedDate: offsetDateTime;
        version: integer;
        outputSettings: string;
        createDate: offsetDateTime;
        deleteDate?: offsetDateTime;
        scopes?: Scope[]; // ?
        anonymizedAp?: ApAccessPointVO; // ?
        outputFilterId?: integer;
    }

    /**
     * Náhrada za `OutputItemResult`
     */
    model ItemRes {
        parent: Def;
        item: OutputItem;
    }

    /**
     * Data for CSV import into outputItem
     */
    model OutputItemCsvImportData {
        /** output version */
        outputVersion: HttpPart<integer>;
        /** output id */
        outputId: HttpPart<integer>;
        /** desc item type id */
        descItemTypeId: HttpPart<integer>;
        /** CSV file */
        file: HttpPart<bytes>;
    }

    /**
     * Create output item
     */
    @route("/item/{outputId}/{fundVersionId}/{outputVersion}/create")
    @post op createOutputItem(
        /** output id */
        @path outputId: integer;
        /** fund version id */
        @path fundVersionId: integer;
        /** output version */
        @path outputVersion: integer;
        /** body output item */
        @body outputItem: OutputItem;
    ): ItemRes;

    /**
     * Update output item
     */
    @route("/item/{fundVersionId}/{outputVersion}/update")
    @put op updateOutputItem(
        /** fund version id */
        @path fundVersionId: integer;
        /** output version */
        @path outputVersion: integer;
        /** body output item */
        @body outputItem: OutputItem;
    ): ItemRes;

    /**
     * Delete output item
     */
    @route("/item/{fundVersionId}/{outputVersion}/{outputItemId}")
    @delete op deleteOutputItem(
        /** fund version id */
        @path fundVersionId: integer;
        /** output version */
        @path outputVersion: integer;
        /** output item id */
        @path outputItemId: integer;
    ): ItemRes;

    /**
     * Deleting attribute values ​​by type
     */
    @route("/item/{fundVersionId}/{outputId}/{outputVersion}/{itemTypeId}")
    @delete op deleteOutputItemsByType(
        /** fund version id */
        @path fundVersionId: integer;
        /** output id */
        @path outputId: integer;
        /** output version */
        @path outputVersion: integer;
        /** item type id */
        @path itemTypeId: integer;
    ): ItemRes;

    /**
     * Set the attribute to "Undefined"
     */
    @route("/item/{fundVersionId}/{outputId}/{outputVersion}/undefined/set")
    @put op setNotIdentifiedOutputItem(
        /** fund version id */
        @path fundVersionId: integer;
        /** output id */
        @path outputId: integer;
        /** output version */
        @path outputVersion: integer;
        /** output item type id */
        @query outputItemTypeId: integer;
        /** output item spec id */
        @query outputItemSpecId?: integer;
        /** output item object id */
        @query outputItemObjectId?: integer;
    ): ItemRes;

    /**
     * Reset the attribute setting to "Undefined"
     */
    @route("/item/{fundVersionId}/{outputVersion}/{outputItemObjectId}/undefined/unset")
    @put op unsetNotIdentifiedOutputItem(
        /** fund version id */
        @path fundVersionId: integer;
        /** output version */
        @path outputVersion: integer;
        /** output item object id */
        @path outputItemObjectId: integer;
    ): ItemRes;

    /**
     * Import a CSV file; a new entry will be created with the file's contents
     */
    @route("/item/{fundVersionId}/csv/import")
    @post op outputItemCsvImport(
        /** fund version id */
        @path fundVersionId: integer;
        /** multipart body */
        @multipartBody body: OutputItemCsvImportData;
        /** header */
        @header contentType: "multipart/form-data";
    ): ItemRes;

    /**
     * Downloading a CSV file from an attribute value
     */
    @route("/item/{fundVersionId}/csv/export")
    @get op outputItemCsvExport(
        /** fund version id */
        @path fundVersionId: integer;
        /** desc item object id */
        @query descItemObjectId: integer;
    ): {
        @Http.statusCode statusCode: 200;
        @header contentType: "text/csv";
        @body data: bytes;
    };

    /**
     * Getting data for the form
     */
    @route("/{outputId}/{fundVersionId}/form")
    @get op getOutputFormData(
        /** output id */
        @path outputId: integer;
        /** fund version id */
        @path fundVersionId: integer;
    ): FormData;

    /**
     * Switch between automatic and user-defined attribute type editing
     */
    @route("/{outputId}/{fundVersionId}/{itemTypeId}/switch")
    @post op switchOutputCalculating(
        /** output id */
        @path outputId: integer;
        /** fund version id */
        @path fundVersionId: integer;
        /** item type id */
        @path itemTypeId: integer;
    ): boolean | {
        /** Not found */
        @Http.statusCode statusCode: 404;
        @body data: BaseException;
    };
}

@tag("search")
namespace ElzaAPI.Search {
    /**
     * Searches for entity
     */
	@route("/search-ap")
	@post op entity(
        /** search parameters */
        @body body: SearchParams;
    ): ResultEntityRef | {
        /** Bad Request, query with incorrect parameters */
        @Http.statusCode statusCode: 400;
        @body data: BaseException;
    } | {
        /** Unauthorized */
        @Http.statusCode statusCode: 401;
        @body data: BaseException;
    } | {
        /** Forbidden */
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    };

    /**
     * Searches for array descriptions
     */
	@route("/search-arr")
	@post op archDesc(
        /** search parameters */
        @body body: SearchParams;
    ): ResultEntityRef | {
        /** Bad Request, query with incorrect parameters */
        @Http.statusCode statusCode: 400;
        @body data: BaseException;
    } | {
        /** Unauthorized */
        @Http.statusCode statusCode: 401;
        @body data: BaseException;
    } | {
        /** Forbidden */
        @Http.statusCode statusCode: 403;
        @body data: BaseException;
    };
}


/**
 * Processing tasks
 */
@tag("tasks")
@route("/tasks")
namespace ElzaAPI.Tasks {
    /**
     * Task status
     */
    enum Status {
        NEW, CANCELLED, FINISHED
    }

    /**
     * Entity type for task
     */
    enum EntityType {
        AP, AP_REV
    }

    /**
     * Task detail
     */
    model ViewDetail {
        taskId: integer;
        created: offsetDateTime;
        closed?: offsetDateTime;
        assigneeId: integer;
        assigneeName: string;
        creatorId?: integer;
        creatorName?: string;
        closedById?: integer;
        closedByName?: string;
        status: Status;
        taskTypeId: integer;
        taskTypeCode: string;
        taskTypeName: string;
        description: string;
        primaryEntityId: integer;
        primaryEntityName: string;
        primaryEntityType: EntityType;
    }

    /**
     * Task list for a given user
     */
    @route("/my")
    @get op getMyTasks(
    ): ViewDetail[];
}

@service(#{
    title: "ELZA API",
})
@info(#{
    version: "1.0.0",
})
@tagMetadata("admin", #{description: "Administrativní funkce"})
@tagMetadata("daos", #{description: "Digitální archivní objekty"})
@tagMetadata("externalsystems", #{description: "Externí systémy"})
@tagMetadata("aiprovider", #{description: "Externí AI systémy (poskytovatelé AI služeb)"})
@tagMetadata("funds", #{description: "Archivní soubory"})
@tagMetadata("descitems", #{description: "Záznamy v archivních fondech"})
@tagMetadata("accesspoints", #{description: "Přístupové body a archivní entity"})
@tagMetadata("io", #{description: "Vstup a výstup informací"})
@tagMetadata("aips", #{description: "aips"})
@tagMetadata("report", #{description: "Statistické zprávy"})
@tagMetadata("node", #{description: "Práce s úrovněmi a uzly"})
@tagMetadata("structure", #{description: "structure"})
@tagMetadata("search", #{description: "Vyhledávání"})
@tagMetadata("tasks", #{description: "Úkoly"})
@tagMetadata("publication", #{description: "Public API for the publication system"})
@tagMetadata("publication-internal", #{description: "Publication API used by the Elza UI"})
@tagMetadata("accesspoint-internal", #{description: "Access-point API used by the Elza UI"})
@tagMetadata("rules", #{description: "Pravidla popisu archiválií (typy atributů, datové typy, …)"})
@tagMetadata("output", #{description: "Výstupy archivních souborů"})
namespace ElzaAPI {
    // add models not used in operations
    union __Types {
        a: NodeUpdateItem,
        b: ItemTypeGroup,
        c: ExtEntityBinding
    }
    @route("/__types") @get
    op __typeRegistry(): __Types;
}
