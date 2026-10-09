import { makeStyles, tokens } from '@fluentui/react-components';
import { defineMessages } from 'react-intl';

// Id jsou převzatá z legacy katalogu beze změny.
export const stateChangeMessages = defineMessages({
    scope: { id: 'ap.state.title.scope', defaultMessage: 'Oblast' },
    type: { id: 'ap.state.title.type', defaultMessage: 'Třída entity' },
    state: { id: 'ap.state.title.state', defaultMessage: 'Stav' },
    comment: { id: 'ap.state.title.comment', defaultMessage: 'Komentář' },
    assignedUser: { id: 'ap.state.title.assignedUser', defaultMessage: 'Přiděleno' },
    toApproveSameUser: {
        id: 'ap.state.title.assignedUser.error.toApproveSameUser',
        defaultMessage: 'Záznam může schválit pouze jiný uživatel',
    },
    validationErrors: { id: 'ap.validation.errors', defaultMessage: 'Chyby validace' },
});

export const useStateChangeDialogStyles = makeStyles({
    surface: {
        maxWidth: '600px',
        width: '100%',
    },
    content: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
    },
});
