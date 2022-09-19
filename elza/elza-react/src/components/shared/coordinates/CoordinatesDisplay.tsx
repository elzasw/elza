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

interface Props {
    value: string;
    id?: number;
    arrangement?: boolean;
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

    return (
        <div className="coordinates-display-wrapper">
            <PolygonShowInMap polygon={value}>
                {({handleShowInMap}) => 
            {
                    return <div
                        className="coordinates-input" >
                        <Button
                            variant={'action'}
                            className={classNames('side-container-button')}
                            size="sm"
                            onClick={handleShowInMap}
                        >
                            <Icon glyph="fa-map"/>
                        </Button>
                        <input 
                            readOnly={true}
                            value={value}
                            onFocus={handleInputFocus}
                            />
                    </div>}
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
