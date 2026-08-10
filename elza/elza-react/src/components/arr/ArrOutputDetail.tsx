import {useCallback, useEffect} from 'react';
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes.jsx';
import {FormInput, HorizontalLoader, i18n} from 'components/shared';
import {
    fundOutputAddNodes,
    fundOutputDetailFetchIfNeeded,
    fundOutputEdit,
    fundOutputRemoveNodes,
} from '../../actions/arr/fundOutput.jsx';
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx';
import {refRulDataTypesFetchIfNeeded} from 'actions/refTables/rulDataTypes.jsx';
import {outputFormActions} from 'actions/arr/subNodeForm';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import OutputInlineForm from 'components/arr/OutputInlineForm';
import './ArrOutputDetail.scss';
import {OutputEdit} from './output/OutputEdit';
import FundNodesList from './FundNodesList';
import FundNodesSelectForm from './FundNodesSelectForm';
import FundOutputFiles from './FundOutputFiles';
import ToggleContent from '../shared/toggle-content/ToggleContent';
import {ApScopeVO, ArrOutputVO} from '../../typings/Outputs';
import {AppFetchingStore} from '../../typings/globals';
import ScopeField from '../admin/ScopeField';
import * as scopeActions from '../../actions/scopes/scopes';
import storeFromArea from '../../shared/utils/storeFromArea';
import {WebApi} from 'actions/index';
import {ScopeList} from './ScopeList';
import {showConfirmDialog} from 'components/shared/dialog';
import {useAppThunkDispatch} from 'utils/hooks';
import {useAppSelector} from 'utils/hooks/useAppSelector';

const OutputState = {
    OPEN: 'OPEN',
    COMPUTING: 'COMPUTING',
    GENERATING: 'GENERATING',
    FINISHED: 'FINISHED',
    OUTDATED: 'OUTDATED',
    ERROR: 'ERROR', /// Pomocný stav websocketu
};

interface Props {
    versionId: number;
    fund: any;
    descItemTypes: any;
    templates: any;
    outputFilters: any;
    rulDataTypes: any;
    closed: boolean;
    readMode: boolean;
    fundOutputDetail: ArrOutputVO & AppFetchingStore & {subNodeForm: any; lockDate: any};
}

const isOutputEditable = (item: Props['fundOutputDetail']) => {
    return !item.lockDate && item.state === OutputState.OPEN;
};

/**
 * Formulář detailu a editace verze výstupu.
 */
export function ArrOutputDetail({
    versionId,
    fund,
    descItemTypes,
    outputFilters,
    closed,
    readMode,
    fundOutputDetail,
}: Props) {
    const dispatch = useAppThunkDispatch();
    const scopeList = useAppSelector(state => storeFromArea(state, scopeActions.AREA_SCOPE_LIST));

    useEffect(() => {
        dispatch(descItemTypesFetchIfNeeded());
        if (fundOutputDetail.fetched) {
            dispatch(outputFormActions.fundSubNodeFormFetchIfNeeded(versionId, null, undefined, undefined, undefined));
        }
        dispatch(refRulDataTypesFetchIfNeeded());
    }, [dispatch, versionId, fundOutputDetail.fetched, fundOutputDetail.currentDataKey]);

    useEffect(() => {
        if (fundOutputDetail.id !== null) {
            dispatch(fundOutputDetailFetchIfNeeded(versionId, fundOutputDetail.id));
        }
        dispatch(outputTypesFetchIfNeeded());
        dispatch(scopeActions.scopesListFetchIfNeeded());
    }, [dispatch, versionId, fundOutputDetail.id, fundOutputDetail.currentDataKey]);

    const handleSaveOutput = useCallback(
        (data: Partial<ArrOutputVO>) => {
            return dispatch(fundOutputEdit(fund.versionId, fundOutputDetail.id, data));
        },
        [dispatch, fund.versionId, fundOutputDetail.id],
    );

    const handleRemoveNode = useCallback(
        async (node: any) => {
            const response = (await dispatch(showConfirmDialog(i18n('arr.fund.nodes.deleteNode')))) as any;
            if (response) {
                dispatch(fundOutputRemoveNodes(fund.versionId, fundOutputDetail.id, [node.id]));
            }
        },
        [dispatch, fund.versionId, fundOutputDetail.id],
    );

    const handleRemoveScope = useCallback(
        async (scope: ApScopeVO) => {
            const response = (await dispatch(showConfirmDialog(i18n('arr.fund.nodes.deleteNode')))) as any;
            if (response) {
                WebApi.deleteRestrictedScope(fundOutputDetail.id, scope.id);
            }
        },
        [dispatch, fundOutputDetail.id],
    );

    const handleAddScope = useCallback(
        (scope: ApScopeVO) => {
            WebApi.addRestrictedScope(fundOutputDetail.id, scope.id);
            // Zbytek zařídí websocket
        },
        [fundOutputDetail.id],
    );

    const handleAddNodes = useCallback(() => {
        dispatch(
            modalDialogShow(
                null,
                i18n('arr.fund.nodes.title.select'),
                <FundNodesSelectForm
                    // @ts-ignore
                    onSubmitForm={(ids, nodes) => {
                        dispatch(fundOutputAddNodes(fund.versionId, fundOutputDetail.id, ids));
                    }}
                />,
            ),
        );
    }, [dispatch, fund.versionId, fundOutputDetail.id]);

    const renderOutputFiles = () => {
        const {
            fundOutput: {fundOutputFiles},
        } = fund;

        if (fundOutputDetail.outputResultIds === null || fundOutputDetail.outputResultIds.length === 0) {
            return null;
        }

        return (
            <FundOutputFiles
                versionId={versionId}
                outputId={fundOutputDetail.id}
                fundOutputFiles={fundOutputFiles}
                outputResultIds={fundOutputDetail.outputResultIds}
            />
        );
    };

    if (fundOutputDetail.id === null) {
        return (
            <div className="arr-output-detail-container">
                <div className="unselected-msg">
                    <div className="title">{i18n('arr.output.noSelection.title')}</div>
                    <div className="msg-text">{i18n('arr.output.noSelection.message')}</div>
                </div>
            </div>
        );
    }

    const fetched =
        fundOutputDetail.fetched &&
        fundOutputDetail.subNodeForm.fetched &&
        outputFilters.fetched &&
        descItemTypes.fetched;
    if (!fetched) {
        return <HorizontalLoader />;
    }

    const form = <OutputEdit outputId={fundOutputDetail.id} />;

    const readonly = closed || readMode || !isOutputEditable(fundOutputDetail);

    const existingScopes = (fundOutputDetail.scopes || []).map(i => i.id);
    const connectableScopes = scopeList.rows && scopeList.rows.filter(s => existingScopes.indexOf(s.id) === -1);

    return (
        <div className="arr-output-detail-container">
            <div className="output-definition-commons">
                <OutputInlineForm disabled={readonly} output={fundOutputDetail} onSave={handleSaveOutput} />
                {fundOutputDetail.error && (
                    <div>
                        <FormInput
                            type="textarea"
                            value={fundOutputDetail.error}
                            disabled
                            label={i18n('arr.output.title.error')}
                        />
                    </div>
                )}
            </div>
            <div>
                <label className="control-label">{i18n('arr.output.title.scopes')}</label>
                {!readonly && <ScopeField scopes={connectableScopes} onChange={handleAddScope} value={null} />}
                <ScopeList scopes={fundOutputDetail.scopes || []} onRemove={handleRemoveScope} readOnly={readonly} />
            </div>
            <div>
                <label className="control-label">{i18n('arr.output.title.nodes')}</label>
                <FundNodesList
                    nodes={fundOutputDetail.nodes}
                    onDeleteNode={handleRemoveNode}
                    onAddNode={handleAddNodes}
                    readOnly={closed || readMode || !isOutputEditable(fundOutputDetail)}
                />
            </div>
            <hr className="small" />
            {renderOutputFiles()}
            <h4 className={'desc-items-title'}>{i18n('developer.title.descItems')}</h4>
            <ToggleContent opened={true} withText>
                {form}
            </ToggleContent>
        </div>
    );
}

export default ArrOutputDetail;
