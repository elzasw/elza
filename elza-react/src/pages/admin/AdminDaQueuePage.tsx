import { Dropdown, MessageBar, MessageBarBody, Option, Spinner } from '@fluentui/react-components';
import { DaQueueList } from 'components/admin/da-queue/DaQueueList';
import { messages } from 'components/admin/da-queue/messages';
import { Ribbon } from 'components/index.jsx';
import { WebApi } from 'actions/index.jsx';
import { useEffect, useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { Redirect, useHistory, useParams } from 'react-router';
import { AdminLayout } from '../shared/layout/AdminLayout';

export const URL_ADMIN_DA = '/admin/da';

export const urlAdminDaQueue = (repositoryId: number) => `${URL_ADMIN_DA}/${repositoryId}/requests`;

interface DigitalRepository {
    id: number;
    name: string;
    digitalRepositoryType?: string;
}

/**
 * Queue of a digital archive: {@code /admin/da/<id>/requests}. Without an id the first digital
 * archive is opened; with more of them a selector switches between them.
 */
export function AdminDaQueuePage() {
    const intl = useIntl();
    const history = useHistory();
    const { daId } = useParams<{ daId?: string }>();
    const [repositories, setRepositories] = useState<DigitalRepository[]>();

    useEffect(() => {
        WebApi.getAllDigitalRepositorySystem().then((all: DigitalRepository[]) =>
            setRepositories(all.filter(repository => repository.digitalRepositoryType === 'DA')));
    }, []);

    let content;
    if (repositories === undefined) {
        content = <Spinner />;
    } else if (repositories.length === 0) {
        content = (
            <MessageBar intent="info">
                <MessageBarBody><FormattedMessage {...messages.noRepository} /></MessageBarBody>
            </MessageBar>
        );
    } else if (daId == null) {
        content = <Redirect to={urlAdminDaQueue(repositories[0].id)} />;
    } else {
        const repositoryId = Number(daId);
        const current = repositories.find(repository => repository.id === repositoryId);
        const selector = repositories.length > 1 || current == null ? (
            <Dropdown
                aria-label={intl.formatMessage(messages.repository)}
                placeholder={intl.formatMessage(messages.repository)}
                value={current?.name ?? ''}
                selectedOptions={current ? [String(current.id)] : []}
                onOptionSelect={(_, { optionValue }) => optionValue && history.push(urlAdminDaQueue(Number(optionValue)))}
            >
                {repositories.map(repository => (
                    <Option key={repository.id} value={String(repository.id)}>{repository.name}</Option>
                ))}
            </Dropdown>
        ) : <strong>{current.name}</strong>;
        // a new key per repository resets the filter and the selection
        content = <DaQueueList key={repositoryId} repositoryId={repositoryId} repositorySelector={selector} />;
    }

    return <AdminLayout ribbon={<Ribbon />} centerPanel={content} />;
}

export default AdminDaQueuePage;
