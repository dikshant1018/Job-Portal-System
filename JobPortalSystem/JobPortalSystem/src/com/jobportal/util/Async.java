package com.jobportal.util;

import com.jobportal.ui.UIStyle;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Runs database work off the Swing thread so the window never freezes. */
public final class Async {
    private Async() {}

    public static <T> void run(Component owner, Supplier<T> work, Consumer<T> onDone) {
        run(owner, work, onDone, null);
    }

    /** onFail (optional) runs after the error message was shown, e.g. to re-enable a button. */
    public static <T> void run(Component owner, Supplier<T> work, Consumer<T> onDone, Runnable onFail) {
        if (owner != null) owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() {
                return work.get();
            }

            @Override protected void done() {
                if (owner != null) owner.setCursor(Cursor.getDefaultCursor());
                try {
                    onDone.accept(get());
                } catch (Exception e) {
                    Throwable c = e.getCause() != null ? e.getCause() : e;
                    c.printStackTrace();
                    UIStyle.error(owner, c.getMessage() != null ? c.getMessage() : "Something went wrong.");
                    if (onFail != null) onFail.run();
                }
            }
        }.execute();
    }

    public static void exec(Component owner, Runnable work, Runnable onDone) {
        exec(owner, work, onDone, null);
    }

    public static void exec(Component owner, Runnable work, Runnable onDone, Runnable onFail) {
        run(owner, () -> { work.run(); return Boolean.TRUE; }, ok -> onDone.run(), onFail);
    }
}
