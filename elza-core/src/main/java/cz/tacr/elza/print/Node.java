package cz.tacr.elza.print;

import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.print.item.Item;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class Node implements RecordProvider {
    private final Output output; // vazba na parent output
    private final ArrNode arrNode; // vazba na DB objekt, povinný údaj
    private final ArrLevel arrLevel; // vazba na DB objekt, povinný údaj

    private Integer position;
    private Integer depth;
    private List<Record> records = new ArrayList<>();
    private List<Item> items = new ArrayList<>(); // seznam všech atributů outputu

    public Node(Output output, ArrNode arrNode, ArrLevel arrLevel) {
        this.output = output;
        this.arrNode = arrNode;
        this.arrLevel = arrLevel;
    }

    public Integer getDepthInLevel() {
        return null;  // TODO Lebeda - implementovat ???
    }

    /**
     * @return dohledá v output.modes node, který je nadřazený tomuto. Pokud není nalezen nebo neexistuje vrací null.
     */
    public Node getParent() {
        final ArrNode arrNodeParent = arrLevel.getNodeParent();

        if (arrLevel.getNodeParent() == null) {
            return null; // žádný otec
        } else {
            // dohledat otce
            return output.getNodes().stream()
                    .filter(nodeOutput -> arrNodeParent.equals(nodeOutput.getArrNode()))
                    .findFirst().orElse(null);
        }

    }

    /**
     * @return vrací seznam dětí, omezeno jen na node outputu
     */
    public List<Node> getChildren() {
        return output.getNodes().stream()
                .filter(node -> arrNode.equals(node.getArrLevel().getNodeParent()))
                .collect(Collectors.toList());
    }

    /**
     * @param codes seznam kódů typů atributů.
     * @return vrací se seznam hodnot těchto atributů, řazeno dle rul_desc_item.view_order + arr_item.position
     */
    public List<Item> getItems(@NotNull Collection<String> codes) {
        Assert.notNull(codes);
        return items.stream()
                .filter(item -> codes.contains(item.getType().getCode()))
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     *
     * @param code požadovaný kód položky
     * @return vrací seznam hodnot položek s odpovídajícím kódem oddělený čárkou (typicky 1 položka = její serializeValue)
     */
    public String getItemsValueByCode(@NotNull String code) {
        return getItems(Collections.singletonList(code)).stream()
                .map(Item::serializeValue)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    /**
     * Metoda pro získání hodnoty do fieldu v Jasper.
     * Umožní na položce v detailu volat metody sám nad sebou (nejen implicitně zpřístupněné gettery).
     *
     * @return odkaz sám na sebe
     */
    public Node getNode() {
        return this;
    }

    /**
     * Vstupem je seznam kódu typů atributů a vrací se seznam všech hodnot atributů výstupu kromě hodnot
     * typů uvedených ve vstupu metody, řazeno dle rul_desc_item.view_order + arr_item.position.
     *
     * @param codes     seznam kódu typů atributů
     * @return   seznam všech hodnot atributů kromě hodnot typů uvedených ve vstupu metody
     */
    public List<Item> getAllItems(@NotNull Collection<String> codes) {
        Assert.notNull(codes);
        return items.stream()
                .filter(item -> !codes.contains(item.getType().getCode()))
                .sorted(Item::compareToItemViewOrderPosition)
                .collect(Collectors.toList());
    }

    public ArrNode getArrNode() {
        return arrNode;
    }

    private ArrLevel getArrLevel() {
        return arrLevel;
    }

    /**
     * @return všechny Items přiřazené na node. (= prostý getter)
     */
    public List<Item> getItems() {
        return items;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public List<Record> getRecords() {
        return records;
    }

    @Override
    public List<Node> getRecordProviderChildern() {
        return getChildren();
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Node) {
            final Node o1 = (Node) o;
            return new EqualsBuilder().append(arrNode.getNodeId(), o1.getArrNode().getNodeId()).isEquals();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        // podstatný je zdrojový arrNode
        return new HashCodeBuilder().append(arrNode).toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
