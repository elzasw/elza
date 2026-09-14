import React, {ChangeEvent, useState} from 'react';
import {connect} from 'react-redux';
import {Button, Col, Form, Modal, Row} from 'react-bootstrap';
import {downloadFileInFrame} from '../../../../actions/global/download';
import { FormattedMessage, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { apDetailMessages } from '../messages';
import {UrlFactory} from '../../../../actions/WebApi';
import {CoordinateFileType} from '../../../../constants';
import {Action, Dispatch} from 'redux';

type Props = {
    itemId: number | undefined;
    arrangement: boolean
    onClose: () => void;
} & ReturnType<typeof mapDispatchToProps>;

const ExportCoordinateModal = ({itemId, arrangement, onClose, handleExport}: Props) => {
    const intl = useIntl();
    const [format, setFormat] = useState(CoordinateFileType.KML);

    const onChange = (e: ChangeEvent<HTMLInputElement>) => {
        setFormat(e.target.value as CoordinateFileType);
    };

    return (
        <>
            <Modal.Body>
                <Row>
                    <Col>{<FormattedMessage {...apDetailMessages.coordinateExportInfo} />}</Col>
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
                                label={intl.formatMessage(apDetailMessages.coordinateFormat, { 0: x.toUpperCase() })}
                            />
                        ))}
                    </Col>
                </Row>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="outline-secondary" onClick={() => handleExport(itemId, arrangement, format, onClose)}>
                    {<FormattedMessage {...apDetailMessages.globalActionExport} />}
                </Button>
                <Button variant="link" onClick={onClose}>
                    {<FormattedMessage {...globalMessages.cancel} />}
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
