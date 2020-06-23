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
import {Icon} from "../../index";
import {Button} from "../../ui";
import {indexById} from "../../../shared/utils";

interface Props {
    item: ApAccessPointVO;
    onToggleCollapsed?: () => void;
    collapsed: boolean;
    id?: number;
    validationResult?: ApValidationErrorsVO;
    apTypesMap: object;
    scopes: any;
}

const formatDate = (a: any, ...other) => a;
const formatDateTime = (a: any, ...other) => a;

const DetailHeader: FC<Props> = ({item, id, collapsed, onToggleCollapsed, validationResult, apTypesMap, scopes}) => {
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
        <Row className={collapsed ? 'ml-3 mt-1 mr-3 pb-1' : 'ml-3 mt-3 mr-3 pb-3 space-between middle'}>
            {collapsed && <Col className={'p-0'} style={{flex: 1}}>
                <h4 className={'m-0'}>
                    <ArchiveEntityName name={item.name} description={item.description}/> {showValidationError()}
                </h4>
            </Col>}

            {!collapsed && <Col className={'p-0'} style={{flex: 1}}>
                <div className={'d-inline-block mr-3 mt-3 pull-left'}>
                    <Icon glyph={'fa-file-o'} className={'fa-4x'}/>
                </div>
                <div className={'d-inline-block'}>
                    <h1 className={'m-0'}>
                        <ArchiveEntityName name={item.name} description={item.description}/> {showValidationError()}
                    </h1>
                    <h4>{apType.name}</h4>
                    <DetailDescriptions>
                        {id && <DetailDescriptionsItem label="ID:">{id}</DetailDescriptionsItem>}
                        {item.state && <DetailDescriptionsItem>
                            <DetailState state={item.stateApproval}/>
                        </DetailDescriptionsItem>}
                        {item.scopeId && scopes[item.scopeId] && <DetailDescriptionsItem>
                            <Icon glyph={'fa-globe'} className={'mr-1'}/>
                            {scopes[item.scopeId].name}
                        </DetailDescriptionsItem>}
                        {item.lastChange && item.lastChange.user && <DetailDescriptionsItem label="Upravil:">
                            {item.lastChange.user.displayName}
                            <span title={'Upraveno ' + formatDateTime(item.lastChange.change, {})}>
                                ({formatDate(item.lastChange.change)})
                            </span>
                        </DetailDescriptionsItem>}
                    </DetailDescriptions>
                </div>
            </Col>}
            <Col>
                <Button onClick={onToggleCollapsed} variant={'light'} style={{position: 'absolute', right: 0, bottom: 0}}
                        title={collapsed ? "Zobrazit podrobnosti" : "Skrýt podrobnosti"}>
                    <Icon className={''} glyph={collapsed ? 'fa-angle-double-down' : 'fa-angle-double-up'}/>
                </Button>
            </Col>
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

const mapStateToProps = (state) => {
    const scopesData = state.refTables.scopesData;
    const id = scopesData && indexById(scopesData.scopes, -1, 'versionId'); // všechny scope
    let scopes = [];
    if (id !== null) {
        scopes = scopesData.scopes[id].scopes;
    }

    return {
        apTypesMap: state.refTables.recordTypes.typeIdMap,
        scopes: scopes,
    }
};

export default connect(
    mapStateToProps
)(DetailHeader);
