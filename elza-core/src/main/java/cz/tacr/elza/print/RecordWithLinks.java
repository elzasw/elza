package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.collections.CollectionUtils;

import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemType;

/**
 * Record with valid links to other entities
 */
public class RecordWithLinks
	extends Record
{

	List<Node> nodes = new ArrayList<>();
	    
    private RecordWithLinks(Record srcRecord)
    {
    	super(srcRecord);
    }

    /*
    // vrací seznam Node přiřazených přes vazbu arr_node_register
    public Map<NodeId, Node> getNodes() {

        IteratorNodes iterator = output.getNodesBFS();

        Map<NodeId, Node> nodes = new LinkedHashMap<>();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            if (node.getRecords().contains(this)) {
                NodeId nodeId = iterator.getActualNodeId();
                nodes.put(nodeId, node);
            }
        }

        return nodes;
    }*/

    /**
     * Serializuje seznam node navázaných na record v aktuálním outputu pomocí první nalezené hodnoty z itemů dle code.
     * Pokud není naleze žádný vyhovující item, vypíše se node.toString().
     *
     * @param codes seznam kódů možných itemů, pořadí je respektováno
     * @return seznam serializovaných node oddělěný čárkou
     */
    /*
    public String getNodesSerialized(@NotNull final Collection<String> codes) {
        List<String> result = new ArrayList<>();
        final Map<NodeId, Node> nodes = getNodes();

        for (Map.Entry<NodeId, Node> nodeIdNodeEntry : nodes.entrySet()) {

            String serializedString = "";
            for (String code : codes) {
                serializedString = nodeIdNodeEntry.getValue().getAllItemsAsString(Collections.singletonList(code));
                if (StringUtils.isNotBlank(serializedString)) {
                    break;
                }
            }
            if (StringUtils.isBlank(serializedString)) {
                serializedString = "[" + nodeIdNodeEntry.getKey().toString() + "]";
            }
            final String trim = StringUtils.trim(serializedString);
            result.add(trim);
        }


        return StringUtils.join(result, ", ");
    }*/
    
	public void addNode(Node node) {
		nodes.add(node);		
	}
	
	public List<Node> getNodes(){
		return nodes;
	}
	
	/**
	 * Return collection of sorted values
	 * @param itemTypeCode Type of item
	 * @return
	 */
	public Collection<String> getSortedValues(String itemTypeCode)
	{
		SortedSet<String> result = null;
		//List<Item> items = new ArrayList<>();
		// prepare collection of items
		for(Node node: nodes)
		{
			for(Item item :node.getItems()) {
				ItemType itemType = item.getType();
				if(itemType.getCode().equals(itemTypeCode)) {
					// prepare result set if do not exists
					if(result==null) {
						result = createSortedSet(itemType);
					}
					result.add(item.serializeValue());
				}
			}
		}
		return result;
	}
	
	/**
	 * Prepare set with comparator for given ItemType
	 * @param itemType
	 * @return
	 */
	private SortedSet<String> createSortedSet(ItemType itemType) {
		Comparator<String> c;
		if(itemType.getDataType().equals("INT")) {
			// comparator for ints
			c = ( (arg0, arg1) -> Integer.compare(Integer.valueOf(arg0), Integer.valueOf(arg1)) );
		} else {
			// comparator for strings
			c =( (a,b) -> a.compareTo(b) );
		}
		return new TreeSet<>(c);
	}

	/**
	 * Return sorted list of values
	 * @param itemType Type of description item
	 * @param separator
	 * @return
	 */
	public String getSortedValuesOf(String itemTypeCode, String separator)
	{
		Collection<String> sortedValues = getSortedValues(itemTypeCode);
		if(CollectionUtils.isEmpty(sortedValues)) {
			return "";
		}
		
		// return values as string
		return String.join(separator, sortedValues);
	}

    /**
     * Return value of RegRecord
     * @param regRecord
     * @param output
     * @return
     */
	public static RecordWithLinks newInstance(Record srcRecord) {
		RecordWithLinks record = new RecordWithLinks(srcRecord);
		return record;
	}
}
