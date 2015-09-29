package cz.tacr.elza.ui.view;

import com.vaadin.data.Collapsible;
import com.vaadin.data.Container;
import com.vaadin.data.util.BeanItemContainer;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 28. 8. 2015
 */
public class HierarchicalCollapsibleBeanItemContainer extends BeanItemContainer<ArrLevel> implements Container.Hierarchical, Collapsible {

    private Set<ArrNode> isExpandedSet = new HashSet<>();
    private Integer rootNodeId;

    public HierarchicalCollapsibleBeanItemContainer(Collection<ArrLevel> collection, Integer rootNodeId) throws IllegalArgumentException {
        super(ArrLevel.class, collection);
        this.rootNodeId = rootNodeId;
    }

    @Override
    public void setCollapsed(Object itemId, boolean collapsed) {
        ArrLevel level = (ArrLevel) itemId;

        if (level == null) {
            return;
        }

        if (collapsed) {
            isExpandedSet.remove(level.getNode());
        } else {
            isExpandedSet.add(level.getNode());
        }
    }

    @Override
    public boolean isCollapsed(Object itemId) {
        ArrLevel level = (ArrLevel) itemId;
        if (level == null) {
            return false;
        }
        return !isExpandedSet.contains(level.getNode());
    }

    @Override
    public List<ArrLevel> getChildren(Object itemId) {
        LinkedList<ArrLevel> children = new LinkedList<ArrLevel>();

        for (Object candidateId : getItemIds()) {
            if (getParent(candidateId) == itemId) {
                children.add((ArrLevel) candidateId);
            }
        }

        return children;
    }

    @Override
    public Object getParent(Object itemId) {
        if (itemId == null) {
            return null;
        }

        ArrLevel item = (ArrLevel) itemId;
        Object parentNodeId = item.getNodeParent().getNodeId();
        for (Object candidateId : getItemIds()) {
            ArrLevel parent = (ArrLevel) candidateId;
            if (parent.getNode().getNodeId().equals(parentNodeId)) {
                return parent;
            }
        }

        return null;
    }

    @Override
    public List<ArrLevel> rootItemIds() {
        LinkedList<ArrLevel> result = new LinkedList<ArrLevel>();
        for (Object candidateId : getItemIds()) {
            ArrLevel node = (ArrLevel) candidateId;
            Object parentRef = node.getNodeParent().getNodeId();
            if (parentRef.equals(rootNodeId)) {
                result.add((ArrLevel) candidateId);
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
        ArrLevel node = (ArrLevel) itemId;
        return node.getNodeParent().getNodeId().equals(rootNodeId);
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
                    ArrLevel child = (ArrLevel) itChilder.next();
                    if (!isCollapsed(child)) {
                        removeItem(child);
                    }
                    super.removeItem(child);
                }
            }
        }
        return super.removeItem(itemId);
    }

    public List<ArrLevel> getSiblings(final Object itemId){
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
    public Collection<ArrLevel> getLowerSiblings(final Object itemId){
        ArrLevel item = (ArrLevel) itemId;
        List<ArrLevel> siblings = getSiblings(itemId);

        siblings.sort(new Comparator<ArrLevel>() {
            @Override
            public int compare(final ArrLevel o1, final ArrLevel o2) {
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
        for (ArrLevel sibling : siblings) {
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
