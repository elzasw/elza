package cz.tacr.elza.ui.window;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.springframework.util.Assert;

import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Upload;

import cz.req.ax.AxAction;
import cz.req.ax.AxWindow;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.ui.utils.ElzaNotifications;


/**
 * Zobrazení okna pro import balíčků s configurací.
 *
 * @author Martin Šlapa
 * @since 16.12.2015
 */
public class PackagesWindow extends AxWindow implements Upload.SucceededListener,
        Upload.FailedListener, Upload.Receiver{

    private CssLayout layout = new CssLayout();

    private final RuleManager ruleManager;

    private File file;

    public PackagesWindow(final RuleManager ruleManager) {
        Assert.notNull(ruleManager);
        this.ruleManager = ruleManager;
    }

    /**
     * Zobrazení okna pro spravu package.
     *
     */
    public AxWindow show() {


        reload();

        CssLayout uploadLayout = new CssLayout();

        Upload upload = new Upload("Upload it here", this);

        upload.addSucceededListener(this);
        upload.addFailedListener(this);

        uploadLayout.addComponent(upload);
        uploadLayout.setSizeUndefined();
        uploadLayout.setStyleName("package-upload");

        caption("Sprava packages").components(layout, uploadLayout)
                .buttonClose().modal().style("level-window-detail");

        return super.show();
    }

    private void reload() {

        layout.removeAllComponents();

        List<RulPackage> packages = ruleManager.getPackages();

        layout.addStyleName("level-box");
        layout.setSizeUndefined();

        AxAction deletePackage;
        AxAction downloadPackage;

        for (RulPackage rulPackage : packages) {

            CssLayout bulkActionLayout = new CssLayout();
            bulkActionLayout.addStyleName("level-line");

            deletePackage = new AxAction().run(() -> {
                try {
                    ruleManager.deletePackage(rulPackage.getCode());
                    ElzaNotifications.show("Smazano...", 2000);
                    reload();
                } catch (Exception e) {
                    e.printStackTrace();
                    ElzaNotifications.showError(e.getMessage());
                }
            }).caption("Smazat").icon(FontAwesome.TRASH_O);

            bulkActionLayout.addComponent(deletePackage.button());

            downloadPackage = new AxAction().caption("Stáhnout").icon(FontAwesome.SAVE);

            downloadPackage.run(() -> {
                try {
                    File file = ruleManager.exportPackage(rulPackage.getCode());
                    FileResource res = new FileResource(file);
                    Page.getCurrent().open(res, null, false);
                    ElzaNotifications.show("Staženo...", 2000);
                } catch (Exception e) {
                    e.printStackTrace();
                    ElzaNotifications.showError(e.getMessage());
                }
            });

            downloadPackage.style("level-name");
            bulkActionLayout.addComponent(downloadPackage.button());

            Label label = newLabel(rulPackage.getCode());
            label.addStyleName("level-code");
            bulkActionLayout.addComponent(label);

            label = newLabel("v" + rulPackage.getVersion().toString());
            label.addStyleName("level-version");
            bulkActionLayout.addComponent(label);

            label = newLabel(rulPackage.getName());
            label.addStyleName("level-name");
            bulkActionLayout.addComponent(label);

            layout.addComponent(bulkActionLayout);
        }


    }

    @Override
    public void uploadFailed(final Upload.FailedEvent event) {
        event.getReason().printStackTrace();
    }

    @Override
    public void uploadSucceeded(final Upload.SucceededEvent event) {
        event.getUpload();
        if (file.length() > 0) {
            try {
                ruleManager.importPackage(file);
                ElzaNotifications.show("Přidáno...", 2000);
                reload();
            } catch (Exception e) {
                e.printStackTrace();
                ElzaNotifications.showError(e.getMessage());
            }
        }
    }

    @Override
    public OutputStream receiveUpload(final String filename, final String mimeType) {
        FileOutputStream fos;

        try {
            file = File.createTempFile("upload", filename);
            file.deleteOnExit();
            fos = new FileOutputStream(file);
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
        return fos;
    }
}
