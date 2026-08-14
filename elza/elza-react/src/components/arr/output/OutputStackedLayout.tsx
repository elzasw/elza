import {makeStyles, tokens} from '@fluentui/react-components';
import {i18n} from 'components/shared';
import ToggleContent from '../../shared/toggle-content/ToggleContent';
import {OutputDefinition} from './OutputDefinition';
import {OutputLayoutProps} from './outputLayoutTypes';

const useStyles = makeStyles({
    definition: {
        padding: `${tokens.spacingVerticalL} ${tokens.spacingHorizontalXL}`,
    },
});

/**
 * Původní rozvržení detailu výstupu – vše v jednom svislém sloupci.
 */
export function OutputStackedLayout({form, ...definitionProps}: OutputLayoutProps) {
    const styles = useStyles();

    return (
        <>
            <div className={styles.definition}>
                <OutputDefinition {...definitionProps} />
                <hr className="small" />
                <h4 className="desc-items-title">{i18n('developer.title.descItems')}</h4>
            </div>
            <ToggleContent opened={true} withText>
                {form}
            </ToggleContent>
        </>
    );
}
