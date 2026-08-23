import { useContext } from 'react';
import { FluentDialogContext } from 'components/shared/dialog/FluentModalDialog';
import { NodeDaosForm } from './NodeDaosForm';

interface ShowNodeDaosOptions {
    nodeId: number;
    readMode: boolean;
    /** DAO, které se má rovnou otevřít na detailu. */
    daoId?: number;
}

/** Otevře okno s digitálními entitami připojenými k jednotce popisu. */
export function useNodeDaosModal() {
    const { showModal } = useContext(FluentDialogContext);

    return function showNodeDaos({ nodeId, readMode, daoId }: ShowNodeDaosOptions) {
        return showModal<undefined, undefined>({
            createDialog: ({ handleResult }) => (
                <NodeDaosForm
                    nodeId={nodeId}
                    readMode={readMode}
                    daoId={daoId}
                    onClose={() => handleResult(undefined, undefined)}
                />
            ),
        });
    };
}
