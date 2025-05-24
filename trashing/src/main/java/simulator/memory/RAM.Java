package simulator.memory;

import simulator.process.Process;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;

public class RAM {
    private final int capacity;
    private final Queue<Process> memory;
    private int swapCount;

    public RAM(int capacity) {
        this.capacity = capacity;
        this.memory = new LinkedList<>();
        this.swapCount = 0;
    }

    public boolean isFull() {
        return memory.size() >= capacity;
    }

    public boolean addProcess(Process p) {
        if (!isFull()) {
            memory.add(p);
            return true;
        }
        return false;
    }

    public Process swapOut() {
        Process removed = memory.poll();
        if (removed != null) swapCount++;
        return removed;
    }

    public void swapIn(Process p) {
        memory.add(p);
        swapCount++;
    }

    public List<Process> getProcesses() {
        return new ArrayList<>(memory);
    }

    public int getSwapCount() {
        return swapCount;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getUsed() {
        return memory.size();
    }
}

