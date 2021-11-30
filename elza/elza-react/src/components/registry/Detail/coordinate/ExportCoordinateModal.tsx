import React, {ChangeEvent, useState} from 'react';
import {connect} from 'react-redux';
import {Button, Col, Form, Modal, Row} from 'react-bootstrap';
import {downloadFileInFrame} from '../../../../actions/global/download';
import i18n from '../../../i18n';
import {UrlFactory} from '../../../../actions/WebApi';
import {CoordinateFileType} from '../../../../constants';
import {Action, Dispatch} from 'redux';

type Props = {
    itemId: number | undefined;
    arrangement: boolean
    onClose: () => void;
} & ReturnType<typeof mapDispatchToProps>;

const ExportCoordinateModal = ({itemId, arrangement, onClose, handleExport}: Props) => {
    const [format, setFormat] = useState(CoordinateFileType.KML);

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
const mapDispatchToProps = (dispatch: Dispatch<Action>) => ({
    handleExport: (itemId: number | undefined, arrangement: boolean, format: CoordinateFileType, onClose: () => void) => {
        dispatch(
            downloadFileInFrame(
                arrangement ? UrlFactory.exportArrCoordinates(itemId, format) : UrlFactory.exportApCoordinate(itemId, format),
                itemId + '-' + format,
            ) as any,
        );
        onClose();
    },
});

export default connect(null, mapDispatchToProps)(ExportCoordinateModal);
