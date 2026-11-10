import { MouseEvent, ReactNode, useEffect, useMemo } from 'react';
import { useHistory } from 'react-router-dom';
import {
    Menu,
    MenuItem,
    MenuItemLink,
    MenuList,
    MenuPopover,
    MenuProps,
    PositioningVirtualElement,
} from '@fluentui/react-components';
import { contextMenuHide } from 'actions/global/contextMenu';
import { useAppThunkDispatch } from 'utils/hooks';

interface Position {
    x: number;
    y: number;
}

export interface ContextMenuItem {
    key: string;
    label: ReactNode;
    /** Route inside the app; the item is a link, so it can also open in a new browser tab. */
    to?: string;
    onClick?: () => void;
}

interface Props {
    visible: boolean;
    /** Cursor position in window coordinates. */
    position: Position | null;
    menu: ContextMenuItem[] | null;
    /** Component that opened the menu; kept in the store only. */
    component?: unknown;
}

export type ContextMenuProps = Props;

function isPlainLeftClick(event: MouseEvent) {
    return event.button === 0 && !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey;
}

/**
 * The global context menu, opened at the cursor through `contextMenuShow`. Fluent positioning keeps
 * the whole menu in the window, and the menu closes on a click outside it, Escape, a chosen item,
 * a window resize or a document scroll.
 */
export function ContextMenu({ visible, position, menu }: Props) {
    const dispatch = useAppThunkDispatch();
    const history = useHistory();
    const cursorX = position?.x;
    const cursorY = position?.y;

    const target = useMemo<PositioningVirtualElement | undefined>(() => {
        if (cursorX === undefined || cursorY === undefined) {
            return undefined;
        }
        const cursorRect = {
            x: cursorX,
            y: cursorY,
            top: cursorY,
            left: cursorX,
            bottom: cursorY,
            right: cursorX,
            width: 0,
            height: 0,
        };
        return { getBoundingClientRect: () => cursorRect };
    }, [cursorX, cursorY]);

    const isOpen = visible && target !== undefined && menu !== null && menu.length > 0;

    useEffect(() => {
        if (!isOpen) {
            return;
        }
        const hideMenu = () => {
            dispatch(contextMenuHide());
        };
        window.addEventListener('resize', hideMenu);
        document.addEventListener('scroll', hideMenu);
        return () => {
            window.removeEventListener('resize', hideMenu);
            document.removeEventListener('scroll', hideMenu);
        };
    }, [isOpen, dispatch]);

    const handleOpenChange: MenuProps['onOpenChange'] = (_event, data) => {
        if (!data.open) {
            dispatch(contextMenuHide());
        }
    };

    if (!isOpen) {
        return null;
    }

    return (
        <Menu open onOpenChange={handleOpenChange} positioning={{ target, position: 'below', align: 'start' }}>
            <MenuPopover>
                <MenuList>
                    {menu.map((item) => {
                        const { key, label, to, onClick } = item;
                        if (to !== undefined) {
                            const handleLinkClick = (event: MouseEvent) => {
                                onClick?.();
                                if (isPlainLeftClick(event)) {
                                    event.preventDefault();
                                    history.push(to);
                                }
                            };
                            return (
                                <MenuItemLink
                                    key={key}
                                    href={history.createHref({ pathname: to })}
                                    onClick={handleLinkClick}
                                >
                                    {label}
                                </MenuItemLink>
                            );
                        }
                        return (
                            <MenuItem key={key} onClick={onClick}>
                                {label}
                            </MenuItem>
                        );
                    })}
                </MenuList>
            </MenuPopover>
        </Menu>
    );
}
