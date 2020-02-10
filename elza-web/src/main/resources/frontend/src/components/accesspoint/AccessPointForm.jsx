import React from 'react';
import ReactDOM from 'react-dom';
import {
    Icon,
    i18n,
    AbstractReactComponent,
    NoFocusButton
} from 'components/shared'
import {connect} from 'react-redux'

import '../arr/NodeSubNodeForm.less';
import { ItemForm } from "./ItemForm";
import {accessPointFormActions} from "./AccessPointFormActions";
import {ItemFactory} from "./ItemFactory";

/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */
class AccessPointForm extends AbstractReactComponent {

    static PropTypes = {
        selectedSubNodeId: React.PropTypes.number.isRequired,
        rulDataTypes: React.PropTypes.object.isRequired,
        calendarTypes: React.PropTypes.object.isRequired,
        descItemTypes: React.PropTypes.object.isRequired,
        structureTypes: React.PropTypes.object.isRequired,
        subNodeForm: React.PropTypes.object.isRequired,
        closed: React.PropTypes.bool.isRequired,
        readMode: React.PropTypes.bool.isRequired,
        focus: React.PropTypes.object.isRequired,
    };

    initFocus = () => {
        this.refs.subNodeForm.getWrappedInstance().initFocus();
    };

    render() {
        const {focus, closed, rulDataTypes, calendarTypes, structureTypes, descItemTypes, subNodeForm, readMode} = this.props;

        return (
            <div className="output-item-form-container">
                <ItemForm
                    ref="subNodeForm"
                    typePrefix="accesspoint"
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    structureTypes={structureTypes}
                    descItemTypes={descItemTypes}
                    subNodeForm={subNodeForm}
                    closed={closed}
                    conformityInfo={{missings: [], errors: []}}
                    descItemCopyFromPrevEnabled={false}
                    focus={focus}
                    singleDescItemTypeId={null}
                    singleDescItemTypeEdit={false}
                    onDescItemTypeCopyFromPrev={() => {}}
                    onDescItemTypeLock={() => {}}
                    onDescItemTypeCopy={() => {}}
                    formActions={accessPointFormActions}
                    showNodeAddons={false}
                    readMode={closed || readMode}
                    descItemFactory={ItemFactory}
                />
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {focus} = state;

    return {
        focus,
    }
}

export default connect(mapStateToProps, null, null, { withRef: true })(AccessPointForm);
