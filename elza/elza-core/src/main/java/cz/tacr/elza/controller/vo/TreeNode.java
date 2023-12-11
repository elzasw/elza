package cz.tacr.elza.controller.vo;

import java.util.LinkedList;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Data uzlu uložené v cache.
 *
 * @since 11.01.2016
 */
public class TreeNode implements Comparable<TreeNode> {

    /**
     * Nodeid uzlu.
     */
    private Integer id;
    /**
     * Hloubka uzlu ve stromu.
     */
    private Integer depth;
    /**
     * Pozice uzlu v dětech.
     */
    private Integer position;
    /**
     * Rodič uzluz
     */
    private TreeNode parent;

    private LinkedList<TreeNode> childs = new LinkedList<>();

    /**
     * Referenční označení. Od kořene k uzlu.
     */
    private Integer[] referenceMark;

    public TreeNode(final Integer nodeId, final Integer position) {
        this.id = nodeId;
        this.position = position;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(final Integer depth) {
        this.depth = depth;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    public TreeNode getParent() {
        return parent;
    }

    public void setParent(final TreeNode parent) {
        this.parent = parent;
    }

    public LinkedList<TreeNode> getChilds() {
        return childs;
    }

    public void setChilds(final LinkedList<TreeNode> childs) {
        this.childs = childs;
    }

    public void addChild(final TreeNode child) {
        childs.add(child);
    }

    public Integer[] getReferenceMark() {
        return referenceMark;
    }

    public void setReferenceMark(final Integer[] referenceMark) {
        this.referenceMark = referenceMark;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TreeNode treeNode = (TreeNode) o;

        return new EqualsBuilder()
                .append(id, treeNode.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "TreeNode{" +
                "position=" + position +
                ", id=" + id +
                ", childs=" + childs.size() +
                '}';
    }

    @Override
    public int compareTo(final TreeNode o) {

        if (this.getParent() == null) {
            return -1;
        }

        if (o.getParent() == null) {
            return 1;
        }

        if (o.getId().equals(getId())) {
            return 0;
        }

        LinkedList<TreeNode> parents1 = new LinkedList<>();
        LinkedList<TreeNode> parents2 = new LinkedList<>();


        TreeNode parent = this;
        while (parent != null) {
            parents1.addFirst(parent);
            parent = parent.getParent();
        }

        parent = o;
        while (parent != null) {
            parents2.addFirst(parent);
            parent = parent.getParent();
        }


        TreeNode level1 = parents1.removeFirst();
        TreeNode level2 = parents2.removeFirst();

        while (ObjectUtils.equals(level1, level2)) {
            level1 = parents1.isEmpty() ? null : parents1.removeFirst();
            level2 = parents2.isEmpty() ? null : parents2.removeFirst();


            if (level1 == null) {
                return -1;
            }

            if (level2 == null) {
                return 1;
            }
        }


        return level1.getPosition().compareTo(level2.getPosition());
    }
}
