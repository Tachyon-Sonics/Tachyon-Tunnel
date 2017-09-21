package ch.tachyon.tunnel.engine.gui;

import java.awt.*;

import javax.swing.*;

import ch.tachyon.tunnel.host.*;
import ch.tachyon.tunnel.host.effect.*;

public class EffectControlPanel extends JPanel {

    private final double[] WEIGHT_X = new double[] {0.1, 0.7, 0.0};
    private final ParameterGuiBuilder pgBuilder = new ParameterGuiBuilder();
    private final IEffect effect;


    public EffectControlPanel(IEffect effect, IProcessingInfo info) {
        this.effect = effect;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        setLayout(new TableLayout(3, gbc));
        for (IParameter parameter : effect.getParameters()) {
            JComponent[] cmps = pgBuilder.buildGuiFor(parameter, info);
            for (int i = 0; i < cmps.length; i++) {
                gbc = (GridBagConstraints) gbc.clone();
                gbc.weightx = WEIGHT_X[i % 3];
                add(cmps[i], gbc);
            }
        }
    }

    public IEffect getEffect() {
        return this.effect;
    }

}
