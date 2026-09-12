/**
 * Hlavní rozcestník aplikace - první skupina v ribbonu.
 *
 * Vyčleněno z Ribbon.jsx při převodu na react-intl: jde o samostatný, čistě
 * prezentační blok, který potřebuje jen `userDetail`, takže ho lze mít jako
 * typovanou funkční komponentu bez `connect`. Ribbon.jsx zůstává legacy třídou
 * a ubývá po částech.
 */
import React from 'react';
import { FormattedMessage, defineMessages } from 'react-intl';
import { IndexLinkContainer, LinkContainer } from 'react-router-bootstrap';
import { Button as BootstrapButton } from 'react-bootstrap';

import { Icon, RibbonGroup } from 'components/shared';
import { Button } from '../ui';
import * as perms from 'actions/user/Permission.jsx';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { URL_ADMIN, URL_AIP, URL_ENTITY, URL_FUND } from '../../constants';

// Ids jsou převzaty z legacy katalogu beze změny - přejmenování id při migraci
// zahodí jeho překlady při dalším locale:merge.
const messages = defineMessages({
    home: {
        id: 'ribbon.action.home',
        defaultMessage: 'Domů',
    },
    fund: {
        id: 'ribbon.action.fund',
        defaultMessage: 'Archivní soubory',
    },
    aip: {
        id: 'ribbon.action.aip',
        defaultMessage: 'Archivní balíčky',
    },
    registry: {
        id: 'ribbon.action.registry',
        defaultMessage: 'Archivní entity',
    },
    admin: {
        id: 'ribbon.action.admin',
        defaultMessage: 'Administrace',
    },
});

/**
 * Ribbon si drží ref na výchozí tlačítko kvůli správě fokusu (`trySetFocus`),
 * proto ho komponenta propaguje dál na tlačítko "Domů".
 */
export const MainNavigation = React.forwardRef<HTMLButtonElement>(function MainNavigation(_props, ref) {
    const userDetail = useAppSelector(state => state.userDetail);

    const canSeeFunds = userDetail.hasOne(perms.FUND_RD_ALL, perms.FUND_RD);
    const canSeeAdmin = userDetail.hasOne(
        perms.ADMIN,
        perms.USR_PERM,
        perms.USER_CONTROL_ENTITY,
        perms.GROUP_CONTROL_ENTITY,
        perms.REPORT_ALL,
    );

    return (
        <RibbonGroup key="ribbon-group-main" className="large">
            <IndexLinkContainer key="ribbon-btn-home" to="/">
                <BootstrapButton ref={ref} variant={'default'}>
                    <Icon glyph="fa-home" />
                    <span className="btnText">
                        <FormattedMessage {...messages.home} />
                    </span>
                </BootstrapButton>
            </IndexLinkContainer>
            {canSeeFunds && (
                <LinkContainer key="ribbon-btn-fund" to={URL_FUND}>
                    <Button variant={'default'}>
                        <Icon glyph="fa-database" />
                        <span className="btnText">
                            <FormattedMessage {...messages.fund} />
                        </span>
                    </Button>
                </LinkContainer>
            )}
            <LinkContainer key="ribbon-btn-aip" to={URL_AIP}>
                <Button variant={'default'}>
                    <Icon glyph="fa-archive" />
                    <span className="btnText">
                        <FormattedMessage {...messages.aip} />
                    </span>
                </Button>
            </LinkContainer>
            <LinkContainer key="ribbon-btn-registry" to={URL_ENTITY}>
                <Button variant={'default'}>
                    <Icon glyph="fa-th-list" />
                    <span className="btnText">
                        <FormattedMessage {...messages.registry} />
                    </span>
                </Button>
            </LinkContainer>
            {canSeeAdmin && (
                <LinkContainer key="ribbon-btn-admin" to={URL_ADMIN}>
                    <Button variant={'default'}>
                        <Icon glyph="fa-cog" />
                        <span className="btnText">
                            <FormattedMessage {...messages.admin} />
                        </span>
                    </Button>
                </LinkContainer>
            )}
        </RibbonGroup>
    );
});
