package cz.tacr.elza.controller.vo.usage;

public class OccurrenceVO {

    private Integer id;

    private OccurrenceType type;

	public OccurrenceVO() {
	}

	public OccurrenceVO(final Integer id, final OccurrenceType type) {
		this.id = id;
		this.type = type;
	}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public OccurrenceType getType() {
        return type;
    }

    public void setType(OccurrenceType type) {
        this.type = type;
    }

}
