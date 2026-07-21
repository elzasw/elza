import { useContext } from 'react';
import { DataGridViewContext } from './DataGridViewProvider';
import { DataGridViewContextValue } from './types';

export function useDataGridView(): DataGridViewContextValue {
    const context = useContext(DataGridViewContext);
    if (!context) {
        throw new Error('useDataGridView must be used within a DataGridViewProvider');
    }
    return context;
}
