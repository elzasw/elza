import React from 'react';
import UsageForm from "./UsageForm"
import * as types from 'actions/constants/ActionTypes.js';
import { i18n } from 'components/shared';
import {WebApi} from "../../actions/WebApi";
import HorizontalLoader from "../shared/loading/HorizontalLoader";

class RegistryUsageForm extends React.Component {

    state = {
        data: null
    };

    componentDidMount(){
        WebApi.findRegistryUsage(this.props.detail.id).then(data => {
            this.setState({data})
        })
    }

    handleReplace = (selectedReplacementNode) => {
        if (selectedReplacementNode) {
            alert('Not implemented | ID from RegField ' + selectedReplacementNode.id);
            //TODO volat
            false && WebApi.replaceRegistry(this.props.detail, selectedReplacementNode);
        }
    };

    render(){
        const {data} = this.state;
        if (data) return <UsageForm
            detail={this.props.detail}
            treeArea={types.FUND_TREE_AREA_USAGE}
            replaceText={i18n("registry.replaceText")}
            onReplace={this.handleReplace}
            type="registry"
            data={data}
        />;

        return <HorizontalLoader/>
    }
}

export default RegistryUsageForm;
