package simulator.memory;

import simulator.process.MemoryProcess;
import simulator.process.SwapResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FifoMemoryManager implements MemoryManager {

    private static final long AUTO_SWAP_OVERHEAD_MS = 300;
    private static final long USER_SWAP_OVERHEAD_MS = 50;

    private final Queue<MemoryProcess> ram;

    private final int capacity;

    private int swapCount;

    private final List<MemoryProcess> allProcessesEverInSystem;

    private int nextDiskProcessToSwapInIndex = 0;

    public FifoMemoryManager(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Memory capacity must be positive.");
        }
        this.capacity = capacity;
        this.ram = new LinkedList<>();
        this.swapCount = 0;
        this.allProcessesEverInSystem = new ArrayList<>();
    }

    @Override
    public synchronized MemoryProcess addProcess(MemoryProcess newProcess) {
        boolean newToSystem = !allProcessesEverInSystem.contains(newProcess);
        if (newToSystem) {
            allProcessesEverInSystem.add(newProcess);
        }

        MemoryProcess swappedOutProcess = null;
        if (ram.size() >= capacity) {
            swappedOutProcess = ram.poll();
            if (swappedOutProcess != null) {
                swapCount++;
                try {
                    Thread.sleep(USER_SWAP_OVERHEAD_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        ram.offer(newProcess);
        return swappedOutProcess;
    }

    @Override
    public synchronized SwapResult performAutoSwap() {
        if (ram.size() < capacity || allProcessesEverInSystem.size() <= ram.size()) {
            return null;
        }

        List<MemoryProcess> currentRamProcesses = new ArrayList<>(ram);
        List<MemoryProcess> onDiskCandidates = new ArrayList<>();
        for (MemoryProcess p : allProcessesEverInSystem) {
            boolean isInRam = false;
            for (MemoryProcess ramP : currentRamProcesses) {
                if (ramP.id().equals(p.id())) {
                    isInRam = true;
                    break;
                }
            }
            if (!isInRam) {
                onDiskCandidates.add(p);
            }
        }

        if (onDiskCandidates.isEmpty()) {
            return null;
        }

        MemoryProcess processToSwapOut = ram.poll();
        if (processToSwapOut == null) {
            return null;
        }

        if (nextDiskProcessToSwapInIndex >= onDiskCandidates.size()) {
            nextDiskProcessToSwapInIndex = 0;
        }
        MemoryProcess processToSwapIn = onDiskCandidates.get(nextDiskProcessToSwapInIndex);
        nextDiskProcessToSwapInIndex++;

        ram.offer(processToSwapIn);
        swapCount++;

        try {
            Thread.sleep(AUTO_SWAP_OVERHEAD_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new SwapResult(processToSwapOut, processToSwapIn);
    }


    @Override
    public synchronized List<MemoryProcess> getProcessesInRam() {
        return new ArrayList<>(ram);
    }

    @Override
    public synchronized int getSwapCount() {
        return swapCount;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public synchronized int getCurrentSize() {
        return ram.size();
    }

    @Override
    public synchronized List<MemoryProcess> getAllProcessesEverInSystem() {
        return List.copyOf(allProcessesEverInSystem);
    }
}