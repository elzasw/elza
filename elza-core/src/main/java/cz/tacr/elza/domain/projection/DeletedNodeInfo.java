package cz.tacr.elza.domain.projection;

import java.time.OffsetDateTime;

/**
 * The last deletion of a unit of description that has no active level: where the unit was and
 * who deleted it, so a user can find the deletion in the history of changes of the fund.
 *
 * @param nodeId     id of the unit
 * @param uuid       UUID of the unit
 * @param fundName   name of the fund of the unit
 * @param deleteDate date of the change that deleted the unit
 * @param username   user who made that change, null for a change without a user (the system or
 *                   the built-in administrator)
 */
public record DeletedNodeInfo(Integer nodeId,
                              String uuid,
                              String fundName,
                              OffsetDateTime deleteDate,
                              String username) {
}
