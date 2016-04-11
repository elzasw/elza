/**
 * Formulář editace PP - jako buňky v tabulce hromadných úprav - FundDataGrid.
 */

require ('./FundDataGridCellForm.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import * as types from 'actions/constants/ActionTypes';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, SubNodeForm} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {packetsFetchIfNeeded} from 'actions/arr/packets'
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes'
import {fundSubNodeFormFetchIfNeeded} from 'actions/arr/subNodeForm'
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
import {setInputFocus} from 'components/Utils'

var FundDataGridCellForm = class FundDataGridCellForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('getFundDataGrid')

        this.state = {
            fundDataGrid: this.getFundDataGrid(props)
        }
    }

    componentWillReceiveProps(nextProps) {
        this.requestData(nextProps.versionId)

        const newFundDataGrid = this.getFundDataGrid(nextProps)

        const loadingChanged = this.isLoading(this.props, this.state.fundDataGrid) !== this.isLoading(nextProps, newFundDataGrid)
        
        this.setState({
            fundDataGrid: newFundDataGrid
        }, () => {
            if (loadingChanged) {
                this.refs.subNodeForm.getWrappedInstance().initFocus()
            }
        })
    }

    componentDidMount() {
        const {position} = this.props

        this.requestData(this.props.versionId)

        $('.fund-data-grid-cell-edit .modal-dialog').css({
            top: position.y + 'px',
            left: position.x + 'px',
        })
    }

    /**
     * Načtení dat, pokud je potřeba.
     * @param versionId {String} verze AS
     */
    requestData(versionId) {
        const nodeKey = 'DATA_GRID'

        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(fundSubNodeFormFetchIfNeeded(versionId, nodeKey));
        this.dispatch(refRulDataTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
    }

    getFundDataGrid(props) {
        const {versionId, funds} = props
        const activeFund = funds[indexById(funds, versionId, "versionId")]
        const fundDataGrid = activeFund.fundDataGrid
        return fundDataGrid
    }

    isLoading(props, fundDataGrid) {
        const {refTables, packets} = props
        const {rulDataTypes, calendarTypes, packetTypes, descItemTypes} = refTables

        if (fundDataGrid.subNodeForm.fetched && calendarTypes.fetched && descItemTypes.fetched) {
            return false
        } else {
            return true
        }
    }

    render() {
        const {fundDataGrid} = this.state
        const {versionId, fundId, closed, className, style, refTables, packets, onClose} = this.props
        const {rulDataTypes, calendarTypes, packetTypes, descItemTypes} = refTables

        var form
        if (!this.isLoading(this.props, fundDataGrid)) {
            var fundPackets = []
            if (packets[fundId]) {
                fundPackets = packets[fundId].items
            }

            const conformityInfo = {
                errors: {},
                missings: {}
            }

            form = (
                <SubNodeForm
                    singleDescItemTypeEdit
                    singleDescItemTypeId={fundDataGrid.descItemTypeId}
                    ref='subNodeForm'
                    nodeId={fundDataGrid.parentNodeId}
                    versionId={versionId}
                    selectedSubNodeId={fundDataGrid.nodeId}
                    nodeKey='DATA_GRID'
                    subNodeForm={fundDataGrid.subNodeForm}
                    descItemTypeInfos={fundDataGrid.subNodeForm.descItemTypeInfos}
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    packetTypes={packetTypes}
                    descItemTypes={descItemTypes}
                    conformityInfo={conformityInfo}
                    packets={fundPackets}
                    parentNode={{}}
                    fundId={fundId}
                    selectedSubNode={fundDataGrid.subNodeForm.data.node}
                    descItemCopyFromPrevEnabled={false}
                    closed={closed}
                    onAddDescItemType={()=>{}}
                />
            )
        }

        return (
            <div ref='mainDiv' className={className} style={style}>
                <Modal.Body>
                    {form}
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.close')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion, refTables} = state
    return {
        funds: arrRegion.funds,
        refTables,
        packets: arrRegion.packets,
    }
}

module.exports = connect(mapStateToProps)(FundDataGridCellForm)