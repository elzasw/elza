import React, {FC} from 'react';
import DetailState from './DetailState';
import {Button, Col, Row} from 'react-bootstrap';
import DetailDescriptions from "./DetailDescriptions";
import DetailDescriptionsItem from "./DetailDescriptionsItem";
//import {CodelistData} from "../../shared/reducers/codelist/CodelistTypes";
import {connect} from "react-redux";
import Icon from '../../shared/icon/Icon';
//import {BASE_API_URL} from "../../constants";
//import {faAngleDoubleDown, faAngleDoubleUp, faExclamationTriangle} from "@fortawesome/free-solid-svg-icons";
//import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
//import {formatDate, formatDateTime} from "../../dateutils";
//import ValidationResultIcon from "../ValidationResultIcon";
import ArchiveEntityName from "./ArchiveEntityName";
import {ApValidationErrorsVO} from "../../../api/ApValidationErrorsVO";
import {ApAccessPointVO} from "../../../api/ApAccessPointVO";

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
console.log('COLL', collapsed);
    return <Row className="ml-3 mt-3 mr-3 pb-3 border-bottom space-between middle">
        <Col className={'p-0'} style={{flex: 1}}>
            <h1><ArchiveEntityName name={item.name} description={item.description}/> {showValidationError()}</h1>
            <h3>{apType.name}</h3>
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
};

const mapStateToProps = (state) => ({
    apTypesMap: state.refTables.recordTypes.typeIdMap,
});

export default connect(
    mapStateToProps
)(DetailHeader);
