package ch.tachyon.tritonus.test.util;

import ch.tachyon.tunnel.audio.utils.*;


public class ConsoleProgressMonitor implements IProgressMonitor {

    private final int LENGTH = 75; // + 4 for "< " and " >"
    private int lastPercent = 0;


    public ConsoleProgressMonitor() {
        System.out.print("< 0% ");
        for (int i = 0; i < LENGTH - 3 - 5; i++)
            System.out.print("-");
        System.out.println(" 100% >");
        System.out.print("< ");
    }

    @Override
    public void setProgress(double current, double total) throws OperationAbortedException {
        int percent = (int) (current * LENGTH / total + 0.5);
        while (lastPercent < percent) {
            System.out.print("=");
            lastPercent++;
            if (lastPercent == LENGTH)
                System.out.println(" >");
        }
    }

}
