import PropTypes from 'prop-types';
import * as React from 'react';
import { connect } from 'react-redux';
import { IItemFormState } from '../../stores/app/accesspoint/itemForm';
import { Dispatch } from '../../typings/globals';
import '../arr/NodeSubNodeForm.scss';
import { apNameFormActions } from './ApNameFormActions';
import { ItemFactory } from './ItemFactory';
import { ItemFactoryInterface } from './ItemFactoryInterface';
import { ItemForm } from './ItemForm';
import { ItemFormActions } from './ItemFormActions';

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
        this.props.dispatch(apNameFormActions.fundSubNodeFormFetchIfNeeded(this.props.parent))
    }
    UNSAFE_componentWillReceiveProps(nextProps) {
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
                    //ref="subNodeForm" TODO React 16 REF
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

export default connect(mapStateToProps)(ApItemNameForm as any);
