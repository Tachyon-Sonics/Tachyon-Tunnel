package ch.tachyon.tunnel.engine.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;

public class AwtTools {

    private AwtTools() {

    }

    public static Window getWindow(Component cmp) {
        Component parent = cmp.getParent();
        while (parent != null && !(parent instanceof Window)) {
            parent = parent.getParent();
        }
        return (Window) parent;
    }

    private static Action wrapShortcutKeyAction(final Action target) {
        return new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                Object source = e.getSource();
                if (source instanceof Component) {
                    // Find the focused component
                    Component cmp = (Component) source;
                    Window window = getWindow(cmp);
                    Component focused = window.getFocusOwner();
                    if (focused instanceof JTextComponent || focused instanceof TextComponent)
                        return; // Disable shortcut keys when in a text component
                }
                target.actionPerformed(e);
            }
        };
    }

    public static void addKeyboardShortcut(AbstractButton button, KeyStroke keyStroke) {
        addKeyboardShortcut(button, keyStroke, true);
    }
    
    /**
     * Add a keybord shortcut that is global to when the window has focus
     * @param button the button for which the shortcut is
     * @param keyStroke the keyboard shortcut
     * @param excludeTextInputComponent whether to disable the shortcut when the focus is on a text input. This should be used for most non-function keys
     * to make sure typing in a textfield does not trigger the button. It can be set to false for special keys such as escape and function keys.
     */
    public static void addKeyboardShortcut(AbstractButton button, KeyStroke keyStroke, boolean excludeTextInputComponent) {
        if (button.getAction() == null)
            throw new IllegalArgumentException("The button must have an action");
        Object actionKey = new Object();
        button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionKey);
        Action action = button.getAction();
        if (excludeTextInputComponent)
            action = wrapShortcutKeyAction(action);
        button.getActionMap().put(actionKey, action);
    }

    public static void addKeyboardShortcut(AbstractButton button, int keyCode) {
        addKeyboardShortcut(button, KeyStroke.getKeyStroke(keyCode, 0));
    }

    /**
     * Add a keyboard shortcut for the given action. The action will be executed if the three following
     * conditions hold:
     * <ul>
     * <li>the given key is pressed
     * <li>the window containing the given component currently has the focus
     * <li>the focused component is <i>not</i> a text component ({@link TextComponent}, {@link JTextComponent}
     * or subclass)
     * </ul>
     */
    public static void addKeyboardShortcut(JComponent button, KeyStroke keyStroke, Action action) {
        Object actionKey = new Object();
        button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionKey);
        button.getActionMap().put(actionKey, wrapShortcutKeyAction(action));
    }

    public static void addKeyboardShortcut(JComponent button, int when, KeyStroke keyStroke, Action action) {
        Object actionKey = new Object();
        button.getInputMap(when).put(keyStroke, actionKey);
        button.getActionMap().put(actionKey, action);
    }

}
