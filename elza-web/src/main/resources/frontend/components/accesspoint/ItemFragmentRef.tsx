import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {
    AbstractReactComponent,
    Autocomplete,
    FormInput,
    i18n,
    Icon,
    NoFocusButton,
    TooltipTrigger,
    Utils
} from '../../components/shared';

import {connect} from 'react-redux'
import {Shortcuts} from 'react-shortcuts';
import '../arr/nodeForm/AbstractDescItem.less';
import {WebApi} from '../../actions/index.jsx';
import {Dispatch} from "../../typings/globals";
import {ItemFragmentRefVO} from "../../stores/app/accesspoint/itemFormInterfaces";
import FragmentFormModal from "./FragmentFormModal";
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog'

interface FromState {
}

interface DispatchProps {
    dispatch: Dispatch<FromState>;

}

export interface Props {
    descItem: ItemFragmentRefVO;
    locked: boolean;
    readMode: boolean;
    cal: boolean;
    typePrefix: string
}

interface ItemFormClassState {
}

/**
 * // TODO FOcus
 */
export class ItemFragmentRefClass extends React.Component<DispatchProps & Props, ItemFormClassState>  {

    handleNameAdd = () => {
        //
        WebApi.createFragment("TEST").then(data => {
            this.props.dispatch(modalDialogShow(this, i18n('accesspoint.detail.name.new'), <FragmentFormModal fragmentId={data.frragmentId} onSubmit={(data) => console.warn(data)} />, "dialog-lg"));
        })
    };

    render() {
        const {descItem, locked, readMode, cal, typePrefix, ...otherProps} = this.props;
console.warn(descItem, otherProps)
        return <div className='desc-item-value desc-item-value-parts'>
            <h4>{descItem.fragment && descItem.fragment.value}</h4>
        </div>
    }
}

function mapStateToProps(state, ownProps: Props) {
    return {};
}

export default connect(mapStateToProps, null, null, {withRef: true})(ItemFragmentRefClass as any);
