import PropTypes from 'prop-types';
import React from 'react';

import {connect} from 'react-redux';

import {AbstractReactComponent, i18n, Icon} from 'components/shared';

import './DescItemFileRef.scss';
import {modalDialogShow} from '../../../actions/global/modalDialog';
import {WebApi} from '../../../actions/WebApi';
import {AccessPointFormActions} from '../../accesspoint/AccessPointFormActions';
import {Button} from '../../ui';

let FragmentFormModal;
import("../../accesspoint/FragmentFormModal").then((a) => {
    FragmentFormModal = a.default;
});

class DescItemFragmentRef extends AbstractReactComponent {

    static propTypes = {
        fragmentType: PropTypes.object.isRequired
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
        WebApi.createFragment(this.props.fragmentType.code).then(data => {
            this.props.dispatch(modalDialogShow(this, i18n('accesspoint.detail.name.new'), <FragmentFormModal fragmentId={data.id} onSubmit={() => {
                this.props.onChange(data.id);
                WebApi.confirmFragment(data.id).then(() => {
                    this.props.onBlur();
                });
            }} />, "dialog-lg"));
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
            {!locked && !readMode && descItem.value == null && <Button variant="action" block ref="button" onClick={this.handleFragmentCreate}><Icon glyph="fa-plus-circle"/></Button>}
            {descItem.value !== null && <span>{descItem.fragment && descItem.fragment.value}</span>}
            {!locked && !readMode && descItem.fragment && <Button variant="action" ref="button" onClick={this.handleFragmentEdit}><Icon glyph="fa-pencil"/></Button>}
        </div>
    }
}

export default connect()(DescItemFragmentRef);
