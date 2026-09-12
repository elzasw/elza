/**
 * Doplnění chybějícího exportu v @types/react-router-bootstrap.
 *
 * Balíček react-router-bootstrap exportuje i `IndexLinkContainer`
 * (lib/IndexLinkContainer.js - `LinkContainer` s `exact: true`), komunitní
 * typy z DefinitelyTyped ale deklarují pouze `LinkContainer`. V .jsx souborech
 * to nevadilo, při převodu na .tsx ano.
 */
declare module 'react-router-bootstrap' {
    import type { ComponentClass } from 'react';
    import LinkContainerDefault from 'react-router-bootstrap/lib/LinkContainer';

    export { default as LinkContainer } from 'react-router-bootstrap/lib/LinkContainer';

    type LinkContainerProps = React.ComponentProps<typeof LinkContainerDefault>;

    export const IndexLinkContainer: ComponentClass<LinkContainerProps>;
}
