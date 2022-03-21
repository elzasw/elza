import React from 'react';
import {AbstractReactComponent, Autocomplete} from '../../components/shared';
import Map from 'ol/Map';
import OSM, {ATTRIBUTION} from 'ol/source/OSM';
import {TileWMS} from "ol/source";
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
import {LayerType} from "../../api/LayerType";
import {i18n} from 'components/shared';
import {PropTypes} from "prop-types";
import 'ol/ol.css';
import './MapPage.scss';

export const MAP_URL = '/map';
export const DEFAULT_SYSTEM_LAYER = {name: i18n('global.action.systemLayerOSM'), type: LayerType.OSM};

/**
 * Stránka mapy.
 * Zobrazuje stranku s mapou a vybraným poygonem
 */
class MapPage extends AbstractReactComponent {
    static propTypes = {
        handleChangeSelectedLayer: PropTypes.func.isRequired,
        polygon: PropTypes.string.isRequired,
        selectedLayer: PropTypes.object.isRequired,
    };

    mapRef = null;
    iframe = false;

    state = {
        polygon: null,
    }

    constructor(props) {
        super(props);

        this.mapRef = React.createRef();
    }

    UNSAFE_componentWillMount() {
        const query = new URLSearchParams(this.props.location.search);
        this.iframe = query.get('iframe');

        if (this.iframe) {
            window.addEventListener('message', (event) => {
                const origin = event.origin || event.originalEvent.origin;
                const {data} = event;

                if (origin !== window.location.origin) {
                    return;
                }
                if (!data || (typeof data === 'object' && data.call !== 'sendPolygon')) {
                    return;
                }
                this.setState({polygon: data.polygon});
            }, false);
        }
    }

    componentDidMount() {
        this.initMap();

        if (!this.state.polygon && !this.iframe) {
            this.setState({polygon: this.props.polygon});
        }
    }

    componentDidUpdate() {
        this.initMap();
    }

    createMap() {
        const {handleChangeSelectedLayer, registryLayerList: {fetched, rows}, selectedLayer} = this.props;
        const {polygon} = this.state;

        if (polygon && fetched) {
            let layer = new TileLayer({
                source: new OSM(),
            });
            // nastavení viditelné / inicializační vrstvy
            let initialSelectedLayer = selectedLayer
            if (!initialSelectedLayer) {
                initialSelectedLayer = rows.find(item => item.initial) || DEFAULT_SYSTEM_LAYER;
                if (!this.iframe) {
                    handleChangeSelectedLayer(initialSelectedLayer);
                }
            }
            // nastavení podkladových vrstev pro polygon
            if (initialSelectedLayer && initialSelectedLayer.type) {
                switch (initialSelectedLayer.type) {
                    case LayerType.OSM.toString():
                        layer = new TileLayer({
                            source: new OSM({
                                attributions: [
                                    initialSelectedLayer.name,
                                    ATTRIBUTION,
                                ],
                                url: initialSelectedLayer.url,
                            }),
                        });
                        break;
                    case LayerType.WMS.toString():
                        layer = new TileLayer({
                            source: new TileWMS({
                                attributions: [
                                    initialSelectedLayer.name,
                                    ATTRIBUTION,
                                ],
                                params: {LAYERS: initialSelectedLayer.layer},
                                url: initialSelectedLayer.url,
                            }),
                        });
                        break;
                    default:
                        console.error('Neznámý typ vrstvy: ' + initialSelectedLayer.type);
                }
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
                maxZoom: 20,
            });
            // vytvoření mapy s dvoumi vrstvami a výchozím view
            new Map({
                layers: [layer, vectorLayer],
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

    handleChangeSelected = (value) => {
        const {handleChangeSelectedLayer, selectedLayer} = this.props;

        if (JSON.stringify(value) !== JSON.stringify(selectedLayer)) {
            this.mapRef.current.innerHTML = null;
            handleChangeSelectedLayer(value);
        }
    }

    render() {
        const {registryLayerList: {fetched, rows}, selectedLayer} = this.props;

        return (
            <div className={'h-100 map-page position-relative w-100'}>
                {fetched && rows.length && !this.iframe ?
                    <div className={'position-absolute wrapper-layer-select'}>
                        <Autocomplete
                            getItemId={item => item ? JSON.stringify(item) : null}
                            getItemName={item => item ? item.name : ''}
                            items={[...rows, DEFAULT_SYSTEM_LAYER]}
                            label={i18n('global.action.layerSelection')}
                            onChange={this.handleChangeSelected}
                            value={selectedLayer}
                        />
                    </div> : null}
                <div className={'h-100 w-100'} ref={this.mapRef} />
            </div>
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
