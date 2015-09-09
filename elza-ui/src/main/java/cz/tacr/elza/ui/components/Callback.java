package cz.tacr.elza.ui.components;

/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 9.9.2015
 */
public interface Callback<T> {
    void callback(T item);
}
