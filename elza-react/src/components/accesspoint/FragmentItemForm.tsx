import PropTypes from 'prop-types';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {
    Icon,
    i18n,
    AbstractReactComponent,
    NoFocusButton
} from '../../components/shared'
import {connect} from 'react-redux'
import '../arr/NodeSubNodeForm.scss';
import { ItemForm } from "./ItemForm";
import {ItemFactory} from "./ItemFactory";
import {ItemFactoryInterface} from "./ItemFactoryInterface";
import {Dispatch} from "../../typings/globals";
import {ItemFormActions} from "./ItemFormActions";
import {IItemFormState} from "../../stores/app/accesspoint/itemForm";
import {fragmentItemFormActions} from "./FragmentItemFormActions";


interface State {}

interface FromState {}

interface DispatchProps {
    dispatch: Dispatch<FromState>;
    focus: any;
    descItemTypes: any;
    structureTypes: any;
    rulDataTypes: any;
    calendarTypes: any;
    formStore: IItemFormState;
}

interface Props {
    formActions:ItemFormActions;
    typePrefix: string;
    showNodeAddons: boolean;
    closed: boolean;
    readMode: boolean;
    parent: {id: number};
    descItemFactory: ItemFactoryInterface;
    conformityInfo: {missings: any[], errors: any[]};
    customActions?: (string, any) => React.ReactNode;
}
interface ReactWrappedComponent<P> extends React.ReactElement<P> {
    getWrappedInstance: Function;
}

class FragmentItemForm extends React.Component<Props & DispatchProps, State> {

    static propTypes = {
        selectedSubNodeId: PropTypes.number.isRequired,
        parent: PropTypes.object.isRequired,
        rulDataTypes: PropTypes.object.isRequired,
        calendarTypes: PropTypes.object.isRequired,
        descItemTypes: PropTypes.object.isRequired,
        structureTypes: PropTypes.object.isRequired,
        subNodeForm: PropTypes.object.isRequired,
        closed: PropTypes.bool.isRequired,
        readMode: PropTypes.bool.isRequired,
        focus: PropTypes.object.isRequired,
    };

    componentDidMount() {
        this.props.dispatch(fragmentItemFormActions.fundSubNodeFormFetchIfNeeded(this.props.parent, true))
    }
    componentWillReceiveProps(nextProps) {
        nextProps.dispatch(fragmentItemFormActions.fundSubNodeFormFetchIfNeeded(this.props.parent))
    }

    initFocus = () => {
        (this.refs.subNodeForm as any as ReactWrappedComponent<{}>).getWrappedInstance().initFocus();
    };

    render() {
        const {focus, closed, rulDataTypes, calendarTypes, structureTypes, descItemTypes, formStore, readMode} = this.props;

        return (
            <div className="output-item-form-container">
                {formStore.fetched && <ItemForm
                    ref="subNodeForm"
                    typePrefix="ap-name"
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    structureTypes={structureTypes}
                    descItemTypes={descItemTypes}
                    subNodeForm={formStore}
                    closed={closed}
                    conformityInfo={{missings: [], errors: []}}
                    focus={focus}
                    formActions={fragmentItemFormActions}
                    showNodeAddons={false}
                    readMode={closed || readMode}
                    descItemFactory={ItemFactory}
                />}
            </div>
        )
    }
}

function mapStateToProps(state, props: Props) {
    const {focus, refTables} = state;

    return {
        focus,
        formStore: fragmentItemFormActions._getItemFormStore(state),
        rulDataTypes: refTables.rulDataTypes.items,
        calendarTypes: refTables.calendarTypes.items,
        descItemTypes: refTables.descItemTypes.items,
    }
}

export default connect(mapStateToProps, null, null, { withRef: true })(FragmentItemForm as any);
