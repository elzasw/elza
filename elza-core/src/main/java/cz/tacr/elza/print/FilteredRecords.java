package cz.tacr.elza.print;

import java.text.Collator;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Collection of records
 *
 */
public class FilteredRecords
{
	/**
	 * Code of applied filter
	 */
	String filterType;
	
	/**
	 * Hash map of records by record id
	 */
	private Map<Integer, RecordWithLinks> recordsMap = new HashMap<>();
	
	/**
	 * Sorted collection of records
	 */
	private Collection<RecordWithLinks> records;
	
	FilteredRecords(String filter) {
		filterType = filter;
	}
	
	public Collection<RecordWithLinks> getRecords(){
		return records;
	}

	/**
	 * Add to the list of records 
	 * @param node
	 */
	public void addNode(Node node) {
		List<Record> recs = node.getRecords();
		
		for(Record rec: recs) {
			addRecord(rec, node);
		}		
	}

	/**
	 * Add single result
	 * @param rec
	 * @param node
	 * @return
	 */
	private RecordWithLinks addRecord(Record rec, Node node) {
		int recordId = rec.getRecordId();
		// check if record exists
		RecordWithLinks rwl = recordsMap.get(recordId);
		if(rwl==null) {
			// check if allowed record type
			RecordType recType = rec.getRecordType();
			while(recType!=null) {
				if(filterType.equals(recType.getCode())) {
					rwl = RecordWithLinks.newInstance(rec);
					recordsMap.put(recordId, rwl);
					rwl.addNode(node);
					break;
				}
				// get parent type
				recType = recType.getParentType();
			}			
		} else {
			rwl.addNode(node);
		}
		return rwl;
	}

	/**
	 * Final method when all nodes were added
	 * 
	 *  This method allows to sort items
	 */
	public void nodesAdded() {
		Collator collator = Collator.getInstance(Locale.forLanguageTag("cs"));
		records = recordsMap.values().stream().sorted((v1, v2) -> collator.compare(v1.getRecord(), v2.getRecord()))
				.collect(Collectors.toList());		
	} 
}