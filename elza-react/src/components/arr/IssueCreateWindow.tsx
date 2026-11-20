import { makeStyles } from '@fluentui/react-components';
import { WebApi } from 'actions';
import * as issuesActions from 'actions/arr/issues';
import { createReferenceMarkStringFromArray } from 'components/arr/ArrUtils';
import { daoMessages } from 'components/arr/daoMessages';
import { IssueForm, IssueFormValues } from 'components/form/IssueForm';
import { CollapsibleDragWindow } from 'components/shared/dialog/FluentModalDialog';
import { defineMessages, useIntl } from 'react-intl';
import { IssueVO } from 'types';
import { useAppThunkDispatch } from 'utils/hooks';

const messages = defineMessages({
    nodeTitle: {
        id: 'arr.issues.createWindow.nodeTitle',
        defaultMessage: 'Přidat připomínku - Jednotka popisu {referenceMark}',
    },
});

const useStyles = makeStyles({
    form: {
        padding: 0,
        flexGrow: 1,
        minHeight: 0,
        overflowY: 'auto',
    },
});

interface Props {
    /** Unit of description the comment is for; a comment on the whole fund has none. */
    nodeId?: number;
    /** Shown in the title, so the user knows the target unit after navigating elsewhere. */
    referenceMark?: string[];
    initialValues: IssueFormValues;
    onClose: () => void;
}

/** Non-modal window for adding a lecturing comment; the arrangement stays usable while it is open. */
export function IssueCreateWindow({ nodeId, referenceMark, initialValues, onClose }: Props) {
    const intl = useIntl();
    const styles = useStyles();
    const dispatch = useAppThunkDispatch();

    function getTitle() {
        const isFundIssue = nodeId == null;
        if (isFundIssue) {
            return intl.formatMessage(daoMessages.issuesAddArrTitle);
        }
        const hasReferenceMark = referenceMark != null && referenceMark.length > 0;
        if (!hasReferenceMark) {
            return intl.formatMessage(daoMessages.issuesAddNodeTitle);
        }
        return intl.formatMessage(messages.nodeTitle, {
            referenceMark: createReferenceMarkStringFromArray(referenceMark),
        });
    }

    function addIssue(values: IssueFormValues): Promise<IssueVO> {
        return WebApi.addIssue({ ...values, nodeId });
    }

    function handleIssueAdded(issue: IssueVO) {
        dispatch(issuesActions.list.invalidate(issue.issueListId));
        dispatch(issuesActions.detail.invalidate(issue.id));
        onClose();
    }

    return (
        <CollapsibleDragWindow title={getTitle()} onClose={onClose} initialWidth={500} initialHeight={420} autoHeight>
            <IssueForm
                className={styles.form}
                initialValues={initialValues}
                onSubmit={addIssue}
                onSubmitSuccess={handleIssueAdded}
                onClose={onClose}
            />
        </CollapsibleDragWindow>
    );
}

export type IssueCreateWindowProps = Props;
