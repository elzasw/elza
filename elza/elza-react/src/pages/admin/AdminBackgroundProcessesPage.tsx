import {BackgroundProcessList} from 'components/admin/background-processes';
import {Ribbon} from 'components/index.jsx';
import {AdminLayout} from '../shared/layout/AdminLayout';

export function AdminBackgroundProcessesPage() {
    return <AdminLayout ribbon={<Ribbon />} centerPanel={<BackgroundProcessList />} />;
}

export default AdminBackgroundProcessesPage;
