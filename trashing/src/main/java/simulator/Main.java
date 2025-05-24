package simulator;

import simulator.memory.RAM;
import simulator.memory.Disc;
import simulator.simulator.Simulator;
import simulator.process.Process;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        final int RAM_CAPACITY = 3; // Capacidade da memória principal
        final int TRASHING_THRESHOLD = 10; // Nível para alerta de trashing severo
        RAM ram = new RAM(RAM_CAPACITY);
        Disc disc = new Disc();
        Simulator simulator = new Simulator(ram, disc, TRASHING_THRESHOLD);
        Scanner scanner = new Scanner(System.in);
        int processId = 1;

        // Inicializa com alguns processos
        for (int i = 0; i < RAM_CAPACITY; i++) {
            simulator.addProcess(new Process(processId++));
        }

        System.out.println("Simulador de Trashing iniciado!");
        System.out.println("Digite 'add' para adicionar novo processo ou 'exit' para sair.");

        while (true) {
            // Exibe status
            simulator.printStatus();
            // Simula swapping contínuo
            simulator.simulateSwapping();
            // Entrada do usuário sem bloquear o loop
            try {
                if (System.in.available() > 0) {
                    String input = scanner.nextLine();
                    if (input.equalsIgnoreCase("add")) {
                        simulator.addProcess(new Process(processId++));
                        System.out.println("Processo P" + (processId-1) + " adicionado.");
                    } else if (input.equalsIgnoreCase("exit")) {
                        System.out.println("Encerrando simulador.");
                        break;
                    }
                }
            } catch (Exception e) {
                // Ignora exceções de input
            }
            // Aguarda um pouco para simular tempo real
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
        }
        scanner.close();
    }
}

