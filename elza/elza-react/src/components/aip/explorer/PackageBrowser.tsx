import { useEffect, useMemo, useState } from 'react';
import { Button } from 'react-bootstrap';
import { Tree, TreeItem, TreeItemLayout } from '@fluentui/react-components';
import { AddSquare16Regular, SubtractSquare16Regular } from '@fluentui/react-icons';
import { FormattedMessage, useIntl } from 'react-intl';

import { AipPackageEntry, AipProblemType } from 'elza-api';
import { Icon, Splitter } from 'components/shared';
import { Api } from '../../../api';
import { formatAipSize } from '../format';
import { packageMessages, problemMessages } from '../messages';
import { buildPackageTree, folderPaths, PackageNode } from './packageTree';
import { packageDownloadUrl, packageEntryUrl } from './packageUrls';
import './PackageBrowser.scss';

type Props = {
    aipId: number;
    /** Zjištěný problém AIPu; balíček se prohlíží hlavně kvůli němu. */
    problemType?: AipProblemType;
    problemDescription?: string;
    /** Soubor balíčku, kterého se problém týká, pokud se týká jednoho. */
    problemFile?: string;
};

/** Soubory, které lze rozumně zobrazit jako text. */
const TEXT_SUFFIXES = ['.xml', '.txt', '.json', '.csv', '.md'];

const isText = (path: string) => TEXT_SUFFIXES.some(suffix => path.toLowerCase().endsWith(suffix));

/**
 * Prohlížeč staženého balíčku tak, jak přišel z digitálního archivu.
 *
 * Nezávisí na zpracování balíčku, takže je k dispozici i tehdy, když zpracování selhalo -
 * právě to je situace, kdy je potřeba se do balíčku podívat. Popis problému je proto vidět
 * rovnou nad obsahem balíčku a soubor, kterého se týká, otevře kliknutí na jeho cestu.
 */
export function PackageBrowser({aipId, problemType, problemDescription, problemFile}: Props) {
    const intl = useIntl();
    const [entries, setEntries] = useState<AipPackageEntry[] | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [selected, setSelected] = useState<PackageNode | null>(null);
    const [content, setContent] = useState<string | null>(null);

    const tree = useMemo(() => entries ? buildPackageTree(entries) : [], [entries]);
    const openFolders = useMemo(() => folderPaths(tree), [tree]);
    /**
     * Balíček uvádí cestu souboru vůči kořeni balíčku, položky ZIPu ji navíc mají pod složkou
     * pojmenovanou kódem AIPu - hledá se tedy i podle konce cesty. Odkaz vznikne jen na
     * položku, kterou balíček opravdu obsahuje.
     */
    const problemEntry = useMemo(() => (entries ?? []).find(entry =>
        problemFile && (entry.path === problemFile || entry.path.endsWith(`/${problemFile}`))),
        [problemFile, entries]);

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
        fetch(packageEntryUrl(aipId, selected.path))
            .then(response => response.text())
            .then(text => { if (current) { setContent(text); } })
            .catch(() => { if (current) { setContent(intl.formatMessage(packageMessages.readFailed)); } });
        return () => { current = false; };
    }, [aipId, selected]);

    const problem = problemType && (
        <div className="package-problem">
            <div className="package-problem-title">
                <Icon glyph="fa-exclamation-triangle"/>
                <FormattedMessage {...problemMessages[problemType]}/>
            </div>
            {problemDescription && <p className="package-problem-description">{problemDescription}</p>}
            {problemEntry && <p className="package-problem-files">
                <FormattedMessage {...packageMessages.problemHint}/>
                <button type="button" className="package-problem-file"
                        onClick={() => setSelected({
                            name: problemEntry.path.substring(problemEntry.path.lastIndexOf('/') + 1),
                            path: problemEntry.path,
                            size: problemEntry.size,
                        })}>
                    {problemEntry.path}
                </button>
            </p>}
        </div>
    );

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

    const downloadAll = (
        <Button as="a" size="sm" variant="outline-secondary" href={packageDownloadUrl(aipId)} download>
            <FormattedMessage {...packageMessages.downloadAll}/>
        </Button>
    );

    if (entries.length === 0) {
        return (
            <div className="package-browser-empty">
                {problem}
                <div className="package-browser-message"><FormattedMessage {...packageMessages.empty}/></div>
            </div>
        );
    }

    return (
        <div className="package-browser">
            {problem}
            <div className="package-browser-body">
                <Splitter
                    leftSize={360}
                    left={
                        <div className="package-tree-pane">
                            <div className="package-tree-header">{downloadAll}</div>
                            <Tree aria-label={intl.formatMessage(packageMessages.treeLabel)}
                                  defaultOpenItems={openFolders}
                                  className="package-tree">
                                {renderNodes(tree)}
                            </Tree>
                        </div>
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
                                            href={packageEntryUrl(aipId, selected.path)} download>
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
        </div>
    );
}

export default PackageBrowser;
