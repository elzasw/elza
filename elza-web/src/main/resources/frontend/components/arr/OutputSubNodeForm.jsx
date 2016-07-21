/**
 * Formulář detailu a editace jedné JP - jednoho NODE v konkrétní verzi.
 */

require('./NodeSubNodeForm.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {Icon, i18n, AbstractReactComponent, NoFocusButton, AddPacketForm, AddPartyForm, AddRegistryForm,
    AddPartyEventForm, AddPartyGroupForm, AddPartyDynastyForm, AddPartyOtherForm, AddNodeDropdown} from 'components';
import {connect} from 'react-redux'
import {SubNodeForm} from "components/index.jsx";
import {outputFormActions} from 'actions/arr/subNodeForm.jsx'

var OutputSubNodeForm = class OutputSubNodeForm extends AbstractReactComponent {
    constructor(props) {
        super(props);
        
        this.bindMethods(
            "initFocus",
        );
    }
      
    initFocus() {
        this.refs.subNodeForm.getWrappedInstance().initFocus();
    }

    render() {
        const {versionId, focus, closed, fundId, rulDataTypes, calendarTypes, descItemTypes, packetTypes, packets,
            subNodeForm} = this.props;
        
        return (
            <div className="output-item-form-container">
                <SubNodeForm
                    ref="subNodeForm"
                    versionId={versionId}
                    fundId={fundId}
                    routingKey={null}
                    nodeSetting={null}
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    descItemTypes={descItemTypes}
                    packetTypes={packetTypes}
                    packets={packets}
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
                    formActions={outputFormActions}
                    showNodeAddons={false}
                    readMode={false}
                />
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion, focus} = state
    var fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }

    return {
        fund,
        focus,
    }
}

OutputSubNodeForm.propTypes = {
    versionId: React.PropTypes.number.isRequired,
    fundId: React.PropTypes.number.isRequired,
    selectedSubNodeId: React.PropTypes.number.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    packets: React.PropTypes.array.isRequired,
    subNodeForm: React.PropTypes.object.isRequired,
    closed: React.PropTypes.bool.isRequired,
    focus: React.PropTypes.object.isRequired,
}

module.exports = connect(mapStateToProps, null, null, { withRef: true })(OutputSubNodeForm);
