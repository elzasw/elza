package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Úroveň hierarchického popisu. Úroveň sama o sobě není nositelem hodnoty. Vlastní hodnoty prvků
 * popisu jsou zapsány v atributech archivního popisu {@link ArrDescItem}.
 *
 * @author vavrejn
 *
 * @param <FC> {@link ArrChange}
 * @param <N> {@link ArrNode}
 */
public interface ArrLevel<FC extends ArrChange, N extends ArrNode> extends Serializable {

    Integer getLevelId();

    void setLevelId(Integer levelId);

    /**
     * 
     * @return identifikátor uzlu, existuje více záznamů se stejným node_id, všechny reprezentují
     *         stejný uzel stromu - v případě, že je uzel zařazený pouze do jedné AP (má jednoho
     *         nadřízeného), tak je pouze jedno platné node_id - platné = nevyplněné node_id - pokud
     *         je uzel přímo zařazený do jiné AP, tak existují 2 platné záznamy uzlu (nevyplněné
     *         delete_change_id).
     */
    N getNode();

    /**
     * identifikátor uzlu, existuje více záznamů se stejným node_id, všechny reprezentují stejný
     * uzel stromu - v případě, že je uzel zařazený pouze do jedné AP (má jednoho nadřízeného), tak
     * je pouze jedno platné node_id - platné = nevyplněné node_id - pokud je uzel přímo zařazený do
     * jiné AP, tak existují 2 platné záznamy uzlu (nevyplněné delete_change_id)
     * 
     * @param node identifikátor uzlu.
     */
    void setNode(N node);

    /**
     * 
     * @return odkaz na nadřízený uzel stromu, v případě root levelu je NULL.
     */
    N getNodeParent();

    /**
     * 
     * @param parentNode odkaz na nadřízený uzel stromu, v případě root levelu je NULL.
     */
    void setNodeParent(N parentNode);

    /**
     * @return číslo změny vytvoření uzlu.
     */
    FC getCreateChange();

    /**
     * @param createChange číslo změny vytvoření uzlu.
     */
    void setCreateChange(FC createChange);

    /**
     * @return číslo změny smazání uzlu.
     */
    FC getDeleteChange();

    /**
     * @param deleteChange číslo změny smazání uzlu.
     */
    void setDeleteChange(FC deleteChange);

    /**
     * @return pozice uzlu mezi sourozenci.
     */
    Integer getPosition();

    /**
     * @param position pozice uzlu mezi sourozenci.
     */
    void setPosition(Integer position);
}
