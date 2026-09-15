import { Button, makeStyles, tokens } from '@fluentui/react-components';
import { ArrowDownload20Filled } from '@fluentui/react-icons';
import { useIntl } from 'react-intl';
import { AipDetailVO } from 'elza-api';

import { AipDetailBody } from '../AipDetailBody';
import { detailMessages } from '../messages';
import { packageDownloadUrl } from './packageUrls';

interface Props {
    detail: AipDetailVO;
    /** Shows the file of the package the problem is about. */
    onOpenProblemFile: (file: string) => void;
}

const useStyles = makeStyles({
    root: {
        maxWidth: '640px',
        padding: tokens.spacingVerticalL,
        overflow: 'auto',
    },
    actions: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
        marginBottom: tokens.spacingVerticalL,
    },
});

/**
 * The package as a whole: what ELZA holds of the AIP, whether the whole package is linked to
 * a description node, and the problem that stopped its processing, if any. The parts of the
 * package and its files have their own tabs.
 */
export function AipOverview({ detail, onOpenProblemFile }: Props) {
    const { formatMessage } = useIntl();
    const styles = useStyles();

    return (
        <div className={styles.root}>
            {/* The download needs the package on disk; the load flag says whether there is one. */}
            {detail.metadataLoad && (
                <div className={styles.actions}>
                    <Button as="a" href={packageDownloadUrl(detail.aipId)} download
                            icon={<ArrowDownload20Filled />}>
                        {formatMessage(detailMessages.downloadPackage)}
                    </Button>
                </div>
            )}
            <AipDetailBody detail={detail} onOpenProblemFile={onOpenProblemFile} />
        </div>
    );
}

export type AipOverviewProps = Props;
