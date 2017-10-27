package cz.tacr.elza.controller.vo.usage;

public class OccurrenceVO {

	public Integer id;

	public OccurrenceType type;

	public OccurrenceVO() {
	}

	public OccurrenceVO(final Integer id, final OccurrenceType type) {
		this.id = id;
		this.type = type;
	}

}
