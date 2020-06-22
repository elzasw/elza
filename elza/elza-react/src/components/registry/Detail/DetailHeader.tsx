import React, {FC} from 'react';
import DetailState from './DetailState';
import {Col, Row} from 'react-bootstrap';
import DetailDescriptions from "./DetailDescriptions";
import DetailDescriptionsItem from "./DetailDescriptionsItem";
import {connect} from "react-redux";
import ArchiveEntityName from "./ArchiveEntityName";
import {ApValidationErrorsVO} from "../../../api/ApValidationErrorsVO";
import {ApAccessPointVO} from "../../../api/ApAccessPointVO";
import './DetailHeader.scss';

interface Props {
    item: ApAccessPointVO;
    onToggleCollapsed?: () => void;
    collapsed: boolean;
    id?: number;
    validationResult?: ApValidationErrorsVO;
    apTypesMap: object;
}

const formatDate = (a: any, ...other) => a;
const formatDateTime = (a: any, ...other) => a;

const DetailHeader: FC<Props> = ({item, id, collapsed, onToggleCollapsed, validationResult, apTypesMap}) => {
    const apType = apTypesMap[item.typeId];

    const showValidationError = () => {
        if (validationResult && validationResult.errors && validationResult.errors.length > 0) {
            return <Col>
                ValidationResultIcon message={validationResult.errors}
            </Col>;
        }
    };

    const renderApTypeNames = (delimiter = '>') => {
        let elements: JSX.Element[] = [];

        if (apType.parents) {
            apType.parents.reverse().forEach((name, i) => {
                elements.push(
                    <span key={'name-' + i} className="hierarchy-level">
                        {name.toUpperCase()}
                    </span>
                );
                elements.push(
                    <span key={'delimiter-' + i} className="hierarchy-delimiter">
                        {delimiter}
                    </span>,
                );
            });
        }
        elements.push(
            <span key="name-main" className="hierarchy-level main">
                {apType.name.toUpperCase()}
            </span>,
        );

        return elements;
    };


    return <div className={'detail-header-wrapper'}>
        <Row className="ml-3 mt-3 mr-3 pb-3 space-between middle">
            <Col className={'p-0'} style={{flex: 1}}>
                <h1><ArchiveEntityName name={item.name} description={item.description}/> {showValidationError()}</h1>
                <DetailDescriptions className="mt-1">
                    {item.state && <DetailDescriptionsItem>
                        <DetailState state={item.stateApproval}/>
                    </DetailDescriptionsItem>}
                    {id && <DetailDescriptionsItem label="ID:">
                        {id}
                    </DetailDescriptionsItem>}
                    {item.lastChange && item.lastChange.user && <DetailDescriptionsItem label="Upravil:">
                        {item.lastChange.user.displayName} <span
                        title={'Upraveno ' + formatDateTime(item.lastChange.change, {})}>({formatDate(item.lastChange.change)})</span>
                    </DetailDescriptionsItem>}
                </DetailDescriptions>
            </Col>
            {/*<Col>*/}
            {/*    <Button onClick={onToggleCollapsed} size="sm" className={'btn-secondary'}*/}
            {/*            title={collapsed ? "Zobrazit podrobnosti" : "Skrýt podrobnosti"}>*/}
            {/*        <Icon className="icon" glyph={collapsed ? 'fa-angle-double-down' : 'fa-angle-double-up'}/>*/}
            {/*    </Button>*/}
            {/*</Col>*/}
        </Row>
        <Row>
            <Col className={'ap-type'}>
                <div className={'p-1 pl-3'}>
                    {renderApTypeNames()}
                </div>
            </Col>
        </Row>
    </div>
};

const mapStateToProps = (state) => ({
    apTypesMap: state.refTables.recordTypes.typeIdMap,
});

export default connect(
    mapStateToProps
)(DetailHeader);
