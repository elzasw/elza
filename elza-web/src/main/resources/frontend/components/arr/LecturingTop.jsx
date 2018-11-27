import * as React from 'react';
import {connect} from 'react-redux';
import {DropdownButton, MenuItem, Button, Row, Col, FormControl} from "react-bootstrap";
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

import "./LecturingTop.less";
import * as perms from "../../actions/user/Permission";

const basicOptionMap = (i) => <option key={i.id} value={i.id}>{i.name}</option>;

/**
 * Horní část lektoringu, zajištující seznam protokolů, seznam připomínek a nastavení
 */
class LecturingTop extends React.Component {

    state = {
        issueListId: null,
        issueId: null
    };

    static propTypes = {
        fund: React.PropTypes.object.isRequired,
    };

    componentDidMount() {
        this.props.dispatch(issueTypesActions.fetchIfNeeded());
        this.props.dispatch(issueStatesActions.fetchIfNeeded());
        this.props.dispatch(issuesActions.protocols.fetchIfNeeded(this.props.fund.id));
    }

    componentWillReceiveProps(nextProps, nextContext) {
        const {issueListId, issueId} = this.state;
        nextProps.dispatch(issueTypesActions.fetchIfNeeded());
        nextProps.dispatch(issueStatesActions.fetchIfNeeded());
        nextProps.dispatch(issuesActions.protocols.fetchIfNeeded(this.props.fund.id));
        if (nextProps.issueProtocols.fetched && nextProps.issueProtocols.count) {
            if (!issueListId) {
                const newIssueListId = nextProps.issueProtocols.rows[0].id;
                this.selectIssueList(newIssueListId);
            } else {
                nextProps.dispatch(issuesActions.protocol.fetchIfNeeded(issueListId));
                nextProps.dispatch(issuesActions.list.fetchIfNeeded(issueListId));
            }
        }
        if (issueId) {
            nextProps.dispatch(issuesActions.detail.fetchIfNeeded(issueId))
        }
    }

    filter = (filterPart) => {
        this.props.dispatch(issuesActions.list.filter({...this.props.issueList.filter, ...filterPart}));
    };

    settings = () => {
        this.props.dispatch(modalDialogShow(this, i18n("arr.issues.settings.title"), <IssueLists fundId={this.props.fund.id} />));
    };

    download = () => {
        this.props.dispatch(downloadFile(UrlFactory.exportIssueList(this.props.issueList.filter.protocol)));
    };

    newArr = () => {
        this.props.dispatch(modalDialogShow(this, i18n("arr.issues.add.arr.title"), <IssueForm onSubmit={(data) => WebApi.addIssue({
            ...data,
            issueListId: this.state.issueListId,
            nodeId: null
        })}  onSubmitSuccess={this.afterAdd} />));
    };

    newNode = () => {
        this.props.dispatch(modalDialogShow(this, i18n("arr.issues.add.node.title"), <IssueForm onSubmit={(data) => WebApi.addIssue({
            ...data,
            issueListId: this.state.issueListId,
            nodeId: this.props.node.id
        })}  onSubmitSuccess={this.afterAdd} />));
    };

    afterAdd = (data) => {
        const {dispatch} = this.props;
        dispatch(issuesActions.list.invalidate(data.issueListId));
        dispatch(issuesActions.detail.invalidate(data.id));
        dispatch(modalDialogHide());
    }

    selectIssue = ([index]) => {
        const issueId = this.props.issueList.rows[index].id;
        this.setState({issueId});
        this.props.dispatch(issuesActions.detail.fetchIfNeeded(issueId))
    };

    selectIssueList = (issueListId) => {
        this.setState({issueListId}, () => {
            this.props.dispatch(issuesActions.protocol.fetchIfNeeded(issueListId));
            this.props.dispatch(issuesActions.list.fetchIfNeeded(issueListId));
        });
    };

    updateIssueState = (id, issueStateId) => {
        const {issueListId} = this.state;
        WebApi.setIssueState(id, issueStateId).then(() => {
            this.props.dispatch(issuesActions.list.invalidate(issueListId));
            this.props.dispatch(issuesActions.detail.invalidate(id));
        });
    };

    render() {
        const {issueTypes, issueStates, issueList, issueProtocols, userDetail} = this.props;
        const {issueListId, issueId} = this.state;
        const activeIndex = issueId !== null ? indexById(issueList.rows, issueId) : null;

        const hasAdmin = userDetail.hasOne([perms.FUND_ISSUE_ADMIN_ALL]);

        const canWrite = !!issueListId && (
             hasAdmin || (
                userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR] &&
                userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR].issueListIds &&
                userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR].issueListIds.indexOf(issueListId) !== -1
            )
        );

        return <div className="lecturing-top">
            <div className="actions-container">
                <div className="actions">
                    <DropdownButton disabled={!canWrite} bsStyle="default" id='dropdown-add-comment' noCaret title={<Icon glyph='fa-plus-circle' />}>
                        <MenuItem eventKey="1" onClick={this.newArr}>{i18n("arr.issues.add.arr")}</MenuItem>
                        <MenuItem eventKey="2" onClick={this.newNode}>{i18n("arr.issues.add.node")}</MenuItem>
                    </DropdownButton>
                    {hasAdmin && <Button bsStyle="action" className="pull-right" onClick={this.settings}><Icon glyph='fa-cogs' /></Button>}
                    <Button bsStyle="action" className="pull-right" disabled={!issueListId} onClick={this.download}><Icon glyph='fa-download' /></Button>
                </div>
            </div>
            <FormControl componentClass={"select"} name={"protocol"} onChange={({target: {value}}) => this.selectIssueList(value)} value={issueListId}>
                {issueProtocols.fetched && issueProtocols.count === 0 && <option value={null} />}
                {issueProtocols.fetched && issueProtocols.rows.map(basicOptionMap)}
            </FormControl>
            <Row>
                <Col xs={12} sm={6}>
                    <FormControl componentClass={"select"} name={"state"} onChange={({target: {value}}) => this.filter({state:value})} value={issueList.filter.state}>
                        <option value={""}>{i18n("global.all")}</option>
                        {issueStates.fetched && issueStates.data.map(basicOptionMap)}
                    </FormControl>
                </Col>
                <Col xs={12} sm={6}>
                    <FormControl componentClass={"select"} name={"type"} onChange={({target: {value}}) => this.filter({type:value})} value={issueList.filter.type}>
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
                    renderItemContent={({item: {description, issueStateId, issueTypeId, number, id}, active}) => {
                        const state : IssueStateVO = objectById(issueStates.data, issueStateId);
                        const type = objectById(issueProtocols.data, issueTypeId);
                        // TODO lectoring @compel co s typem, jak tvořit kolečka a barvy + co context menu
                        return <TooltipTrigger className={"flex item"  + (active ? " active" : "")} content={<span><div>#{number} ({state.name})</div><div>{description}</div></span>}>
                            <div className={"flex-1"}>
                            <div>
                                <span className={"circle"}>
                                {state.finalState && (<Icon glyph={false ? "fa-check" : "fa-cross"}/>)}
                                </span>
                                #{number} - {description}
                            </div>
                            </div>
                            {canWrite && <div className="actions">
                                <DropdownButton pullRight bsStyle="action" id='issue-state' noCaret title={<Icon glyph='fa-ellipsis-h' />}>
                                    {issueStates.data.map(i => <MenuItem key={'state-' + i.id} disabled={i.id === issueStateId} onClick={this.updateIssueState.bind(this, id, i.id)}>{i.name}</MenuItem>)}
                                </DropdownButton>
                            </div>}
                        </TooltipTrigger>
                    }}
                />
            </div>
        </div>
    }
}

export default connect((state) => {
    return {
        issueTypes: state.refTables.issueTypes,
        issueStates: state.refTables.issueStates,
        issueList: storeFromArea(state, issuesActions.AREA_LIST),
        issueProtocols: storeFromArea(state, issuesActions.AREA_PROTOCOLS),
        userDetail: state.userDetail
    }
})(LecturingTop);
