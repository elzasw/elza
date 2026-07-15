import PageLayout from './PageLayout';
import { AdminNav } from 'components/admin/AdminNav';

interface Props {
    [prop: string]: unknown;
}

export function AdminLayout(props: Props) {
    return <PageLayout {...props} sidebar={<AdminNav />} />;
}

export type AdminLayoutProps = Props;
