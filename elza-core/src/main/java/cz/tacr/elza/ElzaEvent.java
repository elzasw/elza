package cz.tacr.elza;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
public class ElzaEvent {

    String info;

    public ElzaEvent(String info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return info;
    }
}
