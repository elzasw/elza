import PropTypes from 'prop-types';
import * as React from 'react';
import {connect} from 'react-redux';
import {DropdownButton, Dropdown, Button, Row, Col, FormControl} from "react-bootstrap";
import TooltipTrigger from "../shared/tooltip/TooltipTrigger";
import Icon from "../shared/icon/Icon";
import i18n from "../i18n";
import * as issueTypesActions from '../../actions/refTables/issueTypes'
import * as issueStatesActions from '../../actions/refTables/issueStates'
import * as issuesActions from '../../actions/arr/issues'
import ListBox from "../shared/listbox/ListBox";
import storeFromArea from "../../shared/utils/storeFromArea";
import {modalDialogHide, modalDialogShow} from "../../actions/global/modalDialog";
import IssueLists from "./IssueLists";
import {downloadFile} from "../../actions/global/download";
import {UrlFactory, WebApi} from "../../actions/WebApi";
import IssueForm from "../form/IssueForm";
import objectById from "../../shared/utils/objectById";
import indexById from "../../shared/utils/indexById";

import "./LecturingTop.scss";
import * as perms from "../../actions/user/Permission";
import {IssueStateVO, IssueVO} from "../../types";
import {CSSProperties} from "react";

const basicOptionMap = (i) => <option key={i.id} value={i.id}>{i.name}</option>;

/**
 * Horní část lektoringu, zajištující seznam protokolů, seznam připomínek a nastavení
 */
class LecturingTop extends React.Component {

    props: any;

    state: any = {
        issueListId: null,
    };

    static propTypes = {
        fund: PropTypes.object.isRequired,
    };

    componentDidMount() {
        this.props.dispatch(issueTypesActions.fetchIfNeeded());
        this.props.dispatch(issueStatesActions.fetchIfNeeded());
        this.props.dispatch(issuesActions.protocols.fetchIfNeeded(this.props.fund.id));
        const fundId = this.props.issueProtocols.filter.fundId;
        const issueListId = this.props.issueProtocols.filter.issueListId;
        if (fundId !== null && fundId !== this.props.fund.id) {
            this.props.dispatch(issuesActions.list.reset());
        } else if (issueListId) {
            this.setState({issueListId});
            this.props.dispatch(issuesActions.list.fetchIfNeeded(issueListId));
        }
    }

    UNSAFE_componentWillReceiveProps(nextProps, nextContext) {
        const {issueListId} = this.state;
        nextProps.dispatch(issueTypesActions.fetchIfNeeded());
        nextProps.dispatch(issueStatesActions.fetchIfNeeded());
        nextProps.dispatch(issuesActions.protocols.fetchIfNeeded(this.props.fund.id));
        if (nextProps.fund.id !== this.props.fund.id) {
            this.props.dispatch(issuesActions.list.reset());
        }
        if (nextProps.issueProtocols.fetched && nextProps.issueProtocols.count > 0) {
            if (!issueListId) {
                const newIssueListId = nextProps.issueProtocols.rows[0].id;
                this.selectIssueList(newIssueListId, nextProps.fund.id);
            } else {
                nextProps.dispatch(issuesActions.protocol.fetchIfNeeded(issueListId));
                nextProps.dispatch(issuesActions.list.fetchIfNeeded(issueListId));
            }
        }
    }

    filter = (filterPart) => {
        this.props.dispatch(issuesActions.list.filter({...this.props.issueList.filter, ...filterPart}));
    };

    settings = () => {
        this.props.dispatch(modalDialogShow(this, i18n("arr.issues.settings.title"), <IssueLists fundId={this.props.fund.id} />));
    };

    download = () => {
        this.props.dispatch(downloadFile(UrlFactory.exportIssueList(this.state.issueListId)));
    };

    newArr = () => {
        this.props.dispatch(modalDialogShow(this, i18n("arr.issues.add.arr.title"), <IssueForm onSubmit={(data) => WebApi.addIssue({
            ...data,
            issueListId: this.state.issueListId,
            nodeId: null
        })}  onSubmitSuccess={this.afterAdd} />));
    };

    newNode = () => {
        if (!this.props.node || !this.props.node.selectedSubNodeId) {
            return;
        }
        this.props.dispatch(modalDialogShow(this, i18n("arr.issues.add.node.title"), <IssueForm onSubmit={(data) => WebApi.addIssue({
            ...data,
            issueListId: this.state.issueListId,
            nodeId: this.props.node.selectedSubNodeId
        })}  onSubmitSuccess={this.afterAdd} />));
    };

    afterAdd = (data) => {
        const {dispatch} = this.props;
        dispatch(issuesActions.list.invalidate(data.issueListId));
        dispatch(issuesActions.detail.invalidate(data.id));
        dispatch(modalDialogHide());
    };

    selectIssue = ([index]) => {
        const issueId = this.props.issueList.rows[index].id;
        this.props.dispatch(issuesActions.detail.select(issueId));
    };

    selectIssueList = (issueListId, fundId) => {
        this.setState({issueListId}, () => {
            this.props.dispatch(issuesActions.protocol.fetchIfNeeded(issueListId));
            this.props.dispatch(issuesActions.protocols.filter({issueListId, fundId}));
            this.props.dispatch(issuesActions.list.fetchIfNeeded(issueListId));
        });
    };

    updateIssueType = (id, issueTypeId) => {
        const {issueListId} = this.state;
        WebApi.setIssueType(id, issueTypeId).then(() => {
            this.props.dispatch(issuesActions.list.invalidate(issueListId));
            this.props.dispatch(issuesActions.detail.invalidate(id));
        });
    };

    render() {
        const {fund, issueTypes, issueStates, issueList, issueProtocols, userDetail, issueDetail} = this.props;
        const {issueListId} = this.state;
        const issueId = issueDetail.id;
        const activeIndex = issueId !== null ? indexById(issueList.rows, issueId) : null;

        const hasAdmin = userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL);

        const canWrite = !!issueListId && (
             hasAdmin || (
                userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR] &&
                userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR].issueListIds &&
                userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR].issueListIds.indexOf(issueListId) !== -1
            )
        );

        const config = fund.activeVersion.config;

        return <div className="lecturing-top">
            <div className="actions-container">
                <div className="actions">
                    <DropdownButton disabled={!canWrite} variant="default" id='dropdown-add-comment' noCaret title={(<Icon glyph='fa-plus-circle' /> as any as string)}>
                        <Dropdown.Item eventKey="1" onClick={this.newArr}>{i18n("arr.issues.add.arr")}</Dropdown.Item>
                        <Dropdown.Item eventKey="2" disabled={!this.props.node || !this.props.node.selectedSubNodeId} onClick={this.newNode}>{i18n("arr.issues.add.node")}</Dropdown.Item>
                    </DropdownButton>
                    {hasAdmin && <Button variant={"action" as any} className="pull-right" onClick={this.settings}><Icon glyph='fa-cogs' /></Button>}
                    <Button variant={"action" as any} className="pull-right" disabled={!issueListId} onClick={this.download}><Icon glyph='fa-download' /></Button>
                </div>
            </div>
            <FormControl as={"select"} name={"protocol"} onChange={({target: {value}}: any) => this.selectIssueList(value, fund.id)} value={issueListId}>
                {issueProtocols.fetched && issueProtocols.count === 0 && <option value={''} />}
                {issueProtocols.fetched && issueProtocols.rows.map(basicOptionMap)}
            </FormControl>
            <Row>
                <Col xs={12} sm={6}>
                    <FormControl as={"select"} name={"state"} disabled={!issueListId} onChange={({target: {value}}: any) => this.filter({state:value})} value={issueList.filter.state}>
                        <option value={""}>{i18n("global.all")}</option>
                        {issueStates.fetched && issueStates.data.map(basicOptionMap)}
                    </FormControl>
                </Col>
                <Col xs={12} sm={6}>
                    <FormControl as={"select"} name={"type"} disabled={!issueListId} onChange={({target: {value}}: any) => this.filter({type:value})} value={issueList.filter.type}>
                        <option value={""}>{i18n("global.all")}</option>
                        {issueTypes.fetched && issueTypes.data.map(basicOptionMap)}
                    </FormControl>
                </Col>
            </Row>

            <div className='list-container'>
                <ListBox
                    ref="listBox"
                    activeIndex={activeIndex}
                    onChangeSelection={this.selectIssue}
                    items={issueList.rows}
                    renderItemContent={({item: {description, issueStateId, issueTypeId, number, id, referenceMark}, active} : {item: IssueVO, active: boolean}) => {
                        const state : IssueStateVO = objectById(issueStates.data, issueStateId);
                        const type = objectById(issueTypes.data, issueTypeId);
                        const style: CSSProperties = {};
                        if (config.colors && config.colors[type.code]) {
                            style.background = config.colors[type.code];
                        }
                        let icon = null;
                        if (config.icons && config.icons[state.code]) {
                            icon = config.icons[state.code];
                        }
                        return <TooltipTrigger className={"flex item"  + (active ? " active" : "")} content={<span><div>#{number} ({state.name})</div><div>{description}</div></span>}>
                            <div className={"info"}>
                                <div className="flex">
                                    <span className="circle" style={!state.finalState ? style : undefined}>
                                    {state.finalState && icon && <Icon glyph={icon}/>}
                                    </span>
                                    <span className={"description"}>#{number} - {description}</span>
                                </div>
                                <div className="reference-mark">
                                    {referenceMark && referenceMark.join(" ")}
                                </div>
                            </div>
                            {canWrite && <div className="actions">
                                <DropdownButton pullRight variant={"action" as any} id='issue-type' noCaret title={<Icon glyph='fa-ellipsis-h' /> as any as string}>
                                    {issueTypes.data.map(i => <Dropdown.Item key={'issue-type-' + i.id} disabled={i.id === issueTypeId} onClick={this.updateIssueType.bind(this, id, i.id)}>{i18n("arr.issues.type.change", i.name)}</Dropdown.Item>)}
                                </DropdownButton>
                            </div>}
                        </TooltipTrigger>
                    }}
                />
            </div>
        </div>
    }
}

export default connect((state: any) => {
    return {
        issueTypes: state.refTables.issueTypes,
        issueStates: state.refTables.issueStates,
        issueList: storeFromArea(state, issuesActions.AREA_LIST),
        issueProtocols: storeFromArea(state, issuesActions.AREA_PROTOCOLS),
        issueDetail: storeFromArea(state, issuesActions.AREA_DETAIL),
        userDetail: state.userDetail,
    }
})(LecturingTop as any) as any as React.SFC<{fund: object}>;
