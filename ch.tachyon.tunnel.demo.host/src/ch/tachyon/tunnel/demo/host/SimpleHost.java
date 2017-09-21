package ch.tachyon.tunnel.demo.host;

import ch.tachyon.tunnel.demo.host.gui.*;
import ch.tachyon.tunnel.demo.host.gui.utils.*;

public class SimpleHost {
    
    public static void main(String[] args) {
        AwtTools.initLookAndFeel(SimpleHost.class.getSimpleName());
        FormData controller = new FormData();
        SimpleHostFrame frame = new SimpleHostFrame(controller);
        AwtTools.centerWindowOnScreen(frame);
        frame.setVisible(true);
        FileChooserBooster.start();
    }

}
