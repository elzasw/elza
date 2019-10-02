package cz.tacr.elza.controller.vo.usage;

import java.util.List;

/**
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 10. 2017
 */
public class PartyVO {

	private Integer id;

	private String name;

	private List<OccurrenceVO> occurrences;

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public List<OccurrenceVO> getOccurrences() {
		return occurrences;
	}

	public void setOccurrences(final List<OccurrenceVO> occurrences) {
		this.occurrences = occurrences;
	}

}
