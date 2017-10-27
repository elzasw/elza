package cz.tacr.elza.controller.vo.usage;

import java.util.List;

/**
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 10. 2017
 */
public class NodeVO {

	public Integer id;

	public String title;

	public List<OccurrenceVO> occurrences;

	public NodeVO() {}

	public NodeVO(final Integer id, final String title, final List<OccurrenceVO> occurrences) {
		this.id = id;
		this.title = title;
		this.occurrences = occurrences;
	}
}
