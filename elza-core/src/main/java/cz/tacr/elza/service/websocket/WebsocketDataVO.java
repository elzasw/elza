package cz.tacr.elza.service.websocket;

/**
 * Obálka s daty pro klienta.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public class WebsocketDataVO<T> {

    /**
     * Typ dat.
     */
    private WebsocketDataType area;
    /**
     * Data
     */
    private T value;

    public WebsocketDataVO() {
    }

    public WebsocketDataVO(final WebsocketDataType area, final T value) {
        this.area = area;
        this.value = value;
    }

    public WebsocketDataType getArea() {
        return area;
    }

    public void setArea(final WebsocketDataType area) {
        this.area = area;
    }

    public T getValue() {
        return value;
    }

    public void setValue(final T value) {
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
