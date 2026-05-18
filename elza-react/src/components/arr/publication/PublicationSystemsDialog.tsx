import {
    Button,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
} from "@fluentui/react-components";
import { AddRegular, EyeOffRegular } from "@fluentui/react-icons";
import { useEffect, useState } from "react";
import { defineMessages, useIntl } from "react-intl";
import { ConnectionType, PublicationType } from "elza-api";
import { PublicationSystemDetail } from "./PublicationSystemDetail";
import { Api } from "api/api";
import { useDialogStyles } from "./styles";

const messages = defineMessages({
    title: { id: "publication.systems.dialog.title", defaultMessage: "Správa publikačních systémů" },
    btnAdd: { id: "publication.systems.dialog.add", defaultMessage: "Přidat" },
    btnClose: { id: "publication.systems.dialog.close", defaultMessage: "Zavřít" },
    newSystem: { id: "publication.systems.new", defaultMessage: "Nový typ" },
});


// const DUMMY_SYSTEMS: PublicationType[] = [
//     { id: 1, name: "Veřejný portál", code: "PUBLIC", retentionCount: 5, exportFilterCode: undefined, active: true, allowPermExport: true, allowPermPublication: true },
//     { id: 2, name: "Interní systém", code: "INTERNAL", retentionCount: 10, exportFilterCode: undefined, active: true, allowPermExport: true, allowPermPublication: false },
// ];

interface Props {
    open: boolean;
    onClose: () => void;
}

export type { Props as PublicationSystemsDialogProps };

export function PublicationSystemsDialog({ open, onClose }: Props) {
    const classes = useDialogStyles();
    const { formatMessage } = useIntl();

    const [systems, setSystems] = useState<PublicationType[]>([]);
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [editedSystem, setEditedSystem] = useState<PublicationType | null>(null);

    const isSameSystem = (a: PublicationType, b: PublicationType) =>
        a.id !== undefined ? a.id === b.id : a === b;

    useEffect(() => {
        if (!open) { return; }
        (async () => {
            const { data } = await Api.publication.publicationTypeAdminListPublicationTypes();
            setSystems(data);
            const first = data[0] ?? null;
            setSelectedId(first?.id ?? null);
            setEditedSystem(first ? { ...first } : null);
        })();
    }, [open]);

    const handleSelect = (system: PublicationType) => {
        setSelectedId(system.id ?? null);
        setEditedSystem(system.id !== undefined ? { ...system } : system);
    };

    const handleAdd = () => {
        const newSystem: PublicationType = {
            name: formatMessage(messages.newSystem),
            code: "",
            retentionCount: 5,
            exportFilterCode: undefined,
            active: true,
            allowPermExport: false,
            allowPermPublication: false,
            connectionType: ConnectionType.Development,
        };
        setSystems((prev) => [...prev, newSystem]);
        setSelectedId(null);
        setEditedSystem(newSystem);
    };

    const handleSave = async (values: PublicationType) => {
        if (values.id === undefined) {
            const { data: created } = await Api.publication.publicationTypeAdminCreatePublicationType(values);
            setSystems((prev) => prev.map((s) => isSameSystem(s, editedSystem!) ? created : s));
            setSelectedId(created.id ?? null);
            setEditedSystem({ ...created });
        } else {
            const { data: updated } = await Api.publication.publicationTypeAdminUpdatePublicationType(values.id, values);
            setSystems((prev) => prev.map((s) => isSameSystem(s, updated) ? updated : s));
            setEditedSystem({ ...updated });
        }
    };

    const handleToggleActive = async (system: PublicationType) => {
        const toggled = { ...system, active: !(system.active ?? true) };
        if (system.id !== undefined) {
            const { data } = await Api.publication.publicationTypeAdminUpdatePublicationType(system.id, toggled);
            setSystems((prev) => prev.map((s) => isSameSystem(s, data) ? data : s));
            if (editedSystem && isSameSystem(editedSystem, data)) { setEditedSystem({ ...data }); }
        } else {
            setSystems((prev) => prev.map((s) => s === system ? toggled : s));
            if (editedSystem === system) { setEditedSystem(toggled); }
        }
    };

    const handleRemove = async (system: PublicationType) => {
        if (system.id !== undefined) {
            await Api.publication.publicationTypeAdminDeletePublicationType(system.id);
        }
        setSystems((prev) => {
            const remaining = prev.filter((s) => !isSameSystem(s, system));
            const next = remaining[0] ?? null;
            setSelectedId(next?.id ?? null);
            setEditedSystem(next ? { ...next } : null);
            return remaining;
        });
    };

    return (
        <Dialog modalType="modal" open={open} onOpenChange={(_, data) => { if (!data.open) { onClose(); } }}>
            <DialogSurface className={classes.surface}>
                <DialogBody className={classes.body}>
                    <DialogTitle>{formatMessage(messages.title)}</DialogTitle>
                    <DialogContent className={classes.content}>
                        <div className={classes.left}>
                            <div className={classes.list}>
                                {systems.map((system, index) => {
                                    const isSelected = system.id !== undefined ? system.id === selectedId : editedSystem === system;
                                    return (
                                        <div
                                            key={system.id ?? `new-${index}`}
                                            className={`${classes.listItem} ${isSelected ? classes.listItemSelected : ""}`}
                                            onClick={() => handleSelect(system)}
                                        >
                                            {!(system.active ?? true) && <EyeOffRegular className={classes.inactiveIcon} />}
                                            <span className={`${classes.listItemName} ${!(system.active ?? true) ? classes.listItemInactive : ""}`}>{system.name}</span>
                                        </div>
                                    );
                                })}
                            </div>
                            <Button
                                className={classes.addButton}
                                icon={<AddRegular />}
                                onClick={handleAdd}
                                appearance="subtle"
                            >
                                {formatMessage(messages.btnAdd)}
                            </Button>
                        </div>
                        <div className={classes.right}>
                            {editedSystem
                                ? <PublicationSystemDetail
                                    value={editedSystem}
                                    onSave={handleSave}
                                    onToggleActive={() => handleToggleActive(editedSystem)}
                                    onRemove={() => handleRemove(editedSystem)}
                                />
                                : <div className={classes.empty}>{formatMessage(messages.btnAdd)}</div>
                            }
                        </div>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={onClose}>{formatMessage(messages.btnClose)}</Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}
