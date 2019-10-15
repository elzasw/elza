package script.groovy

import cz.tacr.elza.domain.ArrNode
import cz.tacr.elza.ws.types.v1.Did
import org.springframework.util.Assert
/**
 * Skript pro vytvoření did.
 */


ArrNode node = NODE;
return createDid(node);


/**
 * Provede vytvoření did pro zadaný node.
 * @param node node pro který se vytváří did
 * @return vytvořený did
 */
Did createDid(ArrNode node) {
    Assert.notNull(node)

    // kontrola správnosti zadaného node
    Assert.hasText(node.getUuid())

    Did did = new Did()
    did.setIdentifier(node.getUuid())

    return did
}
