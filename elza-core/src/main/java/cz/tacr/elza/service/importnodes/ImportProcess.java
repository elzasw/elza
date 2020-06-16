package cz.tacr.elza.service.importnodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.castor.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Geometry;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.StructObjValueService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.importnodes.vo.DeepCallback;
import cz.tacr.elza.service.importnodes.vo.ImportParams;
import cz.tacr.elza.service.importnodes.vo.ImportSource;
import cz.tacr.elza.service.importnodes.vo.Node;
import cz.tacr.elza.service.importnodes.vo.descitems.Item;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemCoordinates;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemDecimal;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemEnum;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemFileRef;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemFormattedText;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemInt;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemJsonTable;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemPartyRef;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemRecordRef;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemString;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemStructureRef;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemText;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemUnitdate;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemUnitid;

/**
 * Obsluha importního procesu zdroje do AS.
 *
 * @since 19.07.2017
 */
@Component
@Scope("prototype")
public class ImportProcess {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ImportProcess.class);

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    @Autowired
    private StructObjValueService structObjService;

    @Autowired
    private StructuredObjectRepository structureDataRepository;

    @Autowired
    private StructuredItemRepository structItemRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private DmsService dmsService;

    @Autowired
    private FundLevelService arrMoveLevelService;

    /**
     * Zdroj dat pro import.
     */
    private ImportSource source;

    /**
     * Parametry importu.
     */
    private ImportParams params;

    /**
     * Cílová verze AS.
     */
    private ArrFundVersion targetFundVersion;

    /**
     * Cílový uzel importu.
     */
    private ArrNode targetNode;

    /**
     * Rodič cílového uzlu importu.
     */
    private ArrNode targetParentNode;

    /**
     * Směr zakládání nodů.
     */
    private FundLevelService.AddLevelDirection selectedDirection;

    private List<ArrLevel> levels = new ArrayList<>();
    private List<ArrDescItem> descItems = new ArrayList<>();
    private List<ArrData> dataList = new ArrayList<>();
    private List<Integer> nodeIds = new ArrayList<>();

    /**
     * Uzly, které je potřeba posunout.
     */
    private List<ArrLevel> levelsToShift;

    // konstanty pro uložení při překročení počtu
    private final int LEVEL_LIMIT = 300;
    private final int DESC_ITEM_LIMIT = 500;
    private final int ARR_DATA_LIMIT = 800;

    private boolean needFlush = false;

    private Map<String, RulItemType> itemTypeMap;
    private Map<String, RulItemSpec> itemSpecMap;
    private Map<String, ArrCalendarType> calendarTypeMap;
    private ArrChange change;

    public ImportProcess() {

    }

    /**
     * Inicializace importního procesu.
     *  @param source           zdroj dat importu
     * @param params            parametry importu
     * @param targetFundVersion cílová verze AS
     * @param targetNode        cílový uzel importu
     * @param targetParentNode  rodič cílového uzlu importu
     * @param selectedDirection směr zakládání
     */
    public void init(final ImportSource source,
                     final ImportParams params,
                     final ArrFundVersion targetFundVersion,
                     final ArrNode targetNode,
                     final ArrNode targetParentNode,
                     final FundLevelService.AddLevelDirection selectedDirection) {
        logger.info("Inicializace importu do AS");
        this.source = source;
        this.params = params;
        this.targetFundVersion = targetFundVersion;
        this.targetNode = targetNode;
        this.targetParentNode = targetParentNode;
        this.selectedDirection = selectedDirection;

        itemTypeMap = itemTypeRepository.findAll().stream().collect(Collectors.toMap(RulItemType::getCode, Function.identity()));
        itemSpecMap = itemSpecRepository.findAll().stream().collect(Collectors.toMap(RulItemSpec::getCode, Function.identity()));
        calendarTypeMap = calendarTypeRepository.findAll().stream().collect(Collectors.toMap(ArrCalendarType::getCode, Function.identity()));
        change = arrangementService.createChange(ArrChange.Type.IMPORT);
    }

    /**
     * Spuštění importu.
     */
    public void run() {
        logger.info("Zahájení importu do AS");

        Map<String, ArrFile> filesMapper = resolveFileConflict();
		Map<Integer, ArrStructuredObject> structureDataMapper = prepareStructObjs();

        Stack<DeepData> stack = new Stack<>();
        while (source.hasNext()) {
            Node node = source.getNext(processDeepCallback(stack));

            DeepData deepData = stack.peek();

            ArrLevel level = arrangementService.createLevelSimple(change, deepData.getParentNode(), deepData.getPosition(), node.getUuid(), targetFundVersion.getFund());
            levels.add(level);

            Collection<? extends Item> items = node.getItems();
            if (CollectionUtils.isNotEmpty(items)) {
                Map<String, Integer> descItemPositionMap = new HashMap<>();
                for (Item item : items) {
                    ArrDescItem descItem = new ArrDescItem();
                    //descItem.setUndefined(false);

                    Integer position = descItemPositionMap.merge(item.getTypeCode(), 1, (a, b) -> a + b);

                    RulItemType itemType = itemTypeMap.get(item.getTypeCode());
                    descItem.setItemType(itemType);
                    descItem.setItemSpec(item.getSpecCode() == null ? null : itemSpecMap.get(item.getSpecCode()));
                    descItem.setCreateChange(change);
                    descItem.setPosition(position);
                    descItem.setNode(level.getNode());
                    descItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());
                    descItems.add(descItem);

                    ArrData data = createArrData(filesMapper, structureDataMapper, item, descItem);

                    if (data != null) {
                        descItem.setData(data);
                        data.setDataType(itemType.getDataType());
                        dataList.add(data);
                    }
                }
            }

            deepData.setPrevNode(level.getNode());

            flushData(false);
        }

        flushData(true);

        // posunutí potřebných levelů (pokud se zakládá před nebo za
        if (levelsToShift != null && levelsToShift.size() > 0) {
            DeepData data = null;
            while (!stack.isEmpty()) {
                data = stack.pop();
            }
            if (data != null) {
                arrMoveLevelService.shiftNodes(levelsToShift, change, data.position + 1);
            }
        }

        levelTreeCacheService.invalidateFundVersion(targetFundVersion);

        if (nodeIds.size() > 0) {
            nodeCacheService.syncCache();
			ruleService.conformityInfo(targetFundVersion.getFundVersionId(), nodeIds, NodeTypeOperation.CREATE_NODE,
			        null, null, null);
        }
        logger.info("Dokončení importu do AS: " + nodeIds.size() + " uzlů");

        eventNotificationService.publishEvent(new EventIdsInVersion(EventType.NODES_CHANGE, targetFundVersion.getFundVersionId(), targetNode.getNodeId()));
    }

    /**
     * Vytvoření dat z atributu.
     *
     * @param filesMapper   mapování souborů
     * @param structureDataMapper mapováví strukt.
     * @param item          zdrojový item
     * @param descItem      vazební item   @return vytvořená data
     */
    private ArrData createArrData(final Map<String, ArrFile> filesMapper,
                                  final Map<Integer, ArrStructuredObject> structureDataMapper,
	        final Item item, final ArrDescItem descItem) {
        ArrData data;
        if (item instanceof ItemInt) {
            data = new ArrDataInteger();
            ((ArrDataInteger) data).setValue(((ItemInt) item).getValue());
        } else if (item instanceof ItemEnum) {
            data = new ArrDataNull();
        } else if (item instanceof ItemText) {
            data = new ArrDataText();
            ((ArrDataText) data).setValue(((ItemText) item).getValue());
        } else if (item instanceof ItemFormattedText) {
            data = new ArrDataText();
            ((ArrDataText) data).setValue(((ItemFormattedText) item).getValue());
        } else if (item instanceof ItemString) {
            data = new ArrDataString();
            ((ArrDataString) data).setValue(((ItemString) item).getValue());
        } else if (item instanceof ItemDecimal) {
            data = new ArrDataDecimal();
            ((ArrDataDecimal) data).setValue(((ItemDecimal) item).getValue());
        } else if (item instanceof ItemUnitid) {
            data = new ArrDataUnitid();
            ((ArrDataUnitid) data).setUnitId(((ItemUnitid) item).getValue());
        } else if (item instanceof ItemUnitdate) {
            ArrCalendarType calendarType = calendarTypeMap.get(((ItemUnitdate) item).getCalendarTypeCode());
            CalendarType calType = CalendarType.valueOf(calendarType.getCode());
            String value = ((ItemUnitdate) item).getValue();
            data = ArrDataUnitdate.valueOf(calType, value);
        } else if (item instanceof ItemJsonTable) {
            data = new ArrDataJsonTable();
            ((ArrDataJsonTable) data).setValue(((ItemJsonTable) item).getValue());
        } else if (item instanceof ItemCoordinates) {
            data = new ArrDataCoordinates();
            Geometry geo = ((ItemCoordinates) item).getGeometry();
            // clone value
            Geometry geoClone = (Geometry) geo.clone();
            ((ArrDataCoordinates) data).setValue(geoClone);
        } else if (item instanceof ItemFileRef) {
            data = new ArrDataFileRef();
            ArrFile file = fundFileRepository.findOne(((ItemFileRef) item).getFileId());
            ArrFile fileNew = filesMapper.get(file.getName());
            ((ArrDataFileRef) data).setFile(fileNew);
        } else if (item instanceof ItemStructureRef) {
            data = new ArrDataStructureRef();
            ArrStructuredObject structureData = structureDataRepository
                    .findOne(((ItemStructureRef) item).getStructureDataId());
            ArrStructuredObject structureDataNew = structureDataMapper.get(structureData.getStructuredObjectId());

            // mapping should exists -> if mapping not found then whole item should not be imported? 
            Validate.notNull(structureDataNew);

            ((ArrDataStructureRef) data).setStructuredObject(structureDataNew);
        } else if (item instanceof ItemPartyRef) {
            data = new ArrDataPartyRef();
            ((ArrDataPartyRef) data).setParty(partyRepository.getOne(((ItemPartyRef) item).getPartyId()));
        } else if (item instanceof ItemRecordRef) {
            data = new ArrDataRecordRef();
            ((ArrDataRecordRef) data).setRecord(apAccessPointRepository.getOne(((ItemRecordRef) item).getRecordId()));
        } else {
            data = null;
        }
        return data;
    }

    /**
     * Zpracování posunu ve stromu.
     *
     * @param stack stack pro úrovně ve stromu
     * @return callback
     */
    private DeepCallback processDeepCallback(final Stack<DeepData> stack) {
        return (deep) -> {
            switch (deep) {
                case UP:
                    stack.pop();
                    stack.peek().incPosition();
                    break;
                case DOWN:
                    stack.push(new DeepData(1, stack.peek().getPrevNode()));
                    break;
                case NONE:
                    stack.peek().incPosition();
                    break;
                case RESET: {

                    flushData(true);

                    if (levelsToShift != null && levelsToShift.size() > 0) {
                        DeepData data = null;
                        while (!stack.isEmpty()) {
                            data = stack.pop();
                        }
                        if (data != null) {
                            switch (selectedDirection) {
                                case BEFORE: {
                                    // není třeba měnit
                                    break;
                                }
                                case AFTER:
                                    targetNode = data.getPrevNode();
                                    break;
                                default: {
                                    throw new SystemException("Neplatný směr založení levelu: " + selectedDirection, BaseCode.INVALID_STATE);
                                }
                            }
                            arrMoveLevelService.shiftNodes(levelsToShift, change, data.position + 1);
                        }
                    }

                    switch (selectedDirection) {
                        case CHILD: {
                            Integer position = levelRepository.findMaxPositionUnderParent(targetNode);
                            stack.push(new DeepData(position == null ? 1 : position + 1, targetNode));
                                break;
                        }

                        case AFTER:
                        case BEFORE: {
                            ArrLevel staticLevel = levelRepository.findByNode(targetNode, targetFundVersion.getLockChange());
                            int position = selectedDirection.equals(FundLevelService.AddLevelDirection.AFTER) ? staticLevel.getPosition() + 1 : staticLevel.getPosition();
                            levelsToShift = arrMoveLevelService.nodesToShift(staticLevel);
                            if (selectedDirection.equals(FundLevelService.AddLevelDirection.BEFORE)) {
                                levelsToShift.add(0, staticLevel);
                            }
                            Assert.notNull(targetParentNode, "Musí být vyplněn rodič uzlu");
                            stack.push(new DeepData(position, targetParentNode));
                            break;
                        }

                        default: {
                            throw new SystemException("Neplatný směr založení levelu: " + selectedDirection, BaseCode.INVALID_STATE);
                        }
                    }

                    break;
                }
            }
        };
    }

    /**
     * Vyřešení konfliktů v souborech.
     *
     * @return výsledná mapa pro provazování
     */
    private Map<String, ArrFile> resolveFileConflict() {
		List<ArrFile> sourceFiles = source.getFiles();
        Map<String, ArrFile> fundFilesMapName = fundFileRepository.findByFund(targetFundVersion.getFund()).stream().collect(Collectors.toMap(ArrFile::getName, Function.identity()));
		Map<String, ArrFile> result = new HashMap<>();

		for (ArrFile sourceFile : sourceFiles) {
			String sourceFileName = sourceFile.getName();
			ArrFile arrFile;
            switch (params.getFileConflictResolve()) {
            case USE_TARGET:
            	arrFile = fundFilesMapName.get(sourceFileName);
            	if(arrFile==null) {
					// file not exists -> copy new
					arrFile = copyFileFromSource(sourceFile, fundFilesMapName);
            	}
                break;
            case COPY_AND_RENAME:
            	arrFile = copyFileFromSource(sourceFile, fundFilesMapName);
                break;
			default:
				throw new SystemException("Neplatné vyřešení konfliktu: " + params.getFileConflictResolve(),
				        BaseCode.INVALID_STATE);
            }
			// append new file to map
			fundFilesMapName.put(arrFile.getName(), arrFile);
			// append new file to result
			result.put(sourceFileName, arrFile);
        }
		return result;
    }

    /**
     * Vyřešení konfliktů v obalech.
     *
     * @return výsledná mapa pro provazování
     */
	private Map<Integer, ArrStructuredObject> prepareStructObjs() {
		// Map PacketId to ArrPacket
		Map<Integer, ArrStructuredObject> result = new HashMap<>();

        List<ArrStructuredObject> sourcePackets = source.getStructuredList();
        for (ArrStructuredObject sourcePacket : sourcePackets) {
            ArrStructuredObject structuredObject = null;
			// switch
            switch (params.getStructuredConflictResolve()) {
            /*
               This option is not anymore supported, structured object can be only copied
            case USE_TARGET:            	
                structuredObject = fundPacketsMapName.get(srcPacketKey);
                if (structuredObject == null) {
                    throw new IllegalStateException("Not implemented");
                    // TODO:
                    //structuredObject = copyStrcutureDataFromSource(sourcePacket, fundPacketsMapName);
            	}
            	break;
            	*/
            case COPY_AND_RENAME:
                //String srcPacketKey = sourcePacket.getValue();
                structuredObject = copyStructObjFromSource(sourcePacket);
                break;
            case USE_TARGET:
			default:
				throw new SystemException("Neplatné vyřešení konfliktu: " + params.getFileConflictResolve(),
				        BaseCode.INVALID_STATE);
            }
            // store result
            result.put(sourcePacket.getStructuredObjectId(), structuredObject);
		}

        return result;
    }

    /**
     * Zkopíruje vybraný obal do AS.
     *
     * @param sourceObj
     *            zdrojový objekt
     */
    private ArrStructuredObject copyStructObjFromSource(ArrStructuredObject sourceObj) {
        // prepare new obj
        ArrStructuredObject so = new ArrStructuredObject();
        so.setAssignable(Boolean.TRUE);
        so.setState(sourceObj.getState());
        so.setCreateChange(this.change);
        so.setErrorDescription(sourceObj.getErrorDescription());
        so.setStructuredType(sourceObj.getStructuredType());
        so.setValue(sourceObj.getValue());
        so.setComplement(sourceObj.getComplement());
        so.setFund(targetFundVersion.getFund());
        so = structureDataRepository.save(so);

        Validate.notNull(so.getStructuredObjectId());

        // preapare items and data
        List<ArrStructuredItem> srcItems = structItemRepository
                .findByStructuredObjectAndDeleteChangeIsNullFetchData(sourceObj);
        for (ArrStructuredItem srcItem : srcItems) {
            // make data copy
            ArrData srcData = srcItem.getData();
            ArrData trgData = null;
            if (srcData != null) {
                trgData = ArrData.makeCopyWithoutId(srcData);
                dataRepository.save(trgData);

                Validate.notNull(trgData.getDataId());
            }

            // prepare struct.obj item
            ArrStructuredItem trgItem = new ArrStructuredItem();
            trgItem.setCreateChange(this.change);
            trgItem.setData(trgData);
            trgItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());
            trgItem.setItemSpec(srcItem.getItemSpec());
            trgItem.setItemType(srcItem.getItemType());
            trgItem.setPosition(srcItem.getPosition());
            trgItem.setStructuredObject(so);

            structItemRepository.save(trgItem);
        }

        // generate
        structObjService.addToValidate(so);

        return so;
    }

    /**
     * Zkopírovat obaly ze zdroje do AS.
     *
     * @param sourceStructureds
     *            zdorové obaly [typ/value -> zdrojový obal]
     * @param structureDataMapper
     *            převodní mapa [typ/value -> obal v AS]
     * @param structuredSource
     *            typ/number obalu
     * @param existsStructureds
     *            existující typ/number obaly v AS
     */
    /*private void copyStrcutureDataFromSource(final Map<Pair<String, String>, Structured> sourceStructureds,
                                             final Map<Pair<String, String>, ArrStructureData> structureDataMapper,
                                             final Pair<String, String> structuredSource,
                                             final Set<Pair<String, String>> existsStructureds) {
        // TODO slapa: co tady?
        ArrPacket packet = new ArrPacket();
        packet.setState(ArrPacket.State.OPEN);
		packet.setStorageNumber(preparePacketName(sourcePacket, fundPacketsMapName));
        packet.setFund(targetFundVersion.getFund());
		packet.setPacketType(sourcePacket.getPacketType());
		packet = packetRepository.save(packet);
		//TODO: remove from here
        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.PACKETS_CHANGE, targetFundVersion.getFund().getFundId()));
		return packet;
    }*/

	/**
	 * Zkopírování souboru ze zdroje do AS.
	 *
	 * @param sourceFiles
	 *            zdrojové soubory [název -> zdrojovýSoubor]
	 * @param filesMapper
	 *            převodní mapa [název -> soubor v AS]
	 * @param fileNameSource
	 *            název kopírovaného souboru
	 * @param existsFileNames
	 *            existující názvy souborů v AS
	 * @return
	 */
	private ArrFile copyFileFromSource(ArrFile sourceFile,
	        Map<String, ArrFile> currentFiles) {
        ArrFile file = new ArrFile();
        file.setFileName(sourceFile.getFileName());
        file.setFileSize(sourceFile.getFileSize());
        file.setMimeType(sourceFile.getMimeType());
        file.setPagesCount(sourceFile.getPagesCount());
		file.setName(renameConflictName(sourceFile.getName(), currentFiles));
        file.setFund(targetFundVersion.getFund());
        try {
			dmsService.createFile(file, dmsService.downloadFile(sourceFile));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
		return file;
    }

	public static final String REG_NAME = "(.*\\()([0-9]+)(\\))";

	private static String includeNumber(final String name, final int i) {
		if (name.matches(REG_NAME)) {
			int tmpI = i;
			Matcher m = Pattern.compile(REG_NAME).matcher(name);
			if (m.find()) {
				tmpI = Integer.valueOf(m.group(2)) + 1;
			}
			return name.replaceAll(REG_NAME, "$1" + tmpI + "$3");
		}
		return name + "(" + i + ")";
	}

	public static String renameConflictName(final String name, final Map<String, ArrFile> currentFiles) {
		String tmpName = name;

		int i = 0;
		do {
            if (!currentFiles.containsKey(tmpName)) {
                return tmpName;
			}
            i++;
            tmpName = includeNumber(tmpName, i);
        } while (true);
	}

    /**
     * Pokud je potřeba, provede uložení dat do DB.
     *
     * @param force provede vždy
     */
    private void flushData(final boolean force) {

        if (levels.size() >= LEVEL_LIMIT || descItems.size() >= DESC_ITEM_LIMIT || dataList.size() >= ARR_DATA_LIMIT) {
            needFlush = true;
        }

        if (needFlush || force) {
            logger.debug("Import: uložení dat do DB: start");
            nodeIds.addAll(levels.stream().map(level -> level.getNode().getNodeId()).collect(Collectors.toList()));

            levelRepository.save(levels);
            levelRepository.flush();

            dataRepository.save(dataList);
            dataRepository.flush();

            descItemRepository.save(descItems);
            descItemRepository.flush();

            levels = new ArrayList<>();
            descItems = new ArrayList<>();
            dataList = new ArrayList<>();
            needFlush = false;
            logger.debug("Import: uložení dat do DB: konec");
        }
    }

    /**
     * Pomocná data pro vytváření nové struktury.
     */
    private class DeepData {

        /**
         * Pozice v úrovni.
         */
        private int position;

        /**
         * Předchozí zpracovaný uzel.
         */
        private ArrNode prevNode;

        /**
         * Rodičovský uzel úrovně.
         */
        private ArrNode parentNode;

        public DeepData(final int position, final ArrNode parentNode) {
            this.position = position;
            this.parentNode = parentNode;
        }

        public int getPosition() {
            return position;
        }

        public void incPosition() {
            position++;
        }

        public ArrNode getParentNode() {
            return parentNode;
        }

        public ArrNode getPrevNode() {
            return prevNode;
        }

        public void setPrevNode(final ArrNode prevNode) {
            this.prevNode = prevNode;
        }
    }
}
