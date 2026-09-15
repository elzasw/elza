import { downloadFileInFrame } from 'actions/global/download';
import { UrlFactory } from 'actions/WebApi';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    exportInfo: {
        id: 'ap.coordinate.export.info',
        defaultMessage: 'Zvolte požadovaný formát exportovaných souřadnic:',
    },
    format: { id: 'ap.coordinate.format', defaultMessage: 'Formát {0}' },
    exportAction: { id: 'global.action.export', defaultMessage: 'Exportovat' },
});
import React, { ChangeEvent, useState } from 'react';
import { Button, Col, Form, Modal, Row } from 'react-bootstrap';
import { useThunkDispatch } from 'utils/hooks';
import { CoordinateFileType } from '../../../constants';

type Props = {
    itemId: number | undefined;
    arrangement: boolean
    onClose: () => void;
};

export const ExportCoordinateModal = ({itemId, arrangement, onClose}: Props) => {
    const [format, setFormat] = useState(CoordinateFileType.KML);
    const dispatch = useThunkDispatch();
    const intl = useIntl();

    const handleExport = (itemId: number | undefined, arrangement: boolean, format: CoordinateFileType, onClose: () => void) => {
        dispatch(
            downloadFileInFrame(
                arrangement ? UrlFactory.exportArrCoordinates(itemId, format) : UrlFactory.exportApCoordinate(itemId, format),
                itemId + '-' + format,
            ) as any,
        );
        onClose();
    }

    const onChange = (e: ChangeEvent<HTMLInputElement>) => {
        setFormat(e.target.value as CoordinateFileType);
    };

    return (
        <>
            <Modal.Body>
                <Row>
                    <Col><FormattedMessage {...messages.exportInfo} /></Col>
                </Row>
                <Row className="pt-2">
                    <Col>
                        {Object.keys(CoordinateFileType).map(x => (
                            <Form.Check
                                type="radio"
                                name={'format'}
                                checked={format === x}
                                value={x}
                                onChange={onChange}
                                label={intl.formatMessage(messages.format, { 0: x.toUpperCase() })}
                            />
                        ))}
                    </Col>
                </Row>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="outline-secondary" onClick={() => handleExport(itemId, arrangement, format, onClose)}>
                    <FormattedMessage {...messages.exportAction} />
                </Button>
                <Button variant="link" onClick={onClose}>
                    <FormattedMessage {...globalMessages.cancel} />
                </Button>
            </Modal.Footer>
        </>
    );
};
