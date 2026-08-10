import {ReactNode} from 'react';
import {ApScopeVO, ArrOutputVO} from '../../../typings/Outputs';
import {AppFetchingStore} from '../../../typings/globals';

export interface OutputLayoutProps {
    fundOutputDetail: ArrOutputVO & AppFetchingStore & {subNodeForm: any; lockDate: any};
    readonly: boolean;
    nodesReadOnly: boolean;
    connectableScopes: any;
    form: ReactNode;
    outputFiles: ReactNode;
    onSaveOutput: (data: Partial<ArrOutputVO>) => void;
    onAddScope: (scope: ApScopeVO) => void;
    onRemoveScope: (scope: ApScopeVO) => void;
    onAddNodes: () => void;
    onRemoveNode: (node: any) => void;
}
