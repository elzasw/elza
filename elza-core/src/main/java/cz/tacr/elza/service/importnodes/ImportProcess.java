package cz.tacr.elza.service.importnodes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.castor.core.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

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
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.service.ArrMoveLevelService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.importnodes.vo.DeepCallback;
import cz.tacr.elza.service.importnodes.vo.ImportParams;
import cz.tacr.elza.service.importnodes.vo.ImportSource;
import cz.tacr.elza.service.importnodes.vo.Node;
import cz.tacr.elza.service.importnodes.vo.NodeRegister;
import cz.tacr.elza.service.importnodes.vo.descitems.Item;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemCoordinates;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemDecimal;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemEnum;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemFileRef;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemFormattedText;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemInt;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemJsonTable;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemPacketRef;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemPartyRef;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemRecordRef;
import cz.tacr.elza.service.importnodes.vo.descitems.ItemString;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;

    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    private RegRecordRepository regRecordRepository;

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
    private PacketRepository packetRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private DmsService dmsService;

    @Autowired
    private ArrMoveLevelService arrMoveLevelService;

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
    private ArrMoveLevelService.AddLevelDirection selectedDirection;

    private List<ArrLevel> levels = new ArrayList<>();
    private List<ArrNodeRegister> nodeRegisters = new ArrayList<>();
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
                     final ArrMoveLevelService.AddLevelDirection selectedDirection) {
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
		Map<Integer, ArrPacket> packetsMapper = resolvePacketConflict();

        Stack<DeepData> stack = new Stack<>();
        while (source.hasNext()) {
            Node node = source.getNext(processDeepCallback(stack));

            DeepData deepData = stack.peek();

            ArrLevel level = arrangementService.createLevelSimple(change, deepData.getParentNode(), deepData.getPosition(), node.getUuid(), targetFundVersion.getFund());
            levels.add(level);

            processNodeRegisters(node, level.getNode());

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

                    ArrData data = createArrData(filesMapper, packetsMapper, item, descItem);

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
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    ruleService.conformityInfo(targetFundVersion.getFundVersionId(), nodeIds, NodeTypeOperation.CREATE_NODE, null,
                            null, null);
                }
            });
        }
        logger.info("Dokončení importu do AS: " + nodeIds.size() + " uzlů");

        eventNotificationService.publishEvent(new EventIdsInVersion(EventType.NODES_CHANGE, targetFundVersion.getFundVersionId(), targetNode.getNodeId()));
    }

    /**
     * Vytvoření dat z atributu.
     *
     * @param filesMapper   mapování souborů
     * @param packetsMapper mapovávé obalů
     * @param item          zdrojový item
     * @param descItem      vazební item   @return vytvořená data
     */
	private ArrData createArrData(final Map<String, ArrFile> filesMapper, final Map<Integer, ArrPacket> packetsMapper,
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
            ((ArrDataUnitid) data).setValue(((ItemUnitid) item).getValue());
        } else if (item instanceof ItemUnitdate) {
            data = new ArrDataUnitdate();
            String value = ((ItemUnitdate) item).getValue();
            data = UnitDateConvertor.convertToUnitDate(value, (ArrDataUnitdate) data);
            ArrCalendarType calendarType = calendarTypeMap.get(((ItemUnitdate) item).getCalendarTypeCode());
            value = ((ArrDataUnitdate) data).getValueFrom();
            if (value != null) {
                ((ArrDataUnitdate) data).setNormalizedFrom(CalendarConverter.toSeconds(CalendarType.valueOf(calendarType.getCode()), LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            } else {
                ((ArrDataUnitdate) data).setNormalizedFrom(Long.MIN_VALUE);
            }
            value = ((ArrDataUnitdate) data).getValueTo();
            if (value != null) {
                ((ArrDataUnitdate) data).setNormalizedTo(CalendarConverter.toSeconds(CalendarType.valueOf(calendarType.getCode()), LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            } else {
                ((ArrDataUnitdate) data).setNormalizedTo(Long.MAX_VALUE);
            }
            ((ArrDataUnitdate) data).setCalendarType(calendarType);
        } else if (item instanceof ItemJsonTable) {
            data = new ArrDataJsonTable();
            ((ArrDataJsonTable) data).setValue(((ItemJsonTable) item).getValue());
        } else if (item instanceof ItemCoordinates) {
            data = new ArrDataCoordinates();
            ((ArrDataCoordinates) data).setValue(parseGeometry(((ItemCoordinates) item).getGeometry()));
        } else if (item instanceof ItemFileRef) {
            data = new ArrDataFileRef();
            ArrFile file = fundFileRepository.findOne(((ItemFileRef) item).getFileId());
            ArrFile fileNew = filesMapper.get(file.getName());
            ((ArrDataFileRef) data).setFile(fileNew);
        } else if (item instanceof ItemPacketRef) {
            data = new ArrDataPacketRef();
            ArrPacket packet = packetRepository.findOne(((ItemPacketRef) item).getPacketId());
			ArrPacket packetNew = packetsMapper.get(packet.getPacketId());
            ((ArrDataPacketRef) data).setPacket(packetNew);
        } else if (item instanceof ItemPartyRef) {
            data = new ArrDataPartyRef();
            ((ArrDataPartyRef) data).setParty(partyRepository.getOne(((ItemPartyRef) item).getPartyId()));
        } else if (item instanceof ItemRecordRef) {
            data = new ArrDataRecordRef();
            ((ArrDataRecordRef) data).setRecord(regRecordRepository.getOne(((ItemRecordRef) item).getRecordId()));
        } else {
            //descItem.setUndefined(true);
            data = null;
        }
        return data;
    }

    /**
     * Zpracování rejstříkových hesel u JP.
     *
     * @param sourceNode zdrojová JP
     * @param node       cílová JP
     */
    private void processNodeRegisters(final Node sourceNode, final ArrNode node) {
        Collection<? extends NodeRegister> registers = sourceNode.getNodeRegisters();
        if (CollectionUtils.isNotEmpty(registers)) {
            for (NodeRegister register : registers) {
                ArrNodeRegister nodeRegister = new ArrNodeRegister();
                nodeRegister.setCreateChange(change);
                nodeRegister.setNode(node);
                nodeRegister.setRecord(regRecordRepository.getOne(register.getRecordId()));
                nodeRegisters.add(nodeRegister);
            }
        }
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
                            ArrLevel staticLevel = levelRepository.findNodeInRootTreeByNodeId(targetNode, targetFundVersion.getRootNode(), targetFundVersion.getLockChange());
                            int position = selectedDirection.equals(ArrMoveLevelService.AddLevelDirection.AFTER) ? staticLevel.getPosition() + 1 : staticLevel.getPosition();
                            levelsToShift = arrMoveLevelService.nodesToShift(staticLevel);
                            if (selectedDirection.equals(ArrMoveLevelService.AddLevelDirection.BEFORE)) {
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

	private Pair<Integer, String> createPacketKey(ArrPacket packet) {
		RulPacketType packetType = packet.getPacketType();
		Integer packetTypeId = (packetType != null) ? packetType.getPacketTypeId() : null;
		return new Pair<Integer, String>(packetTypeId, packet.getStorageNumber());
	}

    /**
     * Vyřešení konfliktů v obalech.
     *
     * @return výsledná mapa pro provazování
     */
	private Map<Integer, ArrPacket> resolvePacketConflict() {
		// get current packets
		Map<Pair<Integer, String>, ArrPacket> fundPacketsMapName = packetRepository
		        .findByFund(targetFundVersion.getFund(), Arrays.asList(ArrPacket.State.OPEN, ArrPacket.State.CLOSED))
		        .stream().collect(
		                Collectors.toMap(packet -> createPacketKey(packet), Function.identity())
		        );

		// Map PacketId to ArrPacket
		Map<Integer, ArrPacket> result = new HashMap<>();

		List<ArrPacket> sourcePackets = source.getPackets();
		for (ArrPacket sourcePacket : sourcePackets) {
			ArrPacket packet = null;
			// switch
			switch (params.getPacketConflictResolve()) {
			case USE_TARGET:
				Pair<Integer, String> srcPacketKey = createPacketKey(sourcePacket);
				packet = fundPacketsMapName.get(srcPacketKey);
				if (packet == null) {
					packet = copyPacketFromSource(sourcePacket, fundPacketsMapName);
				}
				break;
			case COPY_AND_RENAME:
				packet = copyPacketFromSource(sourcePacket, fundPacketsMapName);
				break;
			default:
				throw new SystemException("Neplatné vyřešení konfliktu: " + params.getFileConflictResolve(),
				        BaseCode.INVALID_STATE);
			}
			Pair<Integer, String> packetKey = createPacketKey(packet);
			fundPacketsMapName.put(packetKey, packet);
			result.put(sourcePacket.getPacketId(), packet);
		}

		return result;
    }

    /**
	 * Zkopírovat obaly ze zdroje do AS.
	 *
	 * @param sourcePacket
	 * @param fundPacketsMapName
	 *            existující typ/number obaly v AS
	 */
	private ArrPacket copyPacketFromSource(ArrPacket sourcePacket,
	        final Map<Pair<Integer, String>, ArrPacket> fundPacketsMapName) {
        ArrPacket packet = new ArrPacket();
        packet.setState(ArrPacket.State.OPEN);
		packet.setStorageNumber(preparePacketName(sourcePacket, fundPacketsMapName));
        packet.setFund(targetFundVersion.getFund());
		packet.setPacketType(sourcePacket.getPacketType());
		packet = packetRepository.save(packet);
		//TODO: remove from here
        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.PACKETS_CHANGE, targetFundVersion.getFund().getFundId()));
		return packet;
    }

	private String preparePacketName(ArrPacket sourcePacket, Map<Pair<Integer, String>, ArrPacket> packets) {
		String tmpName = sourcePacket.getStorageNumber();
		// prepare packet type id
		Integer packetTypeId = null;
		RulPacketType packetType = sourcePacket.getPacketType();
		if (packetType != null) {
			packetTypeId = packetType.getPacketTypeId();
		}

		boolean conflict = false;
		int i = 0;
		do {
			if (conflict) {
				i++;
				conflict = false;
				tmpName = includeNumber(tmpName, i);
			}
			Pair<Integer, String> packetKey = new Pair<>(packetTypeId, tmpName);
			if (packets.containsKey(packetKey)) {
				conflict = true;
				break;
			}
		} while (conflict);
		return tmpName;
	}

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

		boolean conflict = false;
		int i = 0;
		do {
			if (conflict) {
				i++;
				conflict = false;
				tmpName = includeNumber(tmpName, i);
			}
			if (currentFiles.containsKey(tmpName)) {
				conflict = true;
				break;
			}
		} while (conflict);
		return tmpName;
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

            nodeRegisterRepository.save(nodeRegisters);
            nodeRegisterRepository.flush();

            dataRepository.save(dataList);
            dataRepository.flush();

            descItemRepository.save(descItems);
            descItemRepository.flush();

            levels = new ArrayList<>();
            nodeRegisters = new ArrayList<>();
            descItems = new ArrayList<>();
            dataList = new ArrayList<>();
            needFlush = false;
            logger.debug("Import: uložení dat do DB: konec");
        }
    }

    /**
     * Parsování geometry objektu.
     *
     * @param geometry vstupní data
     * @return převedené geometry
     */
    private Geometry parseGeometry(final String geometry) {
        try {
            return new WKTReader().read(geometry);
        } catch (ParseException e) {
            throw new SystemException("Problém s převodem geometry", e);
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

    /**
     * Pomocná třída pro pár v obalech.
     */
    private class Pair<K, V> {

        private K key;

        private V value;

        public Pair(final K key, final V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
			Pair<?, ?> pair = (Pair<?, ?>) o;
            return Objects.equals(key, pair.key) &&
                    Objects.equals(value, pair.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

}
