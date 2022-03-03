import React from 'react';
import {AbstractReactComponent} from '../../components/shared';
import Map from 'ol/Map';
import OSM from 'ol/source/OSM';
import TileLayer from 'ol/layer/Tile';
import View from 'ol/View';
import WKT from 'ol/format/WKT';
import VectorLayer from "ol/layer/Vector";
import VectorSource from "ol/source/Vector";
import 'ol/ol.css';

/**
 * Stránka mapy.
 * Zobrazuje stranku s mapou a vybraným poygonem
 */
class MapPage extends AbstractReactComponent {
    mapRef = null;

    constructor(props) {
        super(props);

        this.mapRef = React.createRef();
    }

    componentDidMount() {
        const {polygon} = this.props;

        if (polygon) {
            this.openMapLayer = new TileLayer({
                source: new OSM(),
            });
            const format = new WKT();
            const feature = format.readFeature(polygon, {
                dataProjection: 'EPSG:4326',
                featureProjection: 'EPSG:3857',
            });
            const vectorSource = new VectorSource({
                features: [feature],
                format,
                overlaps: false,
            });
            this.vectorLayer = new VectorLayer({
                source: new VectorSource({
                    features: [feature],
                }),
            });
            const view = new View({
                center: [0, 0],
                zoom: 0,
            });
            new Map({
                layers: [this.openMapLayer, this.vectorLayer],
                target: this.mapRef.current,
                view,
            });

            view.fit(vectorSource.getFeatures()[0].getGeometry(), {padding: [50, 50, 50, 50]});
        }
    }

    render() {
        return (
            <div className={'h-100 w-100'} ref={this.mapRef} />
        );
    }
}

export default MapPage;
