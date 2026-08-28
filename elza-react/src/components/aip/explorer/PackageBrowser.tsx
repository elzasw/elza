import { useEffect, useMemo, useState } from 'react';
import { Button } from 'react-bootstrap';
import { Tree, TreeItem, TreeItemLayout } from '@fluentui/react-components';
import { AddSquare16Regular, SubtractSquare16Regular } from '@fluentui/react-icons';
import { FormattedMessage, useIntl } from 'react-intl';

import { AipPackageEntry } from 'elza-api';
import { Splitter } from 'components/shared';
import { Api, serverContextPath } from '../../../api';
import { formatAipSize } from '../format';
import { packageMessages } from '../messages';
import { buildPackageTree, folderPaths, PackageNode } from './packageTree';
import './PackageBrowser.scss';

type Props = {
    aipId: number;
};

/** Soubory, které lze rozumně zobrazit jako text. */
const TEXT_SUFFIXES = ['.xml', '.txt', '.json', '.csv', '.md'];

const isText = (path: string) => TEXT_SUFFIXES.some(suffix => path.toLowerCase().endsWith(suffix));

const entryUrl = (aipId: number, path: string) =>
    `${serverContextPath}/api/v1/aip/${aipId}/package/content?path=${encodeURIComponent(path)}`;

/**
 * Prohlížeč staženého balíčku tak, jak přišel z digitálního archivu.
 *
 * Nezávisí na zpracování balíčku, takže je k dispozici i tehdy, když zpracování selhalo -
 * právě to je situace, kdy je potřeba se do balíčku podívat.
 */
export function PackageBrowser({aipId}: Props) {
    const intl = useIntl();
    const [entries, setEntries] = useState<AipPackageEntry[] | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [selected, setSelected] = useState<PackageNode | null>(null);
    const [content, setContent] = useState<string | null>(null);

    const tree = useMemo(() => entries ? buildPackageTree(entries) : [], [entries]);
    const openFolders = useMemo(() => folderPaths(tree), [tree]);

    useEffect(() => {
        setEntries(null);
        setError(null);
        setSelected(null);
        Api.aips.aipListPackageEntries(aipId)
            .then(response => setEntries(response.data))
            .catch(() => setError(intl.formatMessage(packageMessages.notDownloaded)));
    }, [aipId]);

    useEffect(() => {
        setContent(null);
        if (!selected || !isText(selected.path)) {
            return;
        }
        let current = true;
        fetch(entryUrl(aipId, selected.path))
            .then(response => response.text())
            .then(text => { if (current) { setContent(text); } })
            .catch(() => { if (current) { setContent(intl.formatMessage(packageMessages.readFailed)); } });
        return () => { current = false; };
    }, [aipId, selected]);

    const renderNodes = (nodes: PackageNode[]) => nodes.map(node => node.children
        ? (
            <TreeItem key={node.path} itemType="branch" value={node.path}>
                <TreeItemLayout
                    expandIcon={openFolders.includes(node.path)
                        ? <SubtractSquare16Regular color="black"/>
                        : <AddSquare16Regular color="black"/>}
                >
                    {node.name}
                </TreeItemLayout>
                <Tree>{renderNodes(node.children)}</Tree>
            </TreeItem>
        )
        : (
            <TreeItem key={node.path} itemType="leaf" value={node.path}>
                <TreeItemLayout
                    className={node.path === selected?.path ? 'entry selected' : 'entry'}
                    onClick={() => setSelected(node)}
                >
                    <span className="entry-name">{node.name}</span>
                    <span className="entry-size">{formatAipSize(node.size)}</span>
                </TreeItemLayout>
            </TreeItem>
        ));

    if (error) {
        return <div className="package-browser-message">{error}</div>;
    }
    if (!entries) {
        return <div className="package-browser-message"><FormattedMessage {...packageMessages.loading}/></div>;
    }
    if (entries.length === 0) {
        return <div className="package-browser-message"><FormattedMessage {...packageMessages.empty}/></div>;
    }

    return (
        <div className="package-browser">
            <Splitter
                leftSize={360}
                left={
                    <Tree aria-label={intl.formatMessage(packageMessages.treeLabel)}
                          defaultOpenItems={openFolders}
                          className="package-tree">
                        {renderNodes(tree)}
                    </Tree>
                }
                center={
                    <div className="package-preview">
                        {!selected && <div className="package-browser-message">
                            <FormattedMessage {...packageMessages.selectFile}/>
                        </div>}
                        {selected && <>
                            <div className="package-preview-header">
                                <span>{selected.path}</span>
                                <Button as="a" size="sm" variant="outline-secondary"
                                        href={entryUrl(aipId, selected.path)} download>
                                    <FormattedMessage {...packageMessages.download}/>
                                </Button>
                            </div>
                            {isText(selected.path)
                                ? <pre className="package-preview-content">{content}</pre>
                                : <div className="package-browser-message">
                                    <FormattedMessage {...packageMessages.notPreviewable}/>
                                </div>}
                        </>}
                    </div>
                }
            />
        </div>
    );
}

export default PackageBrowser;
