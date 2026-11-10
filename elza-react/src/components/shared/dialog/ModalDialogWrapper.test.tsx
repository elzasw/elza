import { Button, Dialog, DialogBody, DialogContent, DialogSurface, DialogTitle } from '@fluentui/react-components';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import '../../../stores/AppStore';
import { ModalDialogWrapper } from './ModalDialogWrapper';

/** Legacy modal content opening a Fluent dialog, like the entity picker opening "Nová archivní entita". */
const LegacyContent = () => {
    const [open, setOpen] = useState(false);
    return (
        <>
            <Button onClick={() => setOpen(true)}>Nová entita</Button>
            <Dialog open={open} modalType="modal" onOpenChange={(_event, data) => setOpen(data.open)}>
                <DialogSurface>
                    <DialogBody>
                        <DialogTitle>Nová archivní entita</DialogTitle>
                        <DialogContent>
                            <input aria-label="Hlavní část jména" />
                        </DialogContent>
                    </DialogBody>
                </DialogSurface>
            </Dialog>
        </>
    );
};

describe('ModalDialogWrapper', () => {
    it('lets a Fluent dialog opened from its content keep focus in a text field', async () => {
        const user = userEvent.setup();
        render(
            <ModalDialogWrapper className="dialog-fullscreen" title={null}>
                <LegacyContent />
            </ModalDialogWrapper>,
        );

        await user.click(await screen.findByRole('button', { name: 'Nová entita' }));
        const name = await screen.findByLabelText('Hlavní část jména');
        await user.click(name);
        await user.keyboard('Novák');
        // the bootstrap focus trap reacts to focus events in a setTimeout
        await new Promise((resolve) => setTimeout(resolve, 50));

        expect(document.activeElement).toBe(name);
        expect((name as HTMLInputElement).value).toBe('Novák');
    });
});
