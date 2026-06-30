import {
    Button,
    Menu,
    MenuButton,
    MenuItem,
    MenuList,
    MenuPopover,
    MenuTrigger,
    makeStyles,
    tokens,
} from "@fluentui/react-components";
import { defineMessages, useIntl } from "react-intl";
import { ConnectionType, PublicationType } from "elza-api";
import { Api } from "api/api";
import { useCanUsePublicationType } from "./hooks";
import { ConfirmDialog } from "./ConfirmDialog";

const useStyles = makeStyles({
    btnTest: {
        backgroundColor: tokens.colorPaletteYellowBackground3,
        // color: tokens.colorNeutralForeground1,
        ":hover": { backgroundColor: tokens.colorPaletteYellowBackground2 },
        ":active": { backgroundColor: tokens.colorPaletteYellowBackground2 },
    },
    btnDev: {
        backgroundColor: tokens.colorStatusDangerBackground3,
        ":hover": { backgroundColor: tokens.colorStatusDangerBackground3Hover },
        ":active": { backgroundColor: tokens.colorStatusDangerBackground3Pressed },
    },
});

const messages = defineMessages({
    btnPublish:        { id: "publication.btn.publish",        defaultMessage: "Publikovat: {name}" },
    btnPublishOther:   { id: "publication.btn.publishOther",   defaultMessage: "Publikovat: ..." },
    confirmPublish:    { id: "publication.btn.confirmPublish", defaultMessage: "Opravdu publikovat do {name}?" },
    confirmPublishBtn: { id: "publication.btn.confirmPublishBtn", defaultMessage: "Publikovat" },
});

interface Props {
    fundId: number;
    types: PublicationType[];
    disabledTypeIds?: Set<number>;
    onPublish: () => void;
}

const STATIC_PUBLICATION_TYPES = 3;

export function PublishButton({ fundId, types, disabledTypeIds, onPublish }: Props) {
    const { formatMessage } = useIntl();
    const classes = useStyles();
    const canUse = useCanUsePublicationType(fundId);

    const handlePublish = async (publicationTypeId: number) => {
        await Api.publication.fundPublicationCreateFundPublication(fundId, { publicationTypeId });
        onPublish();
    };

    const activeTypes = types.filter((t) => (t.active ?? true) && canUse(t));

    if (activeTypes.length === 0) {
        return null;
    }

    const staticTypes = STATIC_PUBLICATION_TYPES > 0 ? activeTypes.slice(0, STATIC_PUBLICATION_TYPES) : [];
    const rest = activeTypes.slice(staticTypes.length);

    return (
        <ConfirmDialog>
            {(confirm) => (
                <>
                    {staticTypes.map((type) => (
                        <Button
                            key={type.id}
                            appearance={type.connectionType === ConnectionType.Production ? "primary" : "secondary"}
                            disabled={disabledTypeIds?.has(type.id!)}
                            onClick={() => confirm({
                                text: formatMessage(messages.confirmPublish, { name: type.name }),
                                confirmLabel: formatMessage(messages.confirmPublishBtn),
                                onConfirm: () => handlePublish(type.id!),
                            })}
                        >
                            {formatMessage(messages.btnPublish, { name: type.name })}
                        </Button>
                    ))}
                    {rest.length > 0 && (
                        <Menu>
                            <MenuTrigger disableButtonEnhancement>
                                <MenuButton appearance="secondary">{formatMessage(messages.btnPublishOther)}</MenuButton>
                            </MenuTrigger>
                            <MenuPopover>
                                <MenuList>
                                    {rest.map((type) => (
                                        <MenuItem
                                            key={type.id}
                                            disabled={disabledTypeIds?.has(type.id!)}
                                            onClick={() => confirm({
                                                text: formatMessage(messages.confirmPublish, { name: type.name }),
                                                confirmLabel: formatMessage(messages.confirmPublishBtn),
                                                onConfirm: () => handlePublish(type.id!),
                                            })}
                                        >
                                            {type.name}
                                        </MenuItem>
                                    ))}
                                </MenuList>
                            </MenuPopover>
                        </Menu>
                    )}
                </>
            )}
        </ConfirmDialog>
    );
}
