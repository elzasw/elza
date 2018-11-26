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

class LecturingBottom extends React.Component {

    static propTypes = {
        fund: React.PropTypes.object.isRequired,
    };

    state = {
        text: "",
    };

    componentDidMount() {
        const {issueDetail} = this.props;
        if (issueDetail && issueDetail.fetched) {
            this.props.dispatch(issuesActions.comments.fetchIfNeeded(issueDetail.id));
        }
    }

    componentWillReceiveProps(nextProps, nextContext) {
        const {issueDetail} = nextProps;
        if (issueDetail.id) {
            this.props.dispatch(issuesActions.comments.fetchIfNeeded(issueDetail.id));
        }
    }


    addComment = (nextStateId) => {
        const {issueDetail:{id}} = this.props;
        WebApi.addIssueComment({issueId: id, comment: this.state.text, nextStateId}).then(() => {
            this.setState({text:""});
            this.props.dispatch(issuesActions.comments.invalidate(id));
        })
    };

    render() {
        const {issueStates, issueDetail, issueComments, userDetail} = this.props;

        const {id, data, fetched, isFetching} = issueDetail;
        return <div className="lecturing-bottom">
            {!id && <div className="text-center">{i18n("arr.issues.choose")}</div>}
            {isFetching && <Loading/>}
            {fetched && <div className="lecturing-bottom-container">
                <div className="comments">
                    <div className="comment text-muted">
                        <div>{data.description}</div>
                        <div className="text-right">{data.userCreate.username} ({dateTimeToString(new Date(data.timeCreated))})</div>
                    </div>
                    {issueComments.rows.map((item: CommentVO) => <div>
                        <div className={"comment" + (userDetail.id === item.user.id ? " text-muted" : "")}>
                            <div>{item.comment}</div>
                            <div className="text-right">{item.user.username} ({dateTimeToString(new Date(item.timeCreated))})</div>
                        </div>
                        {item.nextStateId !== item.prevStateId && <div className="state-change"><Icon glyph="fa-angle-double-right"/> {objectById(issueStates.data, item.nextStateId).name}</div>}
                    </div>)}
                </div>
                <div>
                    <TextareaAutosize
                        className="form-control"
                        maxRows={3}
                        rows={3}
                        value={this.state.text} onChange={({target:{value}}) => this.setState({text:value})}
                        innerRef={ref => this.textarea = ref}
                    />
                </div>
                <div className="text-right">
                    <DropdownButton dropup pullRight noCaret title={i18n("arr.issues.state.change")} bsStyle="action" id="comment-state" disabled={!this.state.text}>
                        {issueStates.data.filter(i => i.id !== data.issueStateId).map(i => <MenuItem key={i.id} onClick={this.addComment.bind(this,i.id)}>
                            {i.name}
                        </MenuItem>)}
                    </DropdownButton>
                    <Button bsStyle="action" disabled={!this.state.text} onClick={this.addComment.bind(this,null)}>
                        <Icon glyph="fa-arrow-circle-up"/>
                    </Button>
                </div>
            </div>}
        </div>
    }
}

export default connect((state) => {
    return {
        issueTypes: state.refTables.issueTypes,
        issueStates: state.refTables.issueStates,
        issueDetail: storeFromArea(state, issuesActions.AREA_DETAIL),
        issueComments: storeFromArea(state, issuesActions.AREA_COMMENTS),
        userDetail: state.userDetail,
    }
})(LecturingBottom);
