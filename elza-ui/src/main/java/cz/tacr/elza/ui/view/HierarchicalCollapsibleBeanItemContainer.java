package cz.tacr.elza.ui.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.vaadin.data.Collapsible;
import com.vaadin.data.Container;
import com.vaadin.data.util.BeanItemContainer;

import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrNode;


/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 28. 8. 2015
 */
public class HierarchicalCollapsibleBeanItemContainer extends BeanItemContainer<ArrFaLevel> implements Container.Hierarchical, Collapsible {

    private Set<ArrNode> isExpandedSet = new HashSet<>();
    private Integer rootNodeId;

    public HierarchicalCollapsibleBeanItemContainer(Collection<ArrFaLevel> collection, Integer rootNodeId) throws IllegalArgumentException {
        super(ArrFaLevel.class, collection);
        this.rootNodeId = rootNodeId;
    }

    @Override
    public void setCollapsed(Object itemId, boolean collapsed) {
        ArrFaLevel level = (ArrFaLevel) itemId;

        if (collapsed) {
            isExpandedSet.remove(level.getNode());
        } else {
            isExpandedSet.add(level.getNode());
        }
    }

    @Override
    public boolean isCollapsed(Object itemId) {
        ArrFaLevel level = (ArrFaLevel) itemId;
        return !isExpandedSet.contains(level.getNode());
    }

    @Override
    public List<ArrFaLevel> getChildren(Object itemId) {
        LinkedList<ArrFaLevel> children = new LinkedList<ArrFaLevel>();

        for (Object candidateId : getItemIds()) {
            if (getParent(candidateId) == itemId) {
                children.add((ArrFaLevel) candidateId);
            }
        }

        return children;
    }

    @Override
    public Object getParent(Object itemId) {
        if (itemId == null) {
            return null;
        }

        ArrFaLevel item = (ArrFaLevel) itemId;
        Object parentNodeId = item.getParentNode().getNodeId();
        for (Object candidateId : getItemIds()) {
            ArrFaLevel parent = (ArrFaLevel) candidateId;
            if (parent.getNode().getNodeId().equals(parentNodeId)) {
                return parent;
            }
        }

        return null;
    }

    @Override
    public List<ArrFaLevel> rootItemIds() {
        LinkedList<ArrFaLevel> result = new LinkedList<ArrFaLevel>();
        for (Object candidateId : getItemIds()) {
            ArrFaLevel node = (ArrFaLevel) candidateId;
            Object parentRef = node.getParentNode().getNodeId();
            if (parentRef.equals(rootNodeId)) {
                result.add((ArrFaLevel) candidateId);
            }
        }

        return result;
    }

    @Override
    public boolean setParent(Object itemId, Object newParentId) throws UnsupportedOperationException {
        return true;
    }

    @Override
    public boolean areChildrenAllowed(Object itemId) {
        return true;
    }

    @Override
    public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) throws UnsupportedOperationException {
        return true;
    }

    @Override
    public boolean isRoot(Object itemId) {
        ArrFaLevel node = (ArrFaLevel) itemId;
        return node.getParentNode().getNodeId().equals(rootNodeId);
    }

    @Override
    public boolean hasChildren(Object itemId) {
        return true;
    }

    @Override
    public boolean removeItem(Object itemId) {
        if (!isCollapsed(itemId)) {
            Collection<?> children = getChildren(itemId);
            if (children != null) {
                ArrayList tmpChildren = new ArrayList(children);
                Iterator<?> itChilder = tmpChildren.iterator();
                while (itChilder.hasNext()) {
                    ArrFaLevel child = (ArrFaLevel) itChilder.next();
                    if (!isCollapsed(child)) {
                        removeItem(child);
                    }
                    super.removeItem(child);
                }
            }
        }
        return super.removeItem(itemId);
    }

    public List<ArrFaLevel> getSiblings(final Object itemId){
        if(getParent(itemId) == null){
            return rootItemIds();
        } else{
            return getChildren(getParent(itemId));
        }
    }

    /**
     * Najde seřazené sourozence pod daným id.
     * @param itemId id
     * @return seznam sourozenců
     */
    public Collection<ArrFaLevel> getLowerSiblings(final Object itemId){
        ArrFaLevel item = (ArrFaLevel) itemId;
        List<ArrFaLevel> siblings = getSiblings(itemId);

        siblings.sort(new Comparator<ArrFaLevel>() {
            @Override
            public int compare(final ArrFaLevel o1, final ArrFaLevel o2) {
                int result = o1.getPosition().compareTo(o2.getPosition());
                //pokud mají prvky stejnou pozici a jeden z nich je prvek, pro který hledáme sourozence, chceme
                //ho mít vždy nahoře
                if(result == 0 && o1.getNode().getNodeId().equals(item.getNode().getNodeId())){
                    return -1;
                }
                return result;
            }
        });


        Collection result = new LinkedList();

        boolean insert = false;
        for (ArrFaLevel sibling : siblings) {
            if(sibling.getNode().getNodeId().equals(item.getNode().getNodeId()) && !insert){
                insert = true;
                continue;
            }

            if(insert){
                result.add(sibling);
            }
        }


         return result;
    }
}
