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



    render() {
        const {descItem, locked, readMode, cal, typePrefix, ...otherProps} = this.props;

        return <div className='desc-item-value desc-item-value-parts'>
            <h4>{descItem.fragment && descItem.fragment.value}</h4>
        </div>
    }
}

function mapStateToProps(state, ownProps: Props) {
    return {};
}

export default connect(mapStateToProps, null, null, {withRef: true})(ItemFragmentRefClass as any);
