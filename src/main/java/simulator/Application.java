package simulator;

import simulator.memory.FifoMemoryManager;
import simulator.memory.MemoryManager;
import simulator.process.MemoryProcess;
import simulator.process.SwapResult;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Application {


    private static final long INITIAL_AUTO_SWAP_INTERVAL_MS = 4000;
    private static final long MIN_AUTO_SWAP_INTERVAL_MS = 500;
    private static final long AUTO_SWAP_DECREMENT_STEP_MS = 1000;
    private static final long IDLE_CHECK_INTERVAL_MS = 3000;

    private MemoryManager memoryManager;

    private final Scanner scanner;

    private int trashingAlertThreshold;

    private int nextProcessIdCounter = 1;

    private final ScheduledExecutorService autoSwapScheduler;

    private AutoSwapperTask autoSwapperTask;

    public Application() {
        this.scanner = new Scanner(System.in);
        this.autoSwapScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startSimulation() {
        System.out.println("--- Simulador de Trashing de Sistema ---");

        System.out.print("Defina a capacidade da memória principal (ex: 3): ");
        int capacity = Integer.parseInt(scanner.nextLine());
        this.memoryManager = new FifoMemoryManager(capacity);

        System.out.print("Defina o limiar de swaps para alerta de trashing (ex: 2): ");
        this.trashingAlertThreshold = Integer.parseInt(scanner.nextLine());
        if (this.trashingAlertThreshold <= 0) this.trashingAlertThreshold = 1;

        this.autoSwapperTask = new AutoSwapperTask(this, memoryManager, autoSwapScheduler, trashingAlertThreshold);
        autoSwapScheduler.schedule(autoSwapperTask, calculateAutoSwapDelay(), TimeUnit.MILLISECONDS);

        System.out.println("\nSimulação iniciada. O auto-swapper está ativo.");
        System.out.println("Comandos: 'add', 'status', 'sair'.");
        System.out.print("Comando > ");

        boolean running = true;
        while (running) {
            final var input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "add":
                    MemoryProcess newProcess = new MemoryProcess("P" + nextProcessIdCounter++);
                    System.out.println("\n[CLI] Adicionando processo: " + newProcess.getId());

                    MemoryProcess swappedOut = memoryManager.addProcess(newProcess);

                    if (swappedOut != null) {
                        System.out.println("\n[SWAP EVENT] Processo " + swappedOut.getId() +
                                " removido (SWAP-OUT) para dar espaço ao " + newProcess.getId() + " (SWAP-IN).");
                        if (memoryManager.getSwapCount() >= trashingAlertThreshold) {
                            System.out.println("[TRASHING ALERT] Nível crítico de trashing! Swaps: " +
                                    memoryManager.getSwapCount() + ". Sistema lento.");
                        }
                    } else {
                        System.out.println("\n[MEM] Processo " + newProcess.getId() + " adicionado à RAM.");
                    }
                    displaySystemState();
                    autoSwapperTask.rescheduleNow();
                    break;
                case "status":
                    displaySystemState();
                    break;
                case "sair":
                    running = false;
                    break;
                default:
                    System.out.println("Comando inválido. Use 'add', 'status' ou 'sair'.");
                    break;
            }
            if (running) {
                System.out.print("Comando > ");
            }
        }
        shutdown();
    }

    public void displaySystemState() {
        if (memoryManager == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("\n[SYSTEM]\n");
        List<MemoryProcess> processesInRam = memoryManager.getProcessesInRam();
        String ramContents = processesInRam.stream()
                .map(MemoryProcess::getId)
                .collect(Collectors.joining(", ", "[", "]"));
        sb.append("Memória Principal (").append(memoryManager.getCurrentSize()).append("/")
                .append(memoryManager.getCapacity()).append("): ").append(ramContents).append("\n");
        sb.append("Contagem Total de Swaps: ").append(memoryManager.getSwapCount()).append("\n");

        if (memoryManager.getSwapCount() >= trashingAlertThreshold && memoryManager.getCurrentSize() == memoryManager.getCapacity()) {
            List<MemoryProcess> contendingProcesses = memoryManager.getAllProcessesEverInSystem();
            if (contendingProcesses.size() > memoryManager.getCapacity()) {
                String contendingProcsStr = contendingProcesses.stream()
                        .map(MemoryProcess::getId)
                        .collect(Collectors.joining(", "));
                sb.append("Processos: ").append(contendingProcsStr).append("\n");
            }
        }
        System.out.print(sb);
    }

    public long calculateAutoSwapDelay() {
        int numAllProcesses = memoryManager.getAllProcessesEverInSystem().size();
        int ramCapacity = memoryManager.getCapacity();

        if (memoryManager.getCurrentSize() < ramCapacity || numAllProcesses <= ramCapacity) {
            return IDLE_CHECK_INTERVAL_MS;
        }

        int degreeOfContention = Math.max(0, numAllProcesses - ramCapacity);
        long delay = INITIAL_AUTO_SWAP_INTERVAL_MS - (degreeOfContention * AUTO_SWAP_DECREMENT_STEP_MS);
        return Math.max(MIN_AUTO_SWAP_INTERVAL_MS, delay);
    }

    private void shutdown() {
        System.out.println("\nEncerrando o simulador...");
        if (autoSwapperTask != null) {
            autoSwapperTask.stop();
        }
        autoSwapScheduler.shutdown();
        try {
            if (!autoSwapScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                autoSwapScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            autoSwapScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        scanner.close();
        System.out.println("Simulador encerrado.");
    }

    static class AutoSwapperTask implements Runnable {

        private final Application app;

        private final MemoryManager memoryManager;

        private final ScheduledExecutorService scheduler;

        private final int trashingAlertThreshold;

        private volatile boolean running = true;

        public AutoSwapperTask(
                Application app,
                MemoryManager memoryManager,
                ScheduledExecutorService scheduler,
                int trashingAlertThreshold
        ) {
            this.app = app;
            this.memoryManager = memoryManager;
            this.scheduler = scheduler;
            this.trashingAlertThreshold = trashingAlertThreshold;
        }

        @Override
        public void run() {
            if (!running || scheduler.isShutdown()) {
                return;
            }

            SwapResult result = memoryManager.performAutoSwap();
            if (result != null) {
                System.out.println("\n\n[AUTO-SWAP EVENT] Swapped OUT: " + result.swappedOut.getId() +
                        ", Swapped IN: " + result.swappedIn.getId() + ". Sistema em thrashing.");
                if (memoryManager.getSwapCount() >= trashingAlertThreshold) {
                    System.out.println("[TRASHING ALERT] Nível crítico de trashing! Swaps: " + memoryManager.getSwapCount());
                }
                app.displaySystemState();
                System.out.print("Comando > ");
            }

            if (running && !scheduler.isShutdown()) {
                scheduler.schedule(this, app.calculateAutoSwapDelay(), TimeUnit.MILLISECONDS);
            }
        }

        public void rescheduleNow() {
            if (running && !scheduler.isShutdown()) {
                scheduler.schedule(this, app.calculateAutoSwapDelay(), TimeUnit.MILLISECONDS);
            }
        }

        public void stop() {
            this.running = false;
        }
    }

}