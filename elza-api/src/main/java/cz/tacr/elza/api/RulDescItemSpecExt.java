package cz.tacr.elza.api;

/**
 * Rozšíření {@link RulDescItemSpec}.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 *
 * @param <RIT> {@link RulDescItemType}
 */
public interface RulDescItemSpecExt<RIT extends RulDescItemType, RT extends RegRegisterType, P extends RulPackage> extends RulDescItemSpec<RIT, P> {

}
