package cz.tacr.elza.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * External system providing AI services over the "Elza AI Provider API"
 * (contract: elza-development/typespec-ai). Inherited fields carry the whole
 * configuration: url = provider endpoint, apiKeyId = signing KeyId,
 * apiKeyValue = HMAC secret (scheme ELZA-AI-HMAC-SHA256).
 */
@Entity(name = "ai_external_system")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class AiExternalSystem extends SysExternalSystem {

    public AiExternalSystem() {
    }

    public AiExternalSystem(AiExternalSystem src) {
        super(src);
    }
}
