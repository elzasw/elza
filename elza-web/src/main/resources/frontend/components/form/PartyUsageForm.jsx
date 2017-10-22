import React from 'react';
import UsageForm from "./UsageForm"
import * as types from 'actions/constants/ActionTypes.js';
import { i18n } from 'components/shared';
import {WebApi} from "../../actions/WebApi";
import HorizontalLoader from "../shared/loading/HorizontalLoader";
import * as perms from "../../actions/user/Permission";

class PartyUsageForm extends React.Component {

    state = {
        data: null
    };

    componentDidMount(){
        WebApi.findPartyUsage(this.props.detail.id).then(data => {
            this.setState({data})
        })
    }

    handleReplace = (selectedReplacementNode, selectedNode) => {
        if (selectedNode && selectedReplacementNode) {
            alert(
                'ID from tree' +
                selectedNode.propertyId +
                'ID from PartyField ' +
                selectedReplacementNode.id
            );
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

export default PartyUsageForm;
