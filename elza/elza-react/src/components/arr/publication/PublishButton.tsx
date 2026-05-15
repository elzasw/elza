import {
    Button,
    Menu,
    MenuButton,
    MenuItem,
    MenuList,
    MenuPopover,
    MenuTrigger,
} from "@fluentui/react-components";
import { defineMessages, useIntl } from "react-intl";
import { PublicationType } from "elza-api";
import { Api } from "api/api";

const messages = defineMessages({
    btnPublish:      { id: "publication.btn.publish",      defaultMessage: "Publikovat: {name}" },
    btnPublishOther: { id: "publication.btn.publishOther", defaultMessage: "Publikovat: ..." },
});

interface Props {
    fundId: number;
    types: PublicationType[];
    disabledTypeIds?: Set<number>;
    onPublish: () => void;
}

const STATIC_PUBLICATION_TYPES = 2;

export function PublishButton({ fundId, types, disabledTypeIds, onPublish }: Props) {
    const { formatMessage } = useIntl();

    const handlePublish = async (publicationTypeId: number) => {
        await Api.publication.fundPublicationCreateFundPublication(fundId, { publicationTypeId });
        onPublish();
    };

    const activeTypes = types.filter((t) => t.active ?? true);

    if (activeTypes.length === 0) {
        return null;
    }

    // const [first, ...rest] = activeTypes;
    const staticTypes = STATIC_PUBLICATION_TYPES > 0 ? activeTypes.slice(0, STATIC_PUBLICATION_TYPES) : [];
    const rest = activeTypes.slice(staticTypes.length);

    return (
        <>
            {staticTypes.map((type) => {
                return <Button appearance="primary" disabled={disabledTypeIds?.has(type.id!)} onClick={() => handlePublish(type.id!)}>
                    {formatMessage(messages.btnPublish, { name: type.name })}
                </Button>
            })}
            {rest.length > 0 && (
                <Menu>
                    <MenuTrigger disableButtonEnhancement>
                        <MenuButton appearance="secondary">{formatMessage(messages.btnPublishOther)}</MenuButton>
                    </MenuTrigger>
                    <MenuPopover>
                        <MenuList>
                            {rest.map((type) => (
                                <MenuItem key={type.id} disabled={disabledTypeIds?.has(type.id!)} onClick={() => handlePublish(type.id!)}>
                                    {type.name}
                                </MenuItem>
                            ))}
                        </MenuList>
                    </MenuPopover>
                </Menu>
            )}
        </>
    );
}
