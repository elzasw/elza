package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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
    final private int id;
    /**
     * Hloubka uzlu ve stromu.
     */
    private Integer depth;
    /**
     * Pozice uzlu v dětech.
     */
    private int position;
    /**
     * Rodič uzlu
     */
    private TreeNode parent;

    private List<TreeNode> children = new ArrayList<>();

    /**
     * Referenční označení. Od kořene k uzlu.
     */
    private Integer[] referenceMark;

    public TreeNode(final int nodeId, final int position) {
        this.id = nodeId;
        this.position = position;
    }

    public Integer getId() {
        return id;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(final Integer depth) {
        this.depth = depth;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(final int position) {
        this.position = position;
    }

    public TreeNode getParent() {
        return parent;
    }

    public void setParent(final TreeNode parent) {
        this.parent = parent;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(final List<TreeNode> childs) {
        this.children = childs;
    }

    public void addChild(final TreeNode child) {
        children.add(child);
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
                ", childs=" + children.size() +
                '}';
    }

    @Override
    public int compareTo(final TreeNode o) {
    	// TODO: throw exception if comparing with different parents 

        if (this.getParent() == null) {
            return -1;
        }

        if (o.getParent() == null) {
            return 1;
        }

        if (o.id==id) {
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

        while (Objects.equals(level1, level2)) {
            level1 = parents1.isEmpty() ? null : parents1.removeFirst();
            level2 = parents2.isEmpty() ? null : parents2.removeFirst();


            if (level1 == null) {
                return -1;
            }

            if (level2 == null) {
                return 1;
            }
        }

        return Integer.compare(level1.getPosition(), level2.getPosition());
    }
}
