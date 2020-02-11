import PropTypes from 'prop-types';
import * as React from 'react';
import {connect} from 'react-redux';
import {DropdownButton, MenuItem, Button} from "react-bootstrap";
import Icon from "../shared/icon/Icon";
import * as issuesActions from '../../actions/arr/issues'
import storeFromArea from "../../shared/utils/storeFromArea";
import {WebApi} from "../../actions/WebApi";
import objectById from "../../shared/utils/objectById";
import Loading from "../shared/loading/Loading";
import {dateTimeToString} from "../Utils";
import TextareaAutosize from 'react-autosize-textarea';

import "./LecturingBottom.less"
import i18n from "../i18n";
import {modalDialogHide, modalDialogShow} from "../../actions/global/modalDialog";
import IssueForm from "../form/IssueForm";
import * as perms from "../../actions/user/Permission";


/**
 * Spodní část lektoringu, zajištující seznam komentářů, komentování a editaci
 */
class LecturingBottom extends React.Component {

    static propTypes = {
        fund: PropTypes.object.isRequired,
    };

    state: any = {
        text: "",
        comment: null,
        submitting: false,
        actualFundId: null
    };

    props: any;

    componentDidMount() {
        const {issueDetail, fund} = this.props;
        if (issueDetail && issueDetail.id) {
            this.props.dispatch(issuesActions.detail.fetchIfNeeded(issueDetail.id));
            this.props.dispatch(issuesActions.comments.fetchIfNeeded(issueDetail.id));
        }

        const fundId = this.props.issueProtocols.filter.fundId;
        if (fundId !== null && fundId !== fund.id) {
            this.props.dispatch(issuesActions.detail.reset());
            this.props.dispatch(issuesActions.comments.reset());
        }
    }

    componentWillReceiveProps(nextProps, nextContext) {
        const {issueDetail, fund} = nextProps;

        if (issueDetail && issueDetail.id) {
            this.props.dispatch(issuesActions.detail.fetchIfNeeded(issueDetail.id));
            this.props.dispatch(issuesActions.comments.fetchIfNeeded(issueDetail.id));
        }

        if (fund.id !== this.props.fund.id) {
            this.props.dispatch(issuesActions.detail.reset());
            this.props.dispatch(issuesActions.comments.reset());
        }
    }

    addComment = (nextStateId) => {
        const {issueDetail:{id}} = this.props;
        this.setState({submitting: true});
        WebApi.addIssueComment({issueId: id, comment: this.state.text, nextStateId}).then(this.afterSave)
    };

    editComment = (comment: any) => {
        this.setState({comment, text: comment.comment});
    };

    reset = () => {
        this.setState({text: "", submitting: false, comment: null});
    };

    afterSave = (stateChanged = false) => {
        this.reset();
        this.props.dispatch(issuesActions.comments.invalidate(this.props.issueDetail.id));
        if (stateChanged) {
            this.props.dispatch(issuesActions.detail.invalidate(this.props.issueDetail.id));
            this.props.dispatch(issuesActions.list.invalidate(this.props.issueDetail.issueListId));
        }
    };

    updateComment = () => {
        const {comment, text} = this.state;
        this.setState({submitting: true});
        WebApi.updateIssueComment(comment.id, {...comment, comment: text}).then(this.afterSave.bind(this, true));
    };

    editIssue = () => {
        const {dispatch, issueDetail} = this.props;
        dispatch(modalDialogShow(this, i18n("arr.issues.update.title"), <IssueForm update initialValues={issueDetail.data} onSubmit={this.updateIssue} onSubmitSuccess={() => {
            dispatch(issuesActions.list.invalidate(issueDetail.issueListId));
            dispatch(issuesActions.detail.invalidate(issueDetail.id));
            dispatch(modalDialogHide());
        }} />));
    };

    updateIssue = (data) => {
        const {issueDetail} = this.props;
        return WebApi.updateIssue(issueDetail.data.id, {
            ...issueDetail.data,
            ...data
        })
    };

    render() {
        const {issueStates, issueDetail, issueComments, userDetail} = this.props;
        const {id, data, fetched, isFetching} = issueDetail;
        const {text, comment, submitting} = this.state;


        const canWrite = fetched && (
            userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL) || (
                userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR] &&
                userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR].issueListIds &&
                userDetail.permissionsMap[perms.FUND_ISSUE_LIST_WR].issueListIds.indexOf(data.issueListId) !== -1
            )
        );
        const canUpdateIssue = canWrite && userDetail.id === data.userCreate.id && issueComments.fetched && issueComments.rows.length === 0;

        let state: any = null;
        if (issueStates && issueStates.fetched && data) {
            state = objectById(issueStates.data, data.issueStateId);
        }

        const textFieldDisabled = submitting || state && state.finalState;

        const CustomArea = TextareaAutosize as any;

        return <div className="lecturing-bottom">
            {!id && <div className="text-center">{i18n("arr.issues.choose")}</div>}
            {isFetching && <Loading/>}
            {fetched && <div className="lecturing-bottom-container">
                <div className="comments">
                    <div className="comment text-muted">
                        <div className="comment-text">{data.description}</div>
                        <div className="text-right">
                            {canUpdateIssue && <div className="pull-left">
                                <Button bsStyle="action" onClick={this.editIssue}>
                                    <Icon glyph="fa-pencil" />
                                </Button>
                            </div>}
                            {data.userCreate.username} ({dateTimeToString(new Date(data.timeCreated))})
                        </div>
                    </div>
                    {issueComments.rows.map((item: any, index, arr) => <div>
                        <div className={"comment" + (userDetail.id === item.user.id ? " text-muted" : "")}>
                            <div className="comment-text">{item.comment}</div>
                            <div className="text-right">
                                {canWrite && userDetail.id === item.user.id && arr.length === index+1 && <div className="pull-left">
                                    <Button bsStyle="action" onClick={this.editComment.bind(this, item)}>
                                        <Icon glyph="fa-pencil" />
                                    </Button>
                                </div>}
                                {item.user.username} ({dateTimeToString(new Date(item.timeCreated))})
                            </div>
                        </div>
                        {(item.nextStateId !== item.prevStateId || arr.length === index+1) && <div className="state-change"><Icon glyph="fa-angle-double-right"/> {objectById(issueStates.data, item.nextStateId).name}</div>}
                    </div>)}
                    {!issueComments.rows.length && <div className="state-change"><Icon glyph="fa-angle-double-right"/> {state && state.name}</div>}
                </div>
                {canWrite && !comment && <div className="add-comment">
                    <div>
                        <CustomArea
                            className="form-control"
                            maxRows={3}
                            //rows={3}
                            value={this.state.text} onChange={({target:{value}}: any) => this.setState({text:value})}
                            disabled={textFieldDisabled}
                        />
                    </div>
                    <div className="text-right">
                        <DropdownButton dropup pullRight noCaret title={i18n("arr.issues.state.change")} bsStyle="action" id="comment-state" disabled={!this.state.text || textFieldDisabled}>
                            {issueStates.data.filter(i => i.id !== data.issueStateId).map(i => <MenuItem key={i.id} onClick={this.addComment.bind(this,i.id)}>
                                {i.name}
                            </MenuItem>)}
                        </DropdownButton>
                        <Button bsStyle="action" disabled={!this.state.text || textFieldDisabled} onClick={this.addComment.bind(this,null)}>
                            <Icon glyph="fa-arrow-circle-up"/>
                        </Button>
                    </div>
                </div>}
                {canWrite && comment && <div className="edit-comment">
                    <div>
                        <CustomArea
                            className="form-control"
                            maxRows={12}
                            rows={3}
                            value={text} onChange={({target:{value}}: any) => this.setState({text:value})}
                            disabled={submitting}
                        />
                    </div>
                    <div className="text-right">
                        <Button bsStyle="action" disabled={submitting} onClick={this.reset}>{i18n("global.action.cancel")}</Button>
                        <Button bsStyle="action" disabled={!this.state.text || submitting} onClick={this.updateComment}><Icon glyph="fa-arrow-circle-up"/></Button>
                    </div>
                </div>}
            </div>}
        </div>
    }
}

export default connect((state: any) => {
    return {
        issueTypes: state.refTables.issueTypes,
        issueStates: state.refTables.issueStates,
        issueProtocols: storeFromArea(state, issuesActions.AREA_PROTOCOLS),
        issueDetail: storeFromArea(state, issuesActions.AREA_DETAIL),
        issueComments: storeFromArea(state, issuesActions.AREA_COMMENTS),
        userDetail: state.userDetail,
    }
})(LecturingBottom as any);
