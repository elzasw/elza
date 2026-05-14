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
    btnPublish:      { id: "publication.btn.publish",      defaultMessage: "Publikovat do {name}" },
    btnPublishOther: { id: "publication.btn.publishOther", defaultMessage: "Publikovat..." },
});

interface Props {
    fundId: number;
    types: PublicationType[];
    onPublish: () => void;
}

export function PublishButton({ fundId, types, onPublish }: Props) {
    const { formatMessage } = useIntl();

    const handlePublish = async (publicationTypeId: number) => {
        await Api.publication.fundPublicationCreateFundPublication(fundId, { publicationTypeId });
        onPublish();
    };

    const activeTypes = types.filter((t) => t.active ?? true);

    if (activeTypes.length === 0) {
        return null;
    }

    const [first, ...rest] = activeTypes;

    return (
        <>
            <Button appearance="primary" onClick={() => handlePublish(first.id!)}>
                {formatMessage(messages.btnPublish, { name: first.name })}
            </Button>
            {rest.length > 0 && (
                <Menu>
                    <MenuTrigger disableButtonEnhancement>
                        <MenuButton appearance="secondary">{formatMessage(messages.btnPublishOther)}</MenuButton>
                    </MenuTrigger>
                    <MenuPopover>
                        <MenuList>
                            {rest.map((type) => (
                                <MenuItem key={type.id} onClick={() => handlePublish(type.id!)}>
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
