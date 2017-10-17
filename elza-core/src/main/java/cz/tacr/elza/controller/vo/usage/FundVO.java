package cz.tacr.elza.controller.vo.usage;

import java.util.List;
import java.util.Objects;

/**
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 10. 2017
 */
public class FundVO {

	private Integer id;

	private String name;

	private Integer nodeCount;

	private List<NodeVO> nodes;

	public FundVO(final Integer id, final String name, final Integer nodeCount,
			final List<NodeVO> nodes) {
		this.id = id;
		this.name = name;
		this.nodeCount = nodeCount;
		this.nodes = nodes;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Integer getNodeCount() {
		return nodeCount;
	}

	public List<NodeVO> getNodes() {
		return nodes;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(final Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        FundVO that = (FundVO) o;
        return Objects.equals(id, that.id);
	}
}
