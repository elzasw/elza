package cz.tacr.elza.drools.service;

/**
 * Answers whether another unit of description of the fund carries the same UNITID value of the
 * item type. The Drools model asks it lazily, see
 * {@link cz.tacr.elza.drools.model.DescItem#isDuplicateInFund()}.
 */
@FunctionalInterface
public interface UnitIdDuplicateChecker {

    boolean isDuplicate(Integer nodeId, Integer itemTypeId, String value);
}
