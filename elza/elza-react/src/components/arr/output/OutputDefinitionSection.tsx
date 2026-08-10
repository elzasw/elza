import {FormInput, i18n} from 'components/shared';
import OutputInlineForm from 'components/arr/OutputInlineForm';
import FundNodesList from '../FundNodesList';
import ScopeField from '../../admin/ScopeField';
import {ScopeList} from '../ScopeList';
import {OutputLayoutProps} from './outputLayoutTypes';

type Props = Pick<
    OutputLayoutProps,
    | 'fundOutputDetail'
    | 'readonly'
    | 'nodesReadOnly'
    | 'connectableScopes'
    | 'outputFiles'
    | 'onSaveOutput'
    | 'onAddScope'
    | 'onRemoveScope'
    | 'onAddNodes'
    | 'onRemoveNode'
>;

/**
 * Definiční část výstupu (formulář, rozsahy, uzly, soubory) sdílená mezi rozvrženími.
 */
export function OutputDefinitionSection({
    fundOutputDetail,
    readonly,
    nodesReadOnly,
    connectableScopes,
    outputFiles,
    onSaveOutput,
    onAddScope,
    onRemoveScope,
    onAddNodes,
    onRemoveNode,
}: Props) {
    return (
        <>
            <div className="output-definition-commons">
                <OutputInlineForm disabled={readonly} output={fundOutputDetail} onSave={onSaveOutput} />
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
                {!readonly && <ScopeField scopes={connectableScopes} onChange={onAddScope} value={null} />}
                <ScopeList scopes={fundOutputDetail.scopes || []} onRemove={onRemoveScope} readOnly={readonly} />
            </div>
            <div>
                <label className="control-label">{i18n('arr.output.title.nodes')}</label>
                <FundNodesList
                    nodes={fundOutputDetail.nodes}
                    onDeleteNode={onRemoveNode}
                    onAddNode={onAddNodes}
                    readOnly={nodesReadOnly}
                />
            </div>
            {outputFiles}
        </>
    );
}
