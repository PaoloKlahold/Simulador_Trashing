package simulator.memory;


import simulator.process.MemoryProcess;
import simulator.process.SwapResult;

import java.util.List;

public interface MemoryManager {

    MemoryProcess addProcess(MemoryProcess process);

    List<MemoryProcess> getProcessesInRam();

    int getSwapCount();

    int getCapacity();

    int getCurrentSize();

    List<MemoryProcess> getAllProcessesEverInSystem();

    SwapResult performAutoSwap();
}