package cz.tacr.elza.api;

/**
 * Rozšíření {@link RulItemSpec}.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 *
 * @param <RIT> {@link RulItemType}
 */
public interface RulItemSpecExt<RIT extends RulItemType, RT extends RegRegisterType, P extends RulPackage> extends RulItemSpec<RIT, P> {

}
