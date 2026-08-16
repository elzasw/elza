import { Fragment } from "react";
import { Button, makeStyles, tokens, Table, TableBody, TableCell, TableHeader, TableHeaderCell, TableRow } from "@fluentui/react-components";
import { FormattedMessage } from "react-intl";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import remarkBreaks from "remark-breaks";
import rehypeSanitize from "rehype-sanitize";
import { AiDisplayBlock, AiDisplayBlockType, AiFollowUpAction, AiMarkdownBlock, AiTextBlock, AiTableBlock as AiTableBlockVO, AiDocCitationsBlock, AiNodeUpdateProposalsBlock as AiNodeUpdateProposalsBlockVO, AiRequest } from "elza-api";
import { AiNodeUpdateProposalsBlock } from "./AiNodeUpdateProposalsBlock";
import { aiAssistantMessages } from "./messages";

const useStyles = makeStyles({
    text: {
        whiteSpace: "pre-wrap",
        wordBreak: "break-word",
        margin: 0,
        fontFamily: tokens.fontFamilyBase,
    },
    markdown: {
        wordBreak: "break-word",
        "& p:first-child": { marginTop: 0 },
        "& p:last-child": { marginBottom: 0 },
        "& h1": { fontSize: tokens.fontSizeHero700 },
        "& h2": { fontSize: tokens.fontSizeBase600 },
        "& h3": { fontSize: tokens.fontSizeBase500 },
        "& h4": { fontSize: tokens.fontSizeBase400 },
        "& h5, & h6": { fontSize: tokens.fontSizeBase300 },
        "& pre": {
            whiteSpace: "pre-wrap",
            background: tokens.colorNeutralBackground3,
            padding: tokens.spacingHorizontalS,
            borderRadius: tokens.borderRadiusMedium,
            overflowX: "auto",
        },
        "& code": { fontFamily: tokens.fontFamilyMonospace },
        "& table": { borderCollapse: "collapse" },
        "& th, & td": {
            border: `1px solid ${tokens.colorNeutralStroke2}`,
            padding: `2px ${tokens.spacingHorizontalXS}`,
        },
    },
    caption: {
        fontWeight: tokens.fontWeightSemibold,
        marginBottom: tokens.spacingVerticalXS,
    },
    unsupported: {
        color: tokens.colorNeutralForeground3,
        fontStyle: "italic",
    },
    citations: {
        marginTop: tokens.spacingVerticalS,
        paddingTop: tokens.spacingVerticalXS,
        borderTop: `1px solid ${tokens.colorNeutralStroke2}`,
    },
    citationsTitle: {
        fontSize: tokens.fontSizeBase200,
        fontWeight: tokens.fontWeightSemibold,
        color: tokens.colorNeutralForeground3,
        marginBottom: tokens.spacingVerticalXXS,
    },
    citation: {
        fontSize: tokens.fontSizeBase200,
    },
    citationSource: {
        color: tokens.colorNeutralForeground3,
    },
    followUps: {
        display: "flex",
        justifyContent: "flex-end",
        gap: tokens.spacingHorizontalS,
        marginTop: tokens.spacingVerticalXS,
        marginBottom: tokens.spacingVerticalS,
    },
});

interface Props {
    blocks: AiDisplayBlock[];
    /** Id of the exchange the blocks belong to; enables interactive blocks (proposals). */
    requestId?: number;
    /** Receives the refreshed exchange after an interactive block's server action. */
    onRequestUpdate?: (request: AiRequest) => void;
    /** Prefills the composer with a clarification quote. */
    onClarify?: (text: string) => void;
    /**
     * Submits a server-suggested follow-up (the block's `followUps` buttons) —
     * e.g. a check finding's "prepare the fix" action, which continues the
     * conversation with an `elza.enhanceDescription` exchange. Buttons render
     * only when provided.
     */
    onFollowUp?: (action: AiFollowUpAction) => void;
    /** Disables the follow-up buttons (an exchange is already running). */
    followUpDisabled?: boolean;
}

export function AiDisplayBlocks({ blocks, requestId, onRequestUpdate, onClarify, onFollowUp, followUpDisabled }: Props) {
    const styles = useStyles();

    const renderBlock = (block: AiDisplayBlock) => {
        switch (block.type) {
            case AiDisplayBlockType.Text:
                return <p className={styles.text}>{(block as AiTextBlock).content}</p>;
            case AiDisplayBlockType.Markdown:
                return (
                    <div className={styles.markdown}>
                        <ReactMarkdown remarkPlugins={[remarkGfm, remarkBreaks]} rehypePlugins={[rehypeSanitize]}>
                            {(block as AiMarkdownBlock).content}
                        </ReactMarkdown>
                    </div>
                );
            case AiDisplayBlockType.Table:
                return <AiTableBlock block={block as AiTableBlockVO} captionClassName={styles.caption} />;
            case AiDisplayBlockType.DocCitations:
                return <AiCitationsBlock block={block as AiDocCitationsBlock} styles={styles} />;
            case AiDisplayBlockType.NodeUpdateProposals:
                return (
                    <AiNodeUpdateProposalsBlock
                        block={block as AiNodeUpdateProposalsBlockVO}
                        requestId={requestId}
                        onRequestUpdate={onRequestUpdate}
                        onClarify={onClarify}
                    />
                );
            default:
                return (
                    <p className={styles.unsupported}>
                        <FormattedMessage {...aiAssistantMessages.unsupportedBlock} />
                    </p>
                );
        }
    };

    return (
        <>
            {blocks.map((block, index) => {
                const followUps = onFollowUp && block.followUps?.length ? block.followUps : null;
                return (
                    <Fragment key={index}>
                        {renderBlock(block)}
                        {followUps && (
                            <div className={styles.followUps}>
                                {followUps.map((action, actionIndex) => (
                                    <Button
                                        key={actionIndex}
                                        size="small"
                                        disabled={!!followUpDisabled}
                                        onClick={() => onFollowUp!(action)}
                                    >
                                        {action.label}
                                    </Button>
                                ))}
                            </div>
                        )}
                    </Fragment>
                );
            })}
        </>
    );
}

interface CitationsBlockProps {
    block: AiDocCitationsBlock;
    styles: ReturnType<typeof useStyles>;
}

function AiCitationsBlock({ block, styles }: CitationsBlockProps) {
    const citations = block.citations ?? [];
    if (citations.length === 0) return null;

    return (
        <div className={styles.citations}>
            <div className={styles.citationsTitle}>
                <FormattedMessage {...aiAssistantMessages.citations} />
            </div>
            {citations.map(citation => {
                const label = citation.title || citation.fragment;
                return (
                    <div key={citation.fragment} className={styles.citation}>
                        {citation.url ? (
                            <a href={citation.url} target="_blank" rel="noreferrer">{label}</a>
                        ) : (
                            <span>{label}</span>
                        )}
                        {citation.source && <span className={styles.citationSource}> — {citation.source}</span>}
                    </div>
                );
            })}
        </div>
    );
}

interface TableBlockProps {
    block: AiTableBlockVO;
    captionClassName: string;
}

function AiTableBlock({ block, captionClassName }: TableBlockProps) {
    const columns = block.columns ?? [];
    const rows = block.rows ?? [];

    return (
        <div>
            {block.caption && <div className={captionClassName}>{block.caption}</div>}
            <Table size="small">
                <TableHeader>
                    <TableRow>
                        {columns.map(column => (
                            <TableHeaderCell key={column.key}>{column.label}</TableHeaderCell>
                        ))}
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {rows.map((row, rowIndex) => (
                        <TableRow key={rowIndex}>
                            {columns.map(column => (
                                <TableCell key={column.key}>{row[column.key]}</TableCell>
                            ))}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    );
}
