import PropTypes from 'prop-types';
import { useEffect, useState } from 'react';
import { connect } from 'react-redux';
import { ButtonToolbar, Table } from 'react-bootstrap';
import { Button } from '../ui';
import { AbstractReactComponent, Icon, i18n } from 'components/shared';

import { deletePackage, getPackagesFetchIfNeeded } from 'actions/admin/packages.jsx';
import { downloadFile } from '../../actions/global/download';

import {
    Button as FluentButton,
    Table as FluentTable,
    TableBody,
    TableCell,
    TableCellLayout,
    TableHeader,
    TableHeaderCell,
    TableRow,
    createTableColumn,
    useArrowNavigationGroup,
    useTableFeatures,
    useTableSort,
    useTableColumnSizing_unstable,
} from "@fluentui/react-components";
import { useThunkDispatch } from 'utils/hooks';
import { useSelector } from 'react-redux';

/**
 * Komponenta pro zobrazení naimportovaných balíčků.
 */
class AdminPackagesList extends AbstractReactComponent {
    static propTypes = {
        getExportUrl: PropTypes.func.isRequired,
        items: PropTypes.array.isRequired,
    };

    componentDidMount() {
        this.props.dispatch(getPackagesFetchIfNeeded());
    }

    handleDeletePackage(code) {
        this.props.dispatch(deletePackage(code));
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(getPackagesFetchIfNeeded());
    }

    handleDownload = code => {
        const { getExportUrl } = this.props;
        this.props.dispatch(downloadFile(getExportUrl(code)));
    };

    render() {
        const { items } = this.props;

        let mapItems = {};
        items.forEach(item => (mapItems[item.code] = item));

        return (
            <Table striped bordered condensed hover>
                <thead>
                    <tr>
                        <th>{i18n('admin.packages.label.code')}</th>
                        <th>{i18n('admin.packages.label.name')}</th>
                        <th>{i18n('admin.packages.label.version')}</th>
                        <th>{i18n('admin.packages.label.description')}</th>
                        <th>{i18n('admin.packages.label.dependencies')}</th>
                        <th>{i18n('admin.packages.label.dependencies.by')}</th>
                        <th>{i18n('admin.packages.label.action')}</th>
                    </tr>
                </thead>
                <tbody>
                    {items.map(item => (
                        <tr key={item.code}>
                            <td>{item.code}</td>
                            <td>{item.name}</td>
                            <td>{item.version}</td>
                            <td>{item.description}</td>
                            <td>
                                {item.dependencies &&
                                    item.dependencies.map((dependency, index) => (
                                        <span title={mapItems[dependency.code].name} key={index}>
                                            {dependency.code} ({dependency.version}/{mapItems[dependency.code].version})
                                            <br />
                                        </span>
                                    ))}
                            </td>
                            <td>
                                {item.dependenciesBy &&
                                    item.dependenciesBy.map((dependency, index) => (
                                        <span title={mapItems[dependency.code].name} key={index}>
                                            {dependency.code}
                                            <br />
                                        </span>
                                    ))}
                            </td>
                            <td>
                                <ButtonToolbar>
                                    <Button onClick={() => this.handleDownload(item.code)} bsSize="xsmall">
                                        {i18n('global.action.download')}
                                    </Button>
                                    <Button onClick={this.handleDeletePackage.bind(this, item.code)} bsSize="xsmall">
                                        {i18n('global.action.delete')}
                                    </Button>
                                </ButtonToolbar>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </Table>
        );
    }
}

const columns = [
    createTableColumn({
        columnId: "code",
        compare: (a, b) => {
            return a.code.localeCompare(b.code);
        },
        renderCell: ({ code }) => code
    }),
    createTableColumn({
        columnId: "name",
        compare: (a, b) => {
            return a.name.localeCompare(b.name);
        },
        renderCell: ({ name }) => name
    }),
    createTableColumn({
        columnId: "version",
        compare: (a, b) => {
            return a.version - b.version;
        },
        renderCell: ({ version }) => version
    }),
    createTableColumn({
        columnId: "description",
        compare: (a, b) => {
            return a.description.localeCompare(b.description);
        },
        renderCell: ({ description }) => description
    }),
    createTableColumn({
        columnId: "dependencies",
        renderCell: ({ dependencies, name }, otherRows) => {
            return dependencies?.map((dependency, index) => {
                const currentVersion = otherRows.find(({ item }) => item.code == dependency.code)?.item.version;
                return (
                    <span title={name} key={index}>
                        {dependency.code} ({dependency.version}/{currentVersion})
                        <br />
                    </span>
                )
            }
            )
        }
    }),
    createTableColumn({
        columnId: "dependencies.by",
        renderCell: ({ dependenciesBy, name }) => {
            return dependenciesBy?.map((dependency, index) => (
                <span title={name} key={index}>
                    {dependency.code}
                    <br />
                </span>
            ))
        }
    }),
    createTableColumn({
        columnId: "action",
    }),
];

export function AdminPackagesListFn({ getExportUrl }) {
    const dispatch = useThunkDispatch();
    const [columnSizingOptions] = useState({
        code: {
            idealWidth: 80,
            minWidth: 80
        },
        name: {
            idealWidth: 200,
            minWidth: 200
        },
        version: {
            idealWidth: 50,
            minWidth: 50
        },
        description: {
            idealWidth: 1600,
            minWidth: 100,
        },
        dependencies: {
            idealWidth: 300,
            minWidth: 300,
        },
        action: {
            minWidth: 100,
            idealWidth: 100,
        }
    })
    const items = useSelector(({ adminRegion }) => adminRegion.packages.items)

    const {
        getRows,
        sort: { getSortDirection, toggleColumnSort, sort },
        columnSizing_unstable,
        tableRef
    } = useTableFeatures(
        {
            columns,
            items,
        },
        [
            useTableSort({
                defaultSortState: { sortColumn: "file", sortDirection: "ascending" },
            }),
            useTableColumnSizing_unstable({ columnSizingOptions })
        ]
    );

    const rows = sort(
        getRows((row) => {
            return { ...row };
        })
    );

    const headerSortProps = (columnId) => ({
        onClick: (e) => {
            toggleColumnSort(e, columnId);
        },
        sortDirection: getSortDirection(columnId),
    });

    const handleDownload = code => {
        dispatch(downloadFile(getExportUrl(code)));
    };

    const handleDeletePackage = (code) => {
        dispatch(deletePackage(code));
    }

    const keyboardNavAttr = useArrowNavigationGroup({ axis: "grid" });

    useEffect(() => {
        dispatch(getPackagesFetchIfNeeded())
    }, [])

    return (
        <FluentTable
            {...keyboardNavAttr}
            size='small'
            role="grid"
            sortable
            aria-label="DataGrid implementation with Table primitives"
            ref={tableRef}
            {...columnSizing_unstable.getTableProps()}
        >
            <TableHeader>
                <TableRow>
                    {columns.map(({ columnId }) => {
                        return <TableHeaderCell
                            {...headerSortProps(columnId)}
                            {...columnSizing_unstable.getTableHeaderCellProps(columnId)}
                        >
                            {i18n(`admin.packages.label.${columnId}`)}
                        </TableHeaderCell>
                    })}
                </TableRow>
            </TableHeader>
            <TableBody>
                {rows.map(({ item, onClick, onKeyDown, appearance }) => (
                    <TableRow
                        key={item.packageId}
                        onClick={onClick}
                        onKeyDown={onKeyDown}
                        appearance={appearance}
                    >
                        {columns.map(({ columnId, renderCell }) => {
                            const value = item[columnId];

                            if (columnId === "action") {
                                return <TableCell tabIndex={0} role="gridcell">
                                    <TableCellLayout {...columnSizing_unstable.getTableCellProps("action")}>
                                        <FluentButton
                                            icon={<Icon glyph="fa-download" />}
                                            title={i18n('global.action.download')}
                                            onClick={() => handleDownload(item.code)}
                                        />
                                        <FluentButton
                                            icon={<Icon glyph="fa-trash" />}
                                            title={i18n('global.action.delete')}
                                            onClick={() => handleDeletePackage(item.code)}
                                        />
                                    </TableCellLayout>
                                </TableCell>
                            }

                            return <TableCell tabIndex={0} role="gridcell">
                                <TableCellLayout truncate={false} {...columnSizing_unstable.getTableCellProps(columnId)}>
                                    {renderCell ? renderCell(item, rows) : value}
                                </TableCellLayout>
                            </TableCell>
                        })}
                    </TableRow>
                ))}
            </TableBody>
        </FluentTable>
    );
}

export default connect()(AdminPackagesList);
