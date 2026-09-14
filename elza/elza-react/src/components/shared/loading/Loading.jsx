import React from 'react';

import './Loading.scss';
import { FormattedMessage, defineMessages } from 'react-intl';

// Id je převzaté z legacy katalogu beze změny.
const messages = defineMessages({
    loading: { id: 'global.data.loading', defaultMessage: 'Načítání dat...' },
});

const Loading = ({value = null}) => {
    return <div className="loading">{value ? value : <FormattedMessage {...messages.loading} />}</div>;
};

export default Loading;
