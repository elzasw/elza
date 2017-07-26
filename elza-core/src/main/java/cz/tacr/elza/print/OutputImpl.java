package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemFile;
import cz.tacr.elza.print.item.ItemPacketRef;
import cz.tacr.elza.print.item.ItemPartyRef;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.party.Dynasty;
import cz.tacr.elza.print.party.Event;
import cz.tacr.elza.print.party.Party;
import cz.tacr.elza.print.party.PartyGroup;
import cz.tacr.elza.print.party.PartyInitHelper;
import cz.tacr.elza.print.party.Person;
import cz.tacr.elza.print.party.Relation;
import cz.tacr.elza.print.party.RelationToType;
import cz.tacr.elza.print.party.RelationTo;
import cz.tacr.elza.print.party.RelationType;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.output.OutputFactoryService;
import cz.tacr.elza.utils.AppContext;
import cz.tacr.elza.utils.PartyType;
import cz.tacr.elza.utils.ProxyUtils;

/**
 * Základní objekt pro generování výstupu, při tisku se vytváří 1 instance.
 *
 */
public class OutputImpl implements Output
{

    public static final int MAX_CACHED_NODES = 1000; // maximální počet nodů v cache

    private final int outputId; // ID pro vazbu do DB na entitu arr_output

    private OutputFactoryService outputFactoryService = AppContext.getBean(OutputFactoryService.class);

    private String internal_code;
    private String name;
    private String type;
    private String typeCode;
    private Fund fund;

    private int page = 0;

    /**
     * Vnitřní iterátor - cache.
     */
    private IteratorNodes iteratorNodes = null;

    // seznam všech atributů outputu
    private List<Item> items = new ArrayList<>();

    // seznam všech node outputu (přímo přiřazené + jejich potomci + nadřízení až do root);
    // mapa má jako klíč ID Nodu odpovídající ArrNode.arrNodeId
    private Map<Integer, NodeId> nodeIdsMap = new HashMap<>();

    // seznam rejstříkových hesel podle  typu
    private Map<String, FilteredRecords> filteredRecords = new HashMap <>();

    /**
     * Record type cache
     */
    private Map<Integer, RecordType> recordTypes = new HashMap<>();
    
    /**
     * Cache for party
     */
    private Map<Integer, Party> partyCache = new HashMap<>();
    
    /**
     * Cache for records
     */
    private Map<Integer, Record> recordCache = new HashMap<>();
    
    /**
     * Cache ro relation types
     */
    private Map<Integer, RelationType> partyRelationTypeCache = new HashMap<>();

    private Set<Integer> directNodeIds = new HashSet<>();

    Map<Integer, ItemType> itemTypeMap = new HashMap<>();
    Map<Integer, ItemSpec> itemSpecMap = new HashMap<>();
    
    private Map<Integer, RelationToType> relToTypeCache = new HashMap<>();
    
    /**
     * Vytvoření instance s povinnými údaji
     *
     * @param output arr_output s definicí zpracovávaného výstupu
     */
    public OutputImpl(final ArrOutput output) {
        this.outputId = output.getOutputId();
    }

    /**
     * Přidá {@link NodeId} do výstupu.
     */
    public NodeId addNodeId(final NodeId nodeId) {
        Assert.notNull(nodeId);

        NodeId nodeIdOrig = nodeIdsMap.get(nodeId.getArrNodeId());
        if (nodeIdOrig == null) {
            nodeIdsMap.put(nodeId.getArrNodeId(), nodeId);
            return nodeId;
        } else {
            return nodeIdOrig;
        }
    }

    public Node getNode(final NodeId nodeId) {
        if (nodeIdsMap.size() > 0) {
            if (iteratorNodes == null) {
                iteratorNodes = getNodesBFS();
            }
            return iteratorNodes.moveTo(nodeId);
        } else {
            Map<Integer, Node> nodeMap = outputFactoryService.loadNodes(this, Arrays.asList(nodeId));
            return nodeMap.get(nodeId.getArrNodeId());
        }
    }

    public NodeId getNodeId(final Integer nodeIdentifier) {
        return nodeIdsMap.get(nodeIdentifier);
    }

    public void linkNodeIds(final Integer parentNodeIdentifier, final Integer childNodeIdentifier) {
        final NodeId nodeIdParent = getNodeId(parentNodeIdentifier);
        final NodeId nodeIdChild = getNodeId(childNodeIdentifier);

        nodeIdChild.setParentNodeId(parentNodeIdentifier);
        nodeIdParent.getChildren().add(nodeIdChild);
    }

    /**
     * Externí počítadlo stránek pro Jasper.
     * Obchází chybu, kdy jasper nezvládá interně počítat stránky pokud detail přeteče na více stránek.
     *
     * @param increment má se při volání provést increment
     * @return aktuální hodnota (po případné inkrementaci)
     */
    public Integer getPage(final boolean increment) {
        if (increment) {
            page += 1;
        }
        return page;
    }

    /**
     * @return sečtená hodnota počtu stránek příloh pdf připojených k nodům v output.
     */
    public Integer getAttachedPages() {
        return getItemFilePdfs().stream()
                .mapToInt(ItemFile::getPagesCount)
                .sum();
    }

    /**
     * @return seznam PDF příloh připojených k nodům v output.
     */
    public List<ItemFile> getAttachements() {
        return getItemFilePdfs();
    }

    private List<ItemFile> getItemFilePdfs() {
        IteratorNodes iterator = getNodesBFS();

        List<ItemFile> result = new ArrayList<>();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            List<Item> items = node.getItems();
            for (Item item : items) {
                if (item instanceof ItemFile) {
                    ItemFile itemFile = (ItemFile) item;
                    if (itemFile.getMimeType().equals(DmsService.MIME_TYPE_APPLICATION_PDF)) {
                        result.add(itemFile);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Metoda sahá pomocí service do DB a zafiltruje seznam přímo přiřazených nodes.
     *
     * @return seznam nodes, které jsou přímo přiřazené outputu (arr_node_output), řazeno dle pořadí ve stromu
     */
    public List<NodeId> getDirectNodes() {
        IteratorNodes iterator = getNodesDFS();
        List<NodeId> result = new ArrayList<>();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            if (directNodeIds.contains(node.getArrNodeId())) {
                result.add(iterator.getActualNodeId());
            }
        }
        return result;
    }

    /** Přidá id uzlu přímo přiřazeného k výstupu. */
    public void addDirectNodeIdentifier(final Integer nodeId) {
        directNodeIds.add(nodeId);
    }

    @Override
    public List<Item> getItems(@NotNull final Collection<String> codes) {
        Assert.notNull(codes);
        return items.stream()
                .filter(item -> codes.contains(item.getType().getCode()))
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }

	@Override
	public List<Party> getParties(Collection<String> codes) {
        Assert.notNull(codes);
        List<Party> parties = new ArrayList<>();
        // set to check if party was added
        Set<Integer> exportedParties = new HashSet<>();
        items.forEach(item-> {
        	// check item code
        	if(codes.contains(item.getType().getCode())) {
        		
        		ItemPartyRef partyRef = (ItemPartyRef)item;
        		Party party = partyRef.getParty();
        		
        		// check if party already added and add it
        		if(!exportedParties.contains(party.getPartyId())) {
        			exportedParties.add(party.getPartyId());
        			parties.add(party);
        		}
        	}
        });
        
        return parties;
	}

	@Override
    public Item getSingleItem(final String itemTypeCode) {
        Item found = null;
        for(Item item: items)
        {
            if(itemTypeCode.equals(item.getType().getCode())) {
                // Check if item already found
                if(found!=null) {
                    throw new IllegalStateException("Multiple items with same code exists: "+itemTypeCode);
                }
                found = item;
            }
        }
        return found;
    }

    @Override
    public String getSingleItemValue(final String itemTypeCode) {
        Item found = getSingleItem(itemTypeCode);
        if(found!=null) {
            return found.getSerializedValue();
        } else {
            return null;
        }
    }

    @Override
    public List<Item> getAllItems(@NotNull final Collection<String> codes) {
        Assert.notNull(codes);
        return items.stream()
                .filter(item -> !codes.contains(item.getType().getCode()))
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }

    /**
     * @return distinct seznam Packet navázaný přes nodes
     */
    public List<Packet> getPacketItemsDistinct() {
        IteratorNodes iterator = getNodesBFS();
        Set<Packet> resultsSet = new HashSet<>();

        while (iterator.hasNext()) {
            Node node = iterator.next();
            List<Item> items = node.getItems();
            for (Item item : items) {
                if (item instanceof ItemPacketRef) {
                    resultsSet.add(item.getValue(Packet.class));
                }
            }
        }

        List<Packet> results = new ArrayList<>(resultsSet);
        results.sort(Packet::compareTo);
        return results;
    }

    /**
     * Getter položky items
     *
     * @return seznam items
     */
    @Override
    public List<Item> getItems() {
        return items;
    }

    /**
     *  @return plochý seznam Nodů seřazený dle depth, parent, position
     */
    @Override
    public IteratorNodes getNodesBFS() {
        List<NodeId> nodeIds = nodeIdsMap.values().stream()
                .sorted((o1, o2) -> new CompareToBuilder()
                        .append(o1.getDepth(), o2.getDepth())  // nejprve nejvyšší nody
                        .append(o1.getParent(), o2.getParent()) // pak sezkupit dle parenta
                        .append(o1.getPosition(), o2.getPosition()) // pak dle pořadí
                        .toComparison())
                .collect(Collectors.toList());
        return new IteratorNodes(this, nodeIds, outputFactoryService, MAX_CACHED_NODES);
    }

    /**
     * @param parent výchozí parent
     * @return plochý seznam Nodů seřazený dle prohledávání stromu nodů od root node do hloubky, vč. předaných parentů
     */
    public List<NodeId> getNodesChildsModel(final NodeId parent) {
        List<NodeId> result = new ArrayList<>();
        result.add(parent); // zařadit vlastní parent

        for (NodeId child : parent.getChildren()) {
            result.addAll(getNodesChildsModel(child));
        }

        return result;
    }

    /**
     * Jako výchozí bod vezme root node
     * @return plochý seznam Nodů seřazený dle prohledávání stromu nodů od root node do hloubky
     */
    @Override
    public IteratorNodes getNodesDFS() {
        final NodeId rootNodeId = getFund().getRootNodeId();
        final List<NodeId> nodesChildsModel = new ArrayList<>();
        nodesChildsModel.addAll(getNodesChildsModel(rootNodeId));

        return new IteratorNodes(this, nodesChildsModel, outputFactoryService, MAX_CACHED_NODES);
    }

    @Override
    public FilteredRecords getRecordsByType(final String code) {
    	FilteredRecords recs = filteredRecords.get(code);
    	if(recs==null) {
    		// prepare records
    		recs = filterRecords(code);
    		filteredRecords.put(code, recs);
    	}
    	
    	return recs;
    }

    /**
     * Prepare filtered list of records
     * @param code
     * @return
     */
    private FilteredRecords filterRecords(String code) {
    	FilteredRecords records = new FilteredRecords(code);
    	
    	// Add all nodes
        IteratorNodes iteratorNodes = new IteratorNodes(this, new ArrayList<>(nodeIdsMap.values()), outputFactoryService, MAX_CACHED_NODES);
        while (iteratorNodes.hasNext()) {
        	Node node = iteratorNodes.next();
            records.addNode(node);
        }
        
        // Sort collection
        records.nodesAdded();
        
        return records;
	}

    @Override
    public Fund getFund() {
        return fund;
    }

    public void setFund(final Fund fund) {
        this.fund = fund;
    }

    @Override
    public String getInternal_code() {
        return internal_code;
    }

    public void setInternal_code(final String internal_code) {
        this.internal_code = internal_code;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getOutputId() {
        return outputId;
    }
    
    @Override
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(final String typeCode) {
        this.typeCode = typeCode;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }

    public ArrFundVersion getArrFundVersion() {
        return getFund().getArrFundVersion();
    }

    /**
     * Return item type for output
     * 
     * Item type is created if does not exist
     * @param rulItemType
     * @return
     */
	public ItemType getItemType(RulItemType rulItemType) {
        Integer itemTypeId = rulItemType.getItemTypeId();
        ItemType itemType = itemTypeMap.get(itemTypeId);
        if (itemType == null) {
            itemType = ItemType.instanceOf(rulItemType);
            itemTypeMap.put(itemTypeId, itemType);
        }
		return itemType;
	}

    /**
     * Return item specification for output
     * 
     * Item specification is created if does not exist
     * @param rulItemType
     * @return
     */
	public ItemSpec getItemSpec(RulItemSpec rulItemSpec) {
        Integer itemSpecId = rulItemSpec.getItemSpecId();
        ItemSpec itemSpec = itemSpecMap.get(itemSpecId);
        if (itemSpec == null) {
            itemSpec = ItemSpec.instanceOf(rulItemSpec);
            itemSpecMap.put(itemSpecId, itemSpec);
        }
        return itemSpec;
	}
	
	/**
	 * Return party from cache
	 * @param partyId
	 * @return
	 */
	public Party getParty(final ParParty parParty) {
		Party party = partyCache.get(parParty.getPartyId());
		if(party==null) {
			party = createParty(parParty);
		}
		return party;
	}	
    
	/**
	 * Create party object and store it in cache
	 * @param parParty
	 * @return
	 */
    private Party createParty(final ParParty parParty)
    {
        String partyTypeCode = parParty.getPartyType().getCode();
        PartyType partyType = PartyType.getByCode(partyTypeCode);

        // Prepare corresponding record
        Record record = this.recordCache.get(parParty.getRecord());
        if(record==null) {
        	record = createRecord(parParty.getRecord());
        }
        
        // create relations
        List<ParRelation> dbrelations = parParty.getRelations();
        List<Relation> rels = null;
        if(CollectionUtils.isNotEmpty(dbrelations)) {
        	rels = new ArrayList<>();
        	for(ParRelation dbRelation: dbrelations) {
        		Relation rel = createRelation(dbRelation);
        		rels.add(rel);
        	}
        }
        
        // prepare init helper
        PartyInitHelper initHelper = new PartyInitHelper(record, rels); 
        
        Party party;
        switch (partyType) {
            case DYNASTY:
                ParDynasty parDynasty = ProxyUtils.deproxy(parParty);
                party = Dynasty.newInstance(parDynasty, initHelper);
                break;
            case EVENT:
                ParEvent parEvent = ProxyUtils.deproxy(parParty);
                party = Event.newInstance(parEvent, initHelper);
                break;
            case PARTY_GROUP:
                ParPartyGroup parPartyGroup = ProxyUtils.deproxy(parParty);
                party = PartyGroup.newInstance(parPartyGroup, initHelper);
                break;
            case PERSON:
                ParPerson parPerson = ProxyUtils.deproxy(parParty);
                party = Person.newInstance(parPerson, initHelper);
                break;
            default :
                throw new IllegalStateException("Neznámý typ osoby " + partyType.getCode());
        }
        this.partyCache.put(party.getPartyId(), party);
        return party;
    }

    private Relation createRelation(ParRelation dbRelation) {
    	// prepare relation type
    	ParRelationType dbRelType = dbRelation.getRelationType();
    	RelationType relType = this.partyRelationTypeCache.get(dbRelType.getRelationTypeId());
    	if(relType==null) {
    		relType = RelationType.newInstance(dbRelType);
    		partyRelationTypeCache.put(dbRelType.getRelationTypeId(), relType);
    	}
    	// prepare list of relationTo
    	List<ParRelationEntity> entities = dbRelation.getRelationEntities();
    	List<RelationTo> relsTo = null;
    	if(CollectionUtils.isNotEmpty(entities)) {
    		relsTo = new ArrayList<>(entities.size());
    		for(ParRelationEntity dbEntity: entities)
    		{
    			RelationTo relTo = createRelationTo(dbEntity);
    			relsTo.add(relTo);
    		}
    	}
    	
    	// create relation
    	Relation relation = Relation.newInstance(dbRelation, relType, relsTo);
		return relation;
	}

	private RelationTo createRelationTo(ParRelationEntity dbEntity) {
		// prepare RelationToType
		ParRelationRoleType roleType = dbEntity.getRoleType();
		RelationToType relToType = relToTypeCache.get(roleType.getRoleTypeId());
		if(relToType==null) {
			relToType = RelationToType.newInstance(roleType);
			relToTypeCache.put(roleType.getRoleTypeId(), relToType);
		}
		// get record
		RegRecord dbRecord = dbEntity.getRecord();
		Record record = this.getRecord(dbRecord);
		
		// prepare RelationTo
		RelationTo relTo = RelationTo.newInstance(dbEntity, relToType, record);
		return relTo;
	}

	public Record getRecord(final RegRecord regRecord)
    {
    	Record record = recordCache.get(regRecord.getRecord());
		if(record==null) {
			record = createRecord(regRecord);
		}
		return record;
    }

	private Record createRecord(RegRecord regRecord) {
		RegRegisterType dbRegisterType = regRecord.getRegisterType();
		// lookup via recordTypeId
		RecordType recordType = this.recordTypes.get(regRecord.getRegisterTypeId());
		if(recordType==null) {
			recordType = this.createRecordType(dbRegisterType);
		}
		
		Record record = Record.newInstance(recordType, regRecord);
		recordCache.put(record.getRecordId(), record);
		return record;
	}

	/**
     * Return record type
     * @return
     */
	public RecordType getRecordType(RegRegisterType dbRegisterType) {
		RecordType  recordType = this.recordTypes.get(dbRegisterType.getRegisterTypeId());
		if(recordType==null) {
			recordType = createRecordType(dbRegisterType);
		}
		return recordType;
	}

    /**
     * Add new record type
     * @return
     */
    private RecordType createRecordType(RegRegisterType dbRegisterType) {
		// prepare parent
		RecordType parentType = null;
		RegRegisterType dbParentRegisterType = dbRegisterType.getParentRegisterType();
		if(dbParentRegisterType!=null) {
			parentType = getRecordType(dbParentRegisterType);
		}
		RecordType recordType = RecordType.newInstance(parentType, dbRegisterType);
		recordTypes.put(dbRegisterType.getRegisterTypeId(), recordType);
		return recordType;
	}

    /**
     * Return record from cache
     * @param recordId
     * @return
     */
	public Record getRecordFromCache(Integer recordId) {
		return recordCache.get(recordId);
	}

	public Party getPartyFromCache(Integer partyId) {
		return partyCache.get(partyId);
	}    
}
