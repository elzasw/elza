import { NodeItem } from 'elza-api';
import { useIntl } from 'react-intl';
import { addToastrDanger, addToastrSuccess } from 'components/shared/toastr/ToastrActions';
import { useAppThunkDispatch } from 'utils/hooks';
import { appendItemClipboard, createClipboardItems, readItemClipboard, writeItemClipboard } from './itemClipboard';
import { clipboardMessages } from './messages';

interface CopyItemsParams {
    descItems: NodeItem[];
    nodeId: number;
    fundId: number;
    fundVersionId: number;
    /** Copies only this DescItemType; all own DescItems of the node when omitted. */
    itemTypeId?: number;
    /** Adds the items to the clipboard (replacing the same types) instead of replacing its content. */
    append?: boolean;
}

/** Copies the node's own DescItems into the item clipboard and reports the result in a toast. */
export function useCopyItems() {
    const dispatch = useAppThunkDispatch();
    const { formatMessage } = useIntl();

    return function copyItems({ descItems, nodeId, fundId, fundVersionId, itemTypeId, append = false }: CopyItemsParams) {
        const items = createClipboardItems(descItems, nodeId, itemTypeId);
        const source = { fundId, fundVersionId, sourceNodeId: nodeId };
        const isWritten = append ? appendItemClipboard(source, items) : writeItemClipboard(source, items);

        if (!isWritten) {
            dispatch(addToastrDanger(formatMessage(clipboardMessages.copyFailed)));
            return;
        }

        const totalCount = readItemClipboard()?.items.length ?? items.length;
        const total = append ? formatMessage(clipboardMessages.clipboardTotal, { count: totalCount }) : null;
        dispatch(addToastrSuccess(formatMessage(clipboardMessages.copied, { count: items.length }), total));
    };
}
