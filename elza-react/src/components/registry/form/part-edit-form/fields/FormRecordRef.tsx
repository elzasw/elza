import React, {FC} from 'react';
import {Icon} from '../../../../index';
import {Button, Col, Form, Row} from 'react-bootstrap';
import {RulDescItemTypeExtVO} from '../../../../../api/RulDescItemTypeExtVO';
import {RulDataTypeVO} from '../../../../../api/RulDataTypeVO';
import {ApItemAccessPointRefVO} from '../../../../../api/ApItemAccessPointRefVO';
import {objectById} from '../../../../../shared/utils';
import { RulDescItemSpecExtVO } from 'api/RulDescItemSpecExtVO';

const getDisplayValue = (apItem: ApItemAccessPointRefVO, itemType: RulDescItemTypeExtVO) => {
    const apItemName = apItem.accessPoint?.name;

    if (itemType.useSpecification && apItem.specId) {
        const spec:RulDescItemSpecExtVO | null = objectById(itemType.descItemSpecs, apItem.specId) || null;
        return `${spec?.shortcut}: ${ apItemName }`
    } 

    return apItemName;
}

export const FormRecordRef:FC<{
    name: string;
    label: string;
    disabled: boolean;
    item: ApItemAccessPointRefVO;
    itemType: RulDescItemTypeExtVO;
    dataType: RulDataTypeVO;
    onEdit: (name: string, dataTypeCode: string, item: ApItemAccessPointRefVO) => void;
}> = ({
    name,
    label,
    disabled,
    item,
    itemType,
    dataType,
    onEdit,
}) => {
    return (
        <Row className={'d-flex'}>
            <Col>
                <Form.Label>{label}</Form.Label>
                <Form.Control value={getDisplayValue(item, itemType)} disabled={true} />
            </Col>
            <Col xs="auto" className="action-buttons">
                <Button
                    disabled={disabled}
                    variant={'action' as any}
                    onClick={() => onEdit(name, dataType.code, item)}
                >
                    <Icon glyph="fa-edit" />
                </Button>
            </Col>
        </Row>
    );
}

