import { FluentDialogContext, FluentDialogContextType } from 'components/shared/dialog/FluentModalDialog';
import { useContext } from 'react';
import { Node, NodeBase } from 'typings/store';
import { IssueCreateWindow, IssueCreateWindowProps } from './IssueCreateWindow';

type IssueCreateWindowOptions = Omit<IssueCreateWindowProps, 'onClose'>;

/**
 * Opens the window for adding a lecturing comment. One window per unit of description (and one for
 * the whole fund): a second request for the same target leaves the open window as it is.
 * Class components call this with `showModal` from `FluentDialogContext`.
 */
export function showIssueCreateWindow(
    showModal: FluentDialogContextType['showModal'],
    options: IssueCreateWindowOptions
) {
    return showModal<undefined, undefined>({
        isSingleInstance: true,
        name: `issue-create-${options.nodeId ?? 'fund'}`,
        createDialog: ({ handleResult }) => (
            <IssueCreateWindow {...options} onClose={() => handleResult(undefined, undefined)} />
        ),
    });
}

export function useIssueCreateWindow() {
    const { showModal } = useContext(FluentDialogContext);

    return function openIssueCreateWindow(options: IssueCreateWindowOptions) {
        return showIssueCreateWindow(showModal, options);
    };
}

/** Reference mark of a unit of description among the loaded children of the given parent node. */
export function getChildReferenceMark(parentNode: Node | undefined, nodeId: number | undefined) {
    const childNodes = parentNode?.childNodes as NodeBase[] | undefined;
    return childNodes?.find((childNode) => childNode.id === nodeId)?.referenceMark;
}
