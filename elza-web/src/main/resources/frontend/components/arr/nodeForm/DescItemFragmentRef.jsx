import React from 'react';
import ReactDOM from 'react-dom';

import {connect} from 'react-redux'

import {Icon, i18n, AbstractReactComponent, Autocomplete} from 'components/shared';

import './DescItemFileRef.less'
import {modalDialogShow} from "../../../actions/global/modalDialog";
import {WebApi} from "../../../actions/WebApi";
import {AccessPointFormActions} from "../../accesspoint/AccessPointFormActions";
import {Button} from "react-bootstrap";

let FragmentFormModal;
import("../../accesspoint/FragmentFormModal").then((a) => {
    FragmentFormModal = a.default;
});

class DescItemFragmentRef extends AbstractReactComponent {

    static PropTypes = {
        fundId: React.PropTypes.number.isRequired,
        onFundFiles: React.PropTypes.func.isRequired,
        onCreateFile:  React.PropTypes.func.isRequired,
    };

    state = {
        fileList: []
    };

    focus = () => {
        // No focus
    };

    changeAccessPoint = () => {
        return (dispatch, getState) => {
            const store = getState();
            const parentAp = store.ap.form.parent;
            dispatch({
                type: "CHANGE_ACCESS_POINT",
                id: parentAp.id,
                area: AccessPointFormActions.AREA
            })
        }
    };

    handleFragmentCreate = () => {
        // TODO nutno zjisti propojení fragmentů a accesspoint
        WebApi.createFragment("STAT_ZASTUPCE").then(data => {
            this.props.dispatch(modalDialogShow(this, i18n('accesspoint.detail.name.new'), <FragmentFormModal fragmentId={data.id} onSubmit={() => {
                WebApi.confirmFragment(data.id).then(() => {
                    this.props.onChange(data.id);
                    this.props.onBlur();
                });
            }
            } />, "dialog-lg"));
        })
    };

    handleFragmentEdit = () => {
        this.props.dispatch(modalDialogShow(this, i18n('accesspoint.detail.name.new'), <FragmentFormModal fragmentId={this.props.descItem.value} onSubmit={() => {
            this.props.dispatch(this.changeAccessPoint());
        }} />, "dialog-lg"));
    };

    render() {
        const {descItem, locked, readMode, cal, typePrefix, ...otherProps} = this.props;
        return <div className='desc-item-value desc-item-value-parts'>
            {!locked && descItem.value == null && <Button bsStyle="action" block ref="button" onClick={this.handleFragmentCreate}><Icon glyph="fa-plus-circle"/></Button>}
            {descItem.value !== null && <span>{descItem.fragment && descItem.fragment.value}</span>}
            {!locked && descItem.fragment && <Button bsStyle="action" ref="button" onClick={this.handleFragmentEdit}><Icon glyph="fa-pencil"/></Button>}
        </div>
    }
}

export default connect(null, null, null, { withRef: true })(DescItemFragmentRef);
