package simulator.memory;

import simulator.process.Process;
import java.util.LinkedList;
import java.util.Queue;

public class Disc {
    private final Queue<Process> discQueue;

    public Disc() {
        this.discQueue = new LinkedList<>();
    }

    public void addProcess(Process p) {
        discQueue.add(p);
    }

    public Process removeProcess() {
        return discQueue.poll();
    }

    public boolean isEmpty() {
        return discQueue.isEmpty();
    }

    public int getSize() {
        return discQueue.size();
    }
}

