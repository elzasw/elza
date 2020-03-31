package cz.tacr.elza.service.importnodes.vo.descitems;

/**
 * Rozhraní pro reprezentaci atributu.
 * @since 28.2.2020
 */
public interface ItemUriRef extends Item{

    String getValue();

    String getSchema();

    String getDescription();

    Integer getNodeId();
}
