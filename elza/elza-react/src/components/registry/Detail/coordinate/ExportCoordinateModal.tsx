import React, {ChangeEvent, useState} from 'react';
import {connect} from 'react-redux';
import {Button, Col, Form, Modal, Row} from 'react-bootstrap';
import {downloadFileInFrame} from '../../../../actions/global/download';
import {ApItemCoordinatesVO} from '../../../../api/ApItemCoordinatesVO';
import i18n from '../../../i18n';
import {UrlFactory} from '../../../../actions/WebApi';
import {CoordinateFileType} from '../../../../constants';
import {Action, Dispatch} from 'redux';

type Props = {
    item: ApItemCoordinatesVO;
    onClose: () => void;
} & ReturnType<typeof mapDispatchToProps>;

const ExportCoordinateModal = ({item, onClose, handleExport}: Props) => {
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
                <Button variant="outline-secondary" onClick={() => handleExport(item, format, onClose)}>
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
    handleExport: (item: ApItemCoordinatesVO, format: CoordinateFileType, onClose: () => void) => {
        dispatch(
            downloadFileInFrame(
                UrlFactory.exportApCoordinate(item.id as number, format),
                item.id + '-' + format,
            ) as any,
        );
        onClose();
    },
});

export default connect(null, mapDispatchToProps)(ExportCoordinateModal);
