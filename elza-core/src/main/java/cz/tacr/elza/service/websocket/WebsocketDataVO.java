package cz.tacr.elza.service.websocket;

import java.util.Collection;

import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;

/**
 * Obálka s daty pro klienta.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public class WebsocketDataVO {

    /**
     * Typ dat.
     */
    private WebsocketDataType area;
    /**
     * Data
     */
    private Collection<AbstractEventSimple> value;

    public WebsocketDataVO() {
    }

    public WebsocketDataVO(final WebsocketDataType area, final Collection<AbstractEventSimple> value) {
        this.area = area;
        this.value = value;
    }

    public WebsocketDataType getArea() {
        return area;
    }

    public void setArea(final WebsocketDataType area) {
        this.area = area;
    }

    public Collection<AbstractEventSimple> getValue() {
        return value;
    }

    public void setValue(final Collection<AbstractEventSimple> value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "WebsocketDataVO{" +
                "area=" + area +
                ", value=" + value.toString() +
                '}';
    }
}
