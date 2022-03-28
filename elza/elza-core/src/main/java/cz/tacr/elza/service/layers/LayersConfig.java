package cz.tacr.elza.service.layers;

import cz.tacr.elza.controller.vo.MapLayerVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties("elza.map")
public class LayersConfig {

    private List<MapLayerVO> layers = new ArrayList<>();

    public List<MapLayerVO> getLayers() {
        return layers;
    }

    public void setLayers(List<MapLayerVO> layers) {
        if (CollectionUtils.isNotEmpty(layers)) {
            for (MapLayerVO layer : layers) {
                if (layer.getInitial() == null) {
                    layer.setInitial(false);
                }
            }
        }

        this.layers = layers;
    }
}
