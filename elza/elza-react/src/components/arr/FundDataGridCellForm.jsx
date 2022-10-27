/**
 * Formulář editace PP - jako buňky v tabulce hromadných úprav - FundDataGrid.
 */

import './FundDataGridCellForm.scss';

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n} from 'components/shared';
import NodeSubNodeForm from './NodeSubNodeForm';
import {Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {indexById} from 'stores/app/utils';
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes';
import {nodeFormActions} from 'actions/arr/subNodeForm';
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes';
import $ from 'jquery';
import ConfirmForm from '../shared/form/ConfirmForm';
import {WebApiCls as WebApi} from '../../actions/WebApi';
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog';

class FundDataGridCellForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('getFundDataGrid');
        this.subNodeFormRef = React.createRef(null);

        this.state = {
            fundDataGrid: this.getFundDataGrid(props),
            dataLoaded: false, // data dialogu jsou načtena a můžeme dialog správně napozicovat
        };
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const {dataLoaded} = this.state;
        const newFundDataGrid = this.getFundDataGrid(nextProps);

        const hasData = this.requestData(nextProps.versionId, newFundDataGrid);

        if (hasData && dataLoaded !== hasData) {
            // data byla načtená a předtím ještě ne, napozivujeme jednou dialog
            this.setState({}, this.handlePosition);
        }

        const loadingChanged =
            this.isLoading(this.props, this.state.fundDataGrid) !== this.isLoading(nextProps, newFundDataGrid);

        this.setState(
            {
                fundDataGrid: newFundDataGrid,
            },
            () => {
                if (loadingChanged) {
                    this.subNodeFormRef?.current && this.subNodeFormRef.current.initFocus();
                }
            },
        );
    }

    containsDescItem(formData, descItemTypeId) {
        for (var g = 0; g < formData.descItemGroups.length; g++) {
            const group = formData.descItemGroups[g];
            for (var i = 0; i < group.descItemTypes.length; i++) {
                const descItemType = group.descItemTypes[i];
                if (descItemType.id === descItemTypeId) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Napoyicuje dialog na sprvávnou pozici tak, aby se vešel na obrazovku
     */
    handlePosition = () => {
        const {position} = this.props;

        if (position) {
            const dialog = $('.fund-data-grid-cell-edit .modal-dialog');
            const screen = $(document);
            let dialogSize = {w: dialog.width(), h: dialog.height()};
            const dialogInnerSize = {w: dialog.innerWidth(), h: dialog.innerHeight()};
            const windowSize = {w: screen.width(), h: screen.height()};
            console.log(dialog[0], dialogSize, dialogInnerSize, windowSize);

            let x = position.x;
            let y = position.y;
            if (x + dialogSize.w > windowSize.w) {
                x = windowSize.w - dialogSize.w;
                if (x < 0) {
                    dialogSize.w += x;
                    x = 0;
                }
            }
            if (y + dialogSize.h > windowSize.h) {
                y = windowSize.h - dialogSize.h;
                if (y < 0) {
                    dialogSize.h += y;
                    y = 0;
                }
            }

            $('.fund-data-grid-cell-edit .modal-dialog').css({
                top: y + 'px',
                left: x + 'px',
                width: dialogSize.w,
                height: dialogSize.h,
                visibility: 'visible',
            });
        }
    };

    componentDidMount() {
        const {dataLoaded} = this.state;

        const hasData = this.requestData(this.props.versionId, this.state.fundDataGrid);

        if (hasData && dataLoaded !== hasData) {
            // data byla načtená a předtím ještě ne, napozivujeme jednou dialog
            this.setState({}, this.handlePosition);
        }
    }

    /**
     * Načtení dat, pokud je potřeba.
     * @param versionId {String} verze AS
     * @return ture, pokud již má data
     */
    requestData(versionId, validFundDataGrid) {
        const routingKey = 'DATA_GRID';

        let result = false;

        this.props.dispatch(descItemTypesFetchIfNeeded());
        this.props.dispatch(nodeFormActions.fundSubNodeFormFetchIfNeeded(versionId, routingKey));
        this.props.dispatch(refRulDataTypesFetchIfNeeded());

        // Pokud se jedná o editaci jedné položky, musíme zajistit, že tato položka tam je - alespoň prázdná
        if (validFundDataGrid.subNodeForm.fetched) {
            const subNodeForm = validFundDataGrid.subNodeForm;
            const formData = subNodeForm.formData;

            if (!this.containsDescItem(formData, validFundDataGrid.descItemTypeId)) {
                this.props.dispatch(
                    nodeFormActions.fundSubNodeFormDescItemTypeAdd(
                        versionId,
                        routingKey,
                        validFundDataGrid.descItemTypeId,
                    ),
                );
            } else {
                // Máme data a jsou v pořádku
                this.setState({dataLoaded: true});
                result = true;
            }
        }

        return result;
    }

    getFundDataGrid(props) {
        const {versionId, funds} = props;
        const activeFund = funds[indexById(funds, versionId, 'versionId')];
        const fundDataGrid = activeFund.fundDataGrid;
        return fundDataGrid;
    }

    isLoading(props, fundDataGrid) {
        const {refTables} = props;
        const {rulDataTypes, descItemTypes} = refTables;

        if (fundDataGrid.subNodeForm.fetched && descItemTypes.fetched) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Zobrazení formuláře pro potvrzení synchronizace DAO.
     */
    handleDigitizationSync = () => {
        const {node, versionId} = this.props;
        const nodeId = node.selectedSubNodeId;

        const confirmForm = (
            <ConfirmForm
                confirmMessage={i18n('arr.daos.node.sync.confirm-message')}
                submittingMessage={i18n('arr.daos.node.sync.submitting-message')}
                submitTitle={i18n('global.action.run')}
                onSubmit={() => {
                    return WebApi.syncDaoLink(versionId, nodeId);
                }}
                onSubmitSuccess={() => {
                    this.props.dispatch(modalDialogHide());
                }}
            />
        );
        this.props.dispatch(modalDialogShow(this, i18n('arr.daos.node.sync.title'), confirmForm));
    };

    render() {
        const {fundDataGrid} = this.state;
        const {versionId, fundId, closed, className, style, refTables, onClose} = this.props;
        const {rulDataTypes, descItemTypes} = refTables;

        var form;
        if (!this.isLoading(this.props, fundDataGrid)) {
            const conformityInfo = {
                errors: {},
                missings: {},
            };

            form = (
                <NodeSubNodeForm
                    singleDescItemTypeEdit
                    singleDescItemTypeId={fundDataGrid.descItemTypeId}
                    ref={this.subNodeFormRef}
                    nodeId={fundDataGrid.parentNodeId}
                    versionId={versionId}
                    selectedSubNodeId={fundDataGrid.nodeId}
                    routingKey="DATA_GRID"
                    subNodeForm={fundDataGrid.subNodeForm}
                    descItemTypeInfos={fundDataGrid.subNodeForm.descItemTypeInfos}
                    rulDataTypes={rulDataTypes}
                    descItemTypes={descItemTypes}
                    conformityInfo={conformityInfo}
                    parentNode={{}}
                    onVisiblePolicy={() => {}}
                    fundId={fundId}
                    selectedSubNode={fundDataGrid.subNodeForm.data.parent}
                    descItemCopyFromPrevEnabled={false}
                    closed={closed}
                    onDigitizationSync={this.handleDigitizationSync}
                    onAddDescItemType={() => {}}
                    readMode={false}
                />
            );
        }

        return (
            <div ref="mainDiv" className={className} style={style}>
                <Modal.Body>{form}</Modal.Body>
                <Modal.Footer>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.close')}
                    </Button>
                </Modal.Footer>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const {arrRegion, refTables} = state;
    return {
        funds: arrRegion.funds,
        refTables,
    };
}

export default connect(mapStateToProps)(FundDataGridCellForm);
