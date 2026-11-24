import { FluentDialogContext, FluentDialogContextType } from 'components/shared/dialog/FluentModalDialog';
import { useContext } from 'react';
import { IssueCreateWindow, IssueCreateWindowProps } from './IssueCreateWindow';
import { requestIssueCreateWindowReveal } from './issueCreateWindowReveal';

type IssueCreateWindowOptions = Omit<IssueCreateWindowProps, 'onClose'>;

/**
 * Opens the window for adding a lecturing comment. Only one such window is open at a time; a second
 * request switches the open window to the requested target, expands it and brings it to the front,
 * keeping what the user typed. Class components call this with `showModal` from `FluentDialogContext`.
 */
export function showIssueCreateWindow(
    showModal: FluentDialogContextType['showModal'],
    options: IssueCreateWindowOptions
) {
    requestIssueCreateWindowReveal(options.initialTarget);
    return showModal<undefined, undefined>({
        isSingleInstance: true,
        name: 'issue-create',
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
