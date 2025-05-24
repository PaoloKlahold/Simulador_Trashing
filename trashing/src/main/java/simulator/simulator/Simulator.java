package simulator.simulator;

import simulator.memory.RAM;
import simulator.memory.Disc;
import simulator.process.Process;
import java.util.List;

public class Simulator {
    private final RAM ram;
    private final Disc disc;
    private int trashingLevel;
    private final int trashingThreshold;

    public Simulator(RAM ram, Disc disc, int trashingThreshold) {
        this.ram = ram;
        this.disc = disc;
        this.trashingLevel = 0;
        this.trashingThreshold = trashingThreshold;
    }

    public void addProcess(Process p) {
        if (!ram.addProcess(p)) {
            // RAM cheia, faz swap-out do mais antigo
            Process swapped = ram.swapOut();
            if (swapped != null) {
                disc.addProcess(swapped);
            }
            ram.swapIn(p);
            trashingLevel++;
        }
    }

    public void simulateSwapping() {
        // Simula swapping contínuo se houver processos no disco
        if (!disc.isEmpty()) {
            Process out = ram.swapOut();
            if (out != null) {
                disc.addProcess(out);
            }
            Process in = disc.removeProcess();
            if (in != null) {
                ram.swapIn(in);
                trashingLevel++;
            }
        }
    }

    public void printStatus() {
        List<Process> mem = ram.getProcesses();
        System.out.print("Memória: [");
        for (int i = 0; i < mem.size(); i++) {
            System.out.print("P" + mem.get(i).get());
            if (i < mem.size() - 1) System.out.print(", ");
        }
        System.out.print("]  ");
        System.out.print("Swap: " + ram.getSwapCount());
        if (trashingLevel >= trashingThreshold) {
            System.out.print("  ALERTA: Nível crítico de trashing detectado!");
        }
        System.out.println();
    }

    public int getTrashingLevel() {
        return trashingLevel;
    }
}

