import {useCallback, useEffect} from 'react';
import {mergeClasses, makeStyles} from '@fluentui/react-components';
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes.jsx';
import {HorizontalLoader, i18n} from 'components/shared';
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
import './ArrOutputDetail.scss';
import {OutputEdit} from './output/OutputEdit';
import {OutputColumnLayout} from './output/OutputColumnLayout';
import {OutputStackedLayout} from './output/OutputStackedLayout';
import FundNodesSelectForm from './FundNodesSelectForm';
import FundOutputFiles from './FundOutputFiles';
import {ApScopeVO, ArrOutputVO} from '../../typings/Outputs';
import {AppFetchingStore} from '../../typings/globals';
import * as scopeActions from '../../actions/scopes/scopes';
import storeFromArea from '../../shared/utils/storeFromArea';
import {WebApi} from 'actions/index';
import {showConfirmDialog} from 'components/shared/dialog';
import {useAppThunkDispatch, useContainerWidth} from 'utils/hooks';
import {useAppSelector} from 'utils/hooks/useAppSelector';
import {useUserSettings} from 'contexts/user/useSettings';

const COLUMN_LAYOUT_MIN_WIDTH = 900;

const useStyles = makeStyles({
    columnContainer: {
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
    },
});

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
    const styles = useStyles();
    const {settings} = useUserSettings();
    const [containerRef, containerWidth] = useContainerWidth<HTMLDivElement>();
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

    const fitsColumns = containerWidth === null || containerWidth >= COLUMN_LAYOUT_MIN_WIDTH;
    const useColumns = !!settings.outputColumnLayout && fitsColumns;

    const layoutProps = {
        fundOutputDetail,
        readonly,
        nodesReadOnly: readonly,
        connectableScopes,
        form,
        outputFiles: renderOutputFiles(),
        onSaveOutput: handleSaveOutput,
        onAddScope: handleAddScope,
        onRemoveScope: handleRemoveScope,
        onAddNodes: handleAddNodes,
        onRemoveNode: handleRemoveNode,
    };

    return (
        <div
            ref={containerRef}
            className={mergeClasses('arr-output-detail-container', useColumns && styles.columnContainer)}
        >
            {useColumns ? <OutputColumnLayout {...layoutProps} /> : <OutputStackedLayout {...layoutProps} />}
        </div>
    );
}

export default ArrOutputDetail;
