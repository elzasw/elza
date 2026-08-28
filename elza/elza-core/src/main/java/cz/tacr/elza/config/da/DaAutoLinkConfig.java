package cz.tacr.elza.config.da;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Settings of the automatic attachment of received AIPs to nodes
 * ({@code elza.da.auto-link}).
 *
 * A received AIP is attached to the node of its fund whose description item of
 * {@link #itemType} (and {@link #itemSpec}, when set) carries one of the AIP
 * identifiers, see {@link cz.tacr.elza.service.da.DaAipAutoLinkService}.
 */
@Configuration
@ConfigurationProperties("elza.da.auto-link")
public class DaAutoLinkConfig {

    /** Code of the item type holding the identifier of the unit in its source system. */
    private String itemType = "ZP2015_OTHER_ID";

    /** Code of the item specification of the identifier; {@code null} matches items without a specification. */
    private String itemSpec = "ZP2015_OTHERID_SOURCEID";

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getItemSpec() {
        return itemSpec;
    }

    public void setItemSpec(String itemSpec) {
        this.itemSpec = itemSpec;
    }
}
