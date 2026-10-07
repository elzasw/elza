package scripts

import cz.tacr.elza.core.data.StaticDataProvider
import cz.tacr.elza.groovy.GroovyItem

// computed items of the test rule set: a note naming the script
StaticDataProvider sdp = DATA_PROVIDER
return [new GroovyItem(sdp.getItemTypeByCode("NOTE"), null, "ENT local auto")]
