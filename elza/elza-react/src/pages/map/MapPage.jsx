import React from 'react';
import {AbstractReactComponent} from '../../components/shared';
import Map from 'ol/Map';
import OSM, {ATTRIBUTION} from 'ol/source/OSM';
import TileLayer from 'ol/layer/Tile';
import View from 'ol/View';
import WKT from 'ol/format/WKT';
import VectorLayer from "ol/layer/Vector";
import VectorSource from "ol/source/Vector";
import {
    AREA_REGISTRY_LAYER_LIST,
    layerConfigurationListFetchIfNeeded,
} from "../../actions/registry/registry";
import {connect} from "react-redux";
import {storeFromArea} from 'shared/utils';
import 'ol/ol.css';

export const MAP_URL = '/map';

/**
 * Stránka mapy.
 * Zobrazuje stranku s mapou a vybraným poygonem
 */
class MapPage extends AbstractReactComponent {
    mapRef = null;
    state = {
        polygon: null
    }

    constructor(props) {
        super(props);

        this.mapRef = React.createRef();
    }

    UNSAFE_componentWillMount() {
        window.addEventListener('message', (event) => {
            const origin = event.origin || event.originalEvent.origin;
            const {data} = event;

            if (origin !== window.location.origin)
                return;
            if (!data || (typeof data === 'object' && data.call !== 'sendPolygon')) {
                return;
            }

            this.setState({polygon: data.polygon})
        }, false);
    }

    componentDidMount() {
        this.initMap();

        if (!this.state.polygon) {
            this.setState({polygon: this.props.polygon});
        }
    }

    componentDidUpdate() {
        this.initMap();
    }

    createMap() {
        const {registryLayerList} = this.props;
        const {polygon} = this.state;

        if (polygon && registryLayerList.fetched) {
            const layers = []

            if (registryLayerList.rows.length) {
                // nastavení podkladových vrstev pro polygon
                registryLayerList.rows.forEach(item => {
                    layers.push(new TileLayer({
                        source: new OSM(
                            {
                                attributions: [
                                    item.name,
                                    ATTRIBUTION,
                                ],
                                url: item.url,
                                zDirection: item.zIndex
                            }
                        ),
                    }));
                })
            } else {
                // defaultní podkladová OSM vstva (celý svět) pro polygon
                layers.push(new TileLayer({
                    source: new OSM(),
                }));
            }
            // souřadnice v polygonu máme ve formátu WKT
            const format = new WKT();
            // příprava feature / načení polygonu v WKT, viz dokumentace https://openlayers.org/
            const feature = format.readFeature(polygon, {
                dataProjection: 'EPSG:4326',
                featureProjection: 'EPSG:3857',
            });
            // připrava vektorového zdroje s polygonem / feature pro speciální vektorovou vrstvu
            const vectorSource = new VectorSource({
                features: [feature],
                format,
                overlaps: false,
            });
            // speciální vektorová vrstva s polygonem ve vektrovým zdrojem / feature
            const vectorLayer = new VectorLayer({
                source: new VectorSource({
                    features: [feature],
                }),
            });
            // defaultně nastavený view mapy
            const view = new View({
                center: [0, 0],
                zoom: 0,
            });
            // vytvoření mapy s dvoumi vrstvami a výchozím view
            new Map({
                layers: [...layers, vectorLayer],
                target: this.mapRef.current,
                view,
            });
            // vycentrování mapy na polygon / feature ve vektorovém zdroji
            view.fit(vectorSource.getFeatures()[0].getGeometry(), {padding: [50, 50, 50, 50]});
        }
    }

    initMap() {
        this.props.dispatch(layerConfigurationListFetchIfNeeded());
        this.createMap();
    };

    render() {
        return (
            <div className={'h-100 w-100'} ref={this.mapRef} />
        );
    }
}

const mapStateToProps = (state, props) => {
    const registryLayerList = storeFromArea(state, AREA_REGISTRY_LAYER_LIST);

    return {
        registryLayerList,
        ...props
    };
}

export default connect(mapStateToProps)(MapPage);
