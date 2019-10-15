package cz.tacr.elza.controller.vo.usage;

import java.util.List;
import java.util.Objects;

/**
 * 
 * @since 5. 10. 2017
 */
public class FundVO {

    private Integer id;

    private String name;

    private Integer nodeCount;

    private List<NodeVO> nodes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public List<NodeVO> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeVO> nodes) {
        this.nodes = nodes;
    }

    public FundVO() {
	}

	public FundVO(final Integer id, final String name, final Integer nodeCount,
			final List<NodeVO> nodes) {
		this.id = id;
		this.name = name;
		this.nodeCount = nodeCount;
		this.nodes = nodes;
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
