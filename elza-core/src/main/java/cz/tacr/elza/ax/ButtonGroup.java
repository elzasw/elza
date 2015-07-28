package cz.tacr.elza.ax;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.ui.Button;

import cz.tacr.elza.ax.*;


/**
 * Skupina tlačítek z nichž právě jedno je vždy nastaveno jako primární.
 *
 * @param <T> typ hodnoty, dle které se přepíná primární tlačítko
 * @author <a href="mailto:jan.pikl@marbes.cz">Jan Pikl</a>
 *         Date: 14.4.2015
 */
public class ButtonGroup<T> implements Iterable<Button> {

    private List<Button> buttons = new ArrayList<>();
    private Switch<T, Button> vswitch = new Switch<>();
    private Consumer<T> notifier;
    private T value;

    public ButtonGroup() {
        this.vswitch.action(this::setPrimaryButton);
    }

    public <CS extends Consumer<T> & Supplier<T>> ButtonGroup<T> bindTo(CS target) {
        notifier = null;
        setValue(target.get());
        notifier = target;
        return this;
    }

    public ButtonGroup<T> button(cz.tacr.elza.ax.AxAction<T> action) {
        if (action.getAction() != null) {
            action.action(action.getAction().andThen(this::setValue));
        } else {
            action.action(this::setValue);
        }
        Button button = action.button();
        buttons.add(button);
        if (Objects.equals(action.getValue(), value)) {
            setPrimaryButton(button);
        }
        vswitch.on(action.getValue(), button);
        return this;
    }

    public ButtonGroup<T> setValue(T value) {
        this.value = value;
        vswitch.set(value);
        if (notifier != null) {
            notifier.accept(value);
        }
        return this;
    }

    private void setPrimaryButton(Button primaryButton) {
        for (Button button: buttons) {
            button.removeStyleName("primary");
        }
        if (primaryButton != null) {
            primaryButton.addStyleName("primary");
        }
    }

    @Override
    public Iterator<Button> iterator() {
        return buttons.iterator();
    }

}
