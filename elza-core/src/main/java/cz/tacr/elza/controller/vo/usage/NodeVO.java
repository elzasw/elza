package cz.tacr.elza.controller.vo.usage;

import java.util.List;

/**
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 10. 2017
 */
public class NodeVO {

	private Integer id;

	private String title;

	private List<OccurrenceVO> occurrences;

	public NodeVO(final Integer id, final String title, final List<OccurrenceVO> occurrences) {
		this.id = id;
		this.title = title;
		this.occurrences = occurrences;
	}

	public Integer getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public List<OccurrenceVO> getOccurrences() {
		return occurrences;
	}
}
