package cz.tacr.elza.ax;

import com.google.common.base.Joiner;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cz.tacr.elza.ax.AxConfirm;
import cz.tacr.elza.ax.AxUI;


public class AxAction<T> implements Cloneable {

    Logger logger = LoggerFactory.getLogger(getClass());

    private T value;
    private String caption, description;
    private List<String> styles = new ArrayList<>();
    private Boolean enabled = Boolean.TRUE;
    private Boolean right = Boolean.FALSE;
    private Supplier<String> confirm;
    private Resource icon;
    private Runnable run, runBefore, runAfter;
    private Consumer<T> action;
    private Consumer<ActionException> exception;
    private Supplier<T> variable;
    private List<AxAction> submenu;

    public static <T> AxAction<T> of(T value) {
        return new AxAction<T>().value(value);
    }

    //TODO DescribedFunctionInterface
    public AxAction<T> caption(String caption) {
        this.caption = caption;
        return this;
    }

    public AxAction<T> description(String description) {
        this.description = description;
        return this;
    }

    public AxAction<T> confirm(Supplier<String> confirm) {
        this.confirm = confirm;
        return this;
    }

    public AxAction<T> confirm(String message) {
        return confirm(() -> message);
    }

    public AxAction<T> styles(List<String> styles) {
        this.styles = styles;
        return this;
    }

    public AxAction<T> danger() {
        return style("danger");
    }

    public AxAction<T> primary() {
        return style("primary");
    }

    public AxAction<T> friendly() {
        return style("friendly");
    }

    public AxAction<T> empty() {
        return style("empty");
    }

    public AxAction<T> link() {
        return style("link");
    }

    public AxAction<T> style(String style) {
        styles.add(style);
        return this;
    }

    public AxAction<T> right() {
        this.right = Boolean.TRUE;
        return this;
    }

    public AxAction<T> enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AxAction<T> enabled() {
        return enabled(true);
    }

    public AxAction<T> disabled() {
        return enabled(false);
    }

    public AxAction<T> icon(String themeResource) {
        this.icon= new ThemeResource(themeResource);
        return this;
    }
    //TODO DescribedFunctionInterface
    public AxAction<T> icon(Resource icon) {
        this.icon = icon;
        return this;
    }

    public AxAction<T> value(Supplier<T> call) {
        variable = call;
        return this;
    }

    public AxAction<T> value(T value) {
        this.value = value;
        return this;
    }

    public AxAction<T> action(Consumer<T> action) {
        this.action = action;
        return this;
    }

    public AxAction<T> exception(Consumer<ActionException> exception) {
        this.exception = exception;
        return this;
    }

    public AxAction<T> run(Runnable run) {
        this.run = run;
        return this;
    }

    public AxAction<T> runBefore(Runnable run) {
        runBefore = run;
        return this;
    }

    public AxAction<T> runAfter(Runnable run) {
        runAfter = run;
        return this;
    }

    public AxAction<T> submenu(AxAction... actions) {
        if (submenu == null) submenu = new ArrayList<>();
        Collections.addAll(submenu, actions);
        return this;
    }

    public int submenuSize() {
        return submenu != null ? submenu.size() : 0;
    }

    protected void onAction() {
        try {
            doExecute(Phase.RunBefore, runBefore);
            String message = getConfirm();
            if (message == null) {
                doActionAndAfter();
            } else {
                new AxConfirm(message, this::doActionAndAfter).show();
            }
        } catch (ActionException ae) {
            if (exception == null) {
                logger.error("Missing exception handler");
                navigate(ae);
            } else {
                exception.accept(ae);
            }
        } catch (Throwable th) {
            navigate(th);
        }
    }

    public void navigate(Throwable th) {
        AxUI ui = (AxUI) UI.getCurrent();
        ui.navigate(th);
    }

    private void doActionAndAfter() throws ActionException {
        doExecute(Phase.Run, run);
        doExecute(Phase.Value, action);
        //TODO Action validate predicate?
        doExecute(Phase.RunAfter, runAfter);
    }

    private void doExecute(Phase phase, Object exec) throws ActionException {
        T tran = null;
        try {
            if (exec == null) return;
            if (exec instanceof Runnable) ((Runnable) exec).run();
            if (exec instanceof Consumer) {
                tran = getValue();
                phase = Phase.Action;
                ((Consumer<T>) exec).accept(tran);
            }
            if (exec instanceof AxAction) ((AxAction) exec).onAction();

        } catch (ActionException ae) {
            ae.setPhase(phase);
            throw ae;
        } catch (Exception ex) {
            throw new ActionException(phase, ex);
        } catch (Throwable th) {
            throw th;
        }
    }

    public T getValue() {
        return (variable == null) ? value : variable.get();
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String getCaption() {
        return caption;
    }

    public String getDescription() {
        return description;
    }

    public Resource getIcon() {
        return icon;
    }

    public String getStyle() {
        String style = styles.isEmpty() ? null : styles.iterator().next();
        return Joiner.on("-").skipNulls().join(style, right ? "right" : null);
    }

    public List<String> getStyles() {
//        return styles.stream().map(style -> right ? style + "-right" : style).collect(Collectors.toList());
        return styles;
    }

    public Runnable getRun() {
        return run;
    }

    public Runnable getRunBefore() {
        return runBefore;
    }

    public Runnable getRunAfter() {
        return runAfter;
    }

    public Consumer<T> getAction() {
        return action;
    }

    public String getConfirm() {
        return confirm != null ? confirm.get() : null;
    }

    public Button button() {
        //TODO New factory AxButton
        //TODO Caption/Icon by factory method
        //TODO Review lazy??
        Button button = new Button(caption, icon);
        if (description != null) button.setDescription(description);
        if (!styles.isEmpty()) styles.forEach(button::addStyleName);
        if (right) button.addStyleName("right");
        button.addClickListener(event -> onAction());
        button.setEnabled(enabled);
        return button;
    }

    public MenuBar menuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.addStyleName("actions");
        styles.forEach(menuBar::addStyleName);
        menuItem(menuBar);
        return menuBar;
    }

    public MenuBar.MenuItem menuItem(MenuBar menuBar) {
        if (caption == null) caption = "";
        MenuBar.MenuItem menuItem = menuBar.addItem(caption, null);
        if (icon != null) menuItem.setIcon(icon);
        if (submenu == null) {
            menuItem.setCommand((item) -> onAction());
        } else {
            for (AxAction action : submenu) {
                if (action == null) {
                    //SPACER??
                } else {
                    action.menuItem(menuItem);
                }
            }
        }
        if (enabled != null) {
            menuItem.setEnabled(enabled);
        }
        return menuItem;
    }

    public MenuBar.MenuItem menuItem(MenuBar.MenuItem parentMenuItem) {
        MenuBar.MenuItem item;
        if (icon != null) {
            item = parentMenuItem.addItem(caption, icon, (i) -> onAction());
        } else {
            item = parentMenuItem.addItem(caption, (i) -> onAction());
        }
        if (enabled != null) {
            item.setEnabled(enabled);
        }
        return item;
    }

    public ShortcutListener shortcutListener(int keycode) {
        return new ShortcutListener(null, keycode, null) {
            @Override
            public void handleAction(Object sender, Object target) {
                onAction();
            }
        };
    }

    public ShortcutListener shortcutListenerEnter() {
        return shortcutListener(ShortcutAction.KeyCode.ENTER);
    }


    //TODO Checked exception
    public static class ActionException extends RuntimeException {

        //        Object value;
        Phase phase;

        public ActionException(String message, Throwable cause) {
            super(message, cause);
        }

        public ActionException(Phase phase, Throwable cause) {
            super("Chyba akce ve fazi " + phase, cause);
            this.phase = phase;
//            this.value = value;
        }

        public void setPhase(Phase phase) {
            this.phase = phase;
        }

        public Phase getPhase() {
            return phase;
        }

        /*public Object getValue() {
            return value;
        }*/
    }

    public enum Phase {
        RunBefore, Run, Value, Action, RunAfter
    }

    @Override
    public AxAction<T> clone() {
        return new AxAction<T>()
                .caption(caption)
                .description(description)
                .icon(icon)
                .styles(styles)
                .run(run)
                .runBefore(runBefore)
                .runAfter(runAfter)
                .action(action)
                .confirm(confirm);
    }

}
