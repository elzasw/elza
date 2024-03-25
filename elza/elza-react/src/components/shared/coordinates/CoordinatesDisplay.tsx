import { modalDialogHide, modalDialogShow } from 'actions/global/modalDialog';
import classNames from 'classnames';
import i18n from 'components/i18n';
import { PolygonShowInMap } from "components/PolygonShowInMap";
import { TooltipTrigger } from 'components/shared';
import Icon from 'components/shared/icon/Icon';
import { addToastr } from 'components/shared/toastr/ToastrActions';
import React from 'react';
import { Button } from 'react-bootstrap';
import { useThunkDispatch } from 'utils/hooks';
import { ExportCoordinateModal } from './ExportCoordinateModal';
import './CoordinatesDisplay.scss';
import WKT from 'ol/format/WKT';
import { Geometry } from 'ol/geom';
import { isGeometryCollection, isPoint, isMultiPoint, isLineString, isMultiLineString, isPolygon, isMultiPolygon } from './utils';

interface Props {
    value: string;
    id?: number;
    arrangement?: boolean;
}

const getFormatData = (geometry: Geometry) => {
    const formatData = {
        coordinateCount: 0,
        objectCount: 0,
    }

    if (isGeometryCollection(geometry)) {
        const geometries = geometry.getGeometries();
        geometries.forEach((geometry) => {
            const { coordinateCount, objectCount } = getFormatData(geometry)
            formatData.coordinateCount += coordinateCount;
            formatData.objectCount += objectCount;
        })
    }
    else if (isPoint(geometry)) {
        formatData.coordinateCount += 1;
        formatData.objectCount += 1;
    }
    else if (isMultiPoint(geometry)) {
        const coordinates = geometry.getCoordinates();
        formatData.coordinateCount += coordinates.length;
        formatData.objectCount += coordinates.length;
    }
    else if (isLineString(geometry)) {
        const coordinates = geometry.getCoordinates();
        formatData.coordinateCount += coordinates.length;
        formatData.objectCount += 1;
    }
    else if (isMultiLineString(geometry)) {
        const coordinates = geometry.getCoordinates();
        coordinates.forEach((coordinate) => {
            formatData.coordinateCount += coordinate.length;
            formatData.objectCount += 1;
        })
    }
    else if (isPolygon(geometry)) {
        const coordinates = geometry.getCoordinates();
        coordinates.forEach((polygonPart) => {
            formatData.coordinateCount += polygonPart.length;
        })
        formatData.objectCount += 1;
    }
    else if (isMultiPolygon(geometry)) {
        const coordinates = geometry.getCoordinates();
        coordinates.forEach((polygon) => {
            polygon.forEach((polygonPart) => {
                formatData.coordinateCount += polygonPart.length;
            })
            formatData.objectCount += 1;
        })
    }

    return formatData;
}

export const CoordinatesDisplay: React.FC<Props> = ({
    value,
    id,
    arrangement = false,
}) => {
    const dispatch = useThunkDispatch();

    const copyValueToClipboard = () => {
        dispatch(addToastr(i18n('global.action.copyToClipboard.finished'), undefined, undefined, "md", 3000));
        navigator.clipboard.writeText(value);
    }

    const handleInputFocus = (e: React.FocusEvent<HTMLInputElement>) => {
        e.currentTarget.select();
    }

    const showExportDialog = (id?: number) =>
        dispatch(
            modalDialogShow(
                undefined,
                i18n('ap.coordinate.export.title'),
                <ExportCoordinateModal
                    onClose={() => dispatch(modalDialogHide())}
                    itemId={id}
                    arrangement={arrangement}
                />,
            ),
        )

    const formatLabel = () => {
        const wkt = new WKT();
        const geometry = wkt.readGeometry(value);
        const geometryType = geometry.getType();
        const { objectCount, coordinateCount } = getFormatData(geometry);

        if (objectCount === 0 || coordinateCount === 1) {
            return geometryType;
        }
        if (objectCount === 1) {
            return `${geometryType} ( ${i18n("global.geometry.label.points")}: ${coordinateCount} )`;
        }
        return `${geometryType} ( ${i18n("global.geometry.label.objects")}: ${objectCount} ${i18n("global.geometry.label.points")}: ${coordinateCount} )`;
    }

    return (
        <div className="coordinates-display-wrapper">
            <PolygonShowInMap polygon={value}>
                {({ handleShowInMap }) => {
                    return <div
                        className="coordinates-input" >
                        <Button
                            variant={'action'}
                            className={classNames('side-container-button')}
                            size="sm"
                            onClick={handleShowInMap}
                        >
                            <Icon glyph="fa-map" />
                            &nbsp;
                            <span>
                                {formatLabel()}
                            </span>
                        </Button>
                    </div>
                }
                }
            </PolygonShowInMap>
            <TooltipTrigger placement="vertical" content={i18n('global.action.copyToClipboard')}>
                <Button
                    variant={'action'}
                    className={classNames('side-container-button', 'right')}
                    size="sm"
                    onClick={copyValueToClipboard}
                >
                    <Icon glyph="fa-clone" fixedWidth className="icon" />
                </Button>
            </TooltipTrigger>
            <TooltipTrigger placement="vertical" content={i18n('global.action.export')}>
                <Button
                    variant={'action'}
                    className={classNames('side-container-button', 'right')}
                    size="sm"
                    onClick={() => showExportDialog(id)}
                >
                    <Icon glyph="fa-download" fixedWidth className="icon" />
                </Button>
            </TooltipTrigger>
        </div>
    );
};
