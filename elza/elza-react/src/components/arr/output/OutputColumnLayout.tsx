import {makeStyles, tokens} from '@fluentui/react-components';
import {OutputDefinitionSection} from './OutputDefinitionSection';
import {OutputLayoutProps} from './outputLayoutTypes';

const useStyles = makeStyles({
    columns: {
        display: 'flex',
        alignItems: 'stretch',
        flex: '1 1 auto',
        minHeight: 0,
    },
    column: {
        overflowY: 'auto',
        minHeight: 0,
    },
    definition: {
        flex: '0 0 440px',
        minWidth: 0,
        padding: `${tokens.spacingVerticalM} ${tokens.spacingHorizontalL}`,
        borderRight: `1px solid ${tokens.colorNeutralStroke2}`,
    },
    items: {
        flex: '1 1 auto',
        minWidth: 0,
    },
});

/**
 * Nové rozvržení detailu výstupu – definice a prvky popisu vedle sebe ve dvou sloupcích.
 * Sloupec s prvky popisu vynechává nadpis a sbalování.
 */
export function OutputColumnLayout({form, ...definitionProps}: OutputLayoutProps) {
    const styles = useStyles();

    return (
        <div className={styles.columns}>
            <div className={`${styles.column} ${styles.definition}`}>
                <OutputDefinitionSection {...definitionProps} />
            </div>
            <div className={`${styles.column} ${styles.items}`}>{form}</div>
        </div>
    );
}
