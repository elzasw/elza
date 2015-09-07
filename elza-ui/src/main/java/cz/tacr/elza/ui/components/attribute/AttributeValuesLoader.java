package cz.tacr.elza.ui.components.attribute;

import java.util.List;

import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.ui.components.autocomplete.AutocompleteItem;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 3.9.2015
 */
public interface AttributeValuesLoader {

    List<AutocompleteItem> loadPartyRefItemsFulltext(String text);

    List<AutocompleteItem> loadRecordRefItemsFulltext(String text, RulDescItemSpec specification);


}
