import React from 'react';
import UsageForm from './UsageForm';
import * as types from 'actions/constants/ActionTypes.js';
import {i18n} from 'components/shared';
import {WebApi} from '../../actions/WebApi';
import HorizontalLoader from '../shared/loading/HorizontalLoader';
import {addToastrSuccess} from '../shared/toastr/ToastrActions';
import {connect} from 'react-redux';
import {modalDialogHide} from '../../actions/global/modalDialog';

class RegistryUsageForm extends React.Component {

    state = {
        data: null
    };

    componentDidMount(){
        this.fetchData();
    }

    fetchData = () => {
        WebApi.findRegistryUsage(this.props.detail.id).then(data => {
            this.setState({data})

        })
    };

    handleReplace = (selectedReplacementNode) => {
        if (selectedReplacementNode) {
            WebApi.replaceRegistry(this.props.detail.id, selectedReplacementNode.id).then(() => {
                this.props.dispatch(addToastrSuccess(i18n("registry.replaceSuccess")));
                this.props.dispatch(modalDialogHide());
            });
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

export default connect()(RegistryUsageForm);
