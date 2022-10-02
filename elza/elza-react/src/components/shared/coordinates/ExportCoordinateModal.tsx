import { downloadFileInFrame } from 'actions/global/download';
import { UrlFactory } from 'actions/WebApi';
import i18n from 'components/i18n';
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
                    <Col>{i18n('ap.coordinate.export.info')}</Col>
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
                                label={i18n('ap.coordinate.format', x.toUpperCase())}
                            />
                        ))}
                    </Col>
                </Row>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="outline-secondary" onClick={() => handleExport(itemId, arrangement, format, onClose)}>
                    {i18n('global.action.export')}
                </Button>
                <Button variant="link" onClick={onClose}>
                    {i18n('global.action.cancel')}
                </Button>
            </Modal.Footer>
        </>
    );
};
