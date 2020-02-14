import React from 'react';
import UsageForm from "./UsageForm"
import * as types from 'actions/constants/ActionTypes.js';
import { i18n } from 'components/shared';
import {WebApi} from "../../actions/WebApi";
import HorizontalLoader from "../shared/loading/HorizontalLoader";
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'

import {connect} from "react-redux";
import {modalDialogHide} from "../../actions/global/modalDialog";

class PartyUsageForm extends React.Component {

    state = {
        data: null
    };

    componentDidMount(){
        this.fetchData();
    }

    fetchData = () => {
        WebApi.findPartyUsage(this.props.detail.id).then(data => {
            this.setState({data})
        })
    };

    handleReplace = (selectedReplacementNode) => {
        if (selectedReplacementNode) {
            WebApi.replaceParty(this.props.detail.id, selectedReplacementNode.id).then(() => {
                this.props.dispatch(addToastrSuccess(i18n("party.replaceSuccess")));
                this.props.dispatch(modalDialogHide());
            });
        }
    };

    render(){
        const {data} = this.state;
        if (data) return <UsageForm
            detail={this.props.detail}
            treeArea={types.FUND_TREE_AREA_USAGE}
            replaceText={i18n("party.replaceText")}
            onReplace={this.handleReplace}
            type="party"
            data={data}
        />;

        return <HorizontalLoader/>
    }
}

export default connect()(PartyUsageForm);
