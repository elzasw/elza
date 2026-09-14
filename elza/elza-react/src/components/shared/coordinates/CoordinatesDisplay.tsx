import { modalDialogHide, modalDialogShow } from 'actions/global/modalDialog';
import classNames from 'classnames';
import { defineMessages } from 'react-intl';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    exportTitle: { id: 'ap.coordinate.export.title', defaultMessage: 'Export souřadnic' },
    labelPoints: { id: 'global.geometry.label.points', defaultMessage: 'Body' },
    labelObjects: { id: 'global.geometry.label.objects', defaultMessage: 'Obrazce' },
    undefinedValue: { id: 'subNodeForm.descItemType.undefinedValue', defaultMessage: 'výjimka' },
    exportAction: { id: 'global.action.export', defaultMessage: 'Exportovat' },
});
import { PolygonShowInMap } from "components/PolygonShowInMap";
import { TooltipTrigger } from 'components/shared';
import Icon from 'components/shared/icon/Icon';
import { addToastr, addToastrDanger, addToastrInfo } from 'components/shared/toastr/ToastrActions';
import React from 'react';
import { Button } from 'react-bootstrap';
import { copyTextToClipboard } from 'utils/clipboard';
import { useThunkDispatch } from 'utils/hooks';
import { ExportCoordinateModal } from './ExportCoordinateModal';
import './CoordinatesDisplay.scss';
import WKT from 'ol/format/WKT';
import { getGeometryFormatData } from './utils';
import { useIntl } from 'react-intl';
import { globalMessages } from '../lang';

interface Props {
    value: string;
    id?: number;
    arrangement?: boolean;
    isUndefined?: boolean;
  isInherited?: boolean;
  isInhibited?: boolean;
}

export const CoordinatesDisplay: React.FC<Props> = ({
    value,
    id,
    arrangement = false,
    isUndefined = false,
    isInherited,
    isInhibited,
}) => {
    const dispatch = useThunkDispatch();
    const { formatMessage } = useIntl()

    const copyValueToClipboard = async () => {
        const copied = await copyTextToClipboard(value);
        dispatch(
            copied
                ? addToastrInfo(formatMessage({...globalMessages.copyToClipboardFinished}))
                : addToastrDanger(formatMessage({...globalMessages.copyToClipboardUnavailable})),
        );
    };

    const handleInputFocus = (e: React.FocusEvent<HTMLInputElement>) => {
        e.currentTarget.select();
    }

    const showExportDialog = (id?: number) =>
        dispatch(
            modalDialogShow(
                undefined,
                formatMessage(messages.exportTitle),
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
        const { objectCount, coordinateCount } = getGeometryFormatData(geometry);

        if (objectCount === 0 || coordinateCount === 1) {
            return geometryType;
        }
        if (objectCount === 1) {
            return `${geometryType} ( ${formatMessage(messages.labelPoints)}: ${coordinateCount} )`;
        }
        return `${geometryType} ( ${formatMessage(messages.labelObjects)}: ${objectCount} ${formatMessage(messages.labelPoints)}: ${coordinateCount} )`;
    }

    if (value == undefined) {
        return <></>
    }

    if (isUndefined) {
        return <span>{formatMessage(messages.undefinedValue)}</span>
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
                            <span style={{
                              textDecoration: isInhibited ? "line-through" : undefined,
                              opacity: isInherited ? 0.5 : undefined,
                            }}>
                                {formatLabel()}
                            </span>
                        </Button>
                    </div>
                }
                }
            </PolygonShowInMap>
            <TooltipTrigger placement="vertical" content={formatMessage(globalMessages.copyToClipboard)}>
                <Button
                    variant={'action'}
                    className={classNames('side-container-button', 'right')}
                    size="sm"
                    onClick={copyValueToClipboard}
                >
                    <Icon glyph="fa-clone" fixedWidth className="icon" />
                </Button>
            </TooltipTrigger>
            <TooltipTrigger placement="vertical" content={formatMessage(messages.exportAction)}>
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
