import * as React from 'react';
import * as ReactDOM from 'react-dom';
import {
    Icon,
    i18n,
    AbstractReactComponent,
    NoFocusButton
} from '../../components/shared'
import {connect} from 'react-redux'
import '../arr/NodeSubNodeForm.less';
import { ItemForm } from "./ItemForm";
import {apNameFormActions} from "./ApNameFormActions";
import {ItemFactory} from "./ItemFactory";
import {ItemFactoryInterface} from "./ItemFactoryInterface";
import {Dispatch} from "../../typings/globals";
import {ItemFormActions} from "./ItemFormActions";
import {IItemFormState} from "../../stores/app/accesspoint/itemForm";


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
    parent: {id: number, accessPointId: number};
    descItemFactory: ItemFactoryInterface;
    conformityInfo: {missings: any[], errors: any[]};
    customActions?: (string, any) => React.ReactNode;
}
interface ReactWrappedComponent<P> extends React.ReactElement<P> {
    getWrappedInstance: Function;
}

/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */
class ApItemNameForm extends React.Component<Props & DispatchProps, State> {

    static propTypes = {
        selectedSubNodeId: React.PropTypes.number.isRequired,
        parent: React.PropTypes.object.isRequired,
        rulDataTypes: React.PropTypes.object.isRequired,
        calendarTypes: React.PropTypes.object.isRequired,
        descItemTypes: React.PropTypes.object.isRequired,
        structureTypes: React.PropTypes.object.isRequired,
        subNodeForm: React.PropTypes.object.isRequired,
        closed: React.PropTypes.bool.isRequired,
        readMode: React.PropTypes.bool.isRequired,
        focus: React.PropTypes.object.isRequired,
    };

    componentDidMount() {
        this.props.dispatch(apNameFormActions.fundSubNodeFormFetchIfNeeded(this.props.parent))
    }
    componentWillReceiveProps(nextProps) {
        nextProps.dispatch(apNameFormActions.fundSubNodeFormFetchIfNeeded(this.props.parent))
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
                    formActions={apNameFormActions}
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
        formStore: apNameFormActions._getItemFormStore(state),
        rulDataTypes: refTables.rulDataTypes.items,
        calendarTypes: refTables.calendarTypes.items,
        descItemTypes: refTables.descItemTypes.items,
    }
}

export default connect(mapStateToProps, null, null, { withRef: true })(ApItemNameForm as any);
