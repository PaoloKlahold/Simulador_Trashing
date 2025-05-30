package simulator.process;

import java.util.Objects;

public class MemoryProcess {
    private final String id;

    public MemoryProcess(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryProcess memoryProcess = (MemoryProcess) o;
        return Objects.equals(id, memoryProcess.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}