package cz.tacr.elza.ui.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import com.vaadin.data.Collapsible;
import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;


/**
 * @author Martin Šlapa, martin.slapa@marbes.cz.
 * @since 5.6.2015
 */
public class HierarchicalCollapsibleContainer extends HierarchicalContainer implements Collapsible {

    Set<Integer> isExpandedSet = new HashSet<>();

    @Override
    public void setCollapsed(Object itemId, boolean collapsed) {
        if (collapsed) {
            isExpandedSet.remove(itemId);
        } else {
            isExpandedSet.add((Integer) itemId);
        }
    }

    @Override
    public boolean isCollapsed(Object itemId) {
        return !isExpandedSet.contains(itemId);
    }

    @Override
    public boolean removeItem(Object itemId) {
        if (!isCollapsed(itemId)) {
            Collection<?> children = getChildren(itemId);
            if (children != null) {
                ArrayList tmpChildren = new ArrayList(children);
                Iterator<?> itChilder = tmpChildren.iterator();
                while (itChilder.hasNext()) {
                    Integer child = (Integer) itChilder.next();
                    if (!isCollapsed(child)) {
                        removeItem(child);
                    }
                    super.removeItem(child);
                }
            }
        }
        return super.removeItem(itemId);
    }

    public Collection<?> getSiblings(final Object itemId){

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
    public Collection<Integer> getLowerSiblings(final Object itemId){
        Collection<?> siblings = getSiblings(itemId);

        ArrayList<OrderItem> sortList = new ArrayList<OrderItem>(siblings.size());
        for (Object sibling : siblings) {
            Item item = getItem(sibling);
            sortList.add(new OrderItem((Integer) sibling, (Integer) item.getItemProperty("Pozice").getValue()));
        }
        sortList.sort(new Comparator<OrderItem>() {
            @Override
            public int compare(final OrderItem o1, final OrderItem o2) {
                int result = o1.getPosition().compareTo(o2.getPosition());
                //pokud mají prvky stejnou pozici a jeden z nich je prvek, pro který hledáme sourozence, chceme
                //ho mít vždy nahoře
                if(result == 0 && o1.getItemId().equals(itemId)){
                    return -1;
                }
                return result;
            }
        });


        Collection result = new LinkedList();

        boolean insert = false;
        for (OrderItem sibling : sortList) {
            if(sibling.getItemId().equals(itemId) && !insert){
                insert = true;
                continue;
            }

            if(insert){
                result.add(sibling.getItemId());
            }
        }


         return result;
    }

    private class OrderItem {
        private Integer itemId;
        private Integer position;

        private OrderItem(final Integer itemId, final Integer position) {
            this.itemId = itemId;
            this.position = position;
        }

        public Integer getItemId() {
            return itemId;
        }


        public Integer getPosition() {
            return position;
        }

    }
}
