package simulator.process;

public class SwapResult {
    public final MemoryProcess swappedOut;
    public final MemoryProcess swappedIn;

    public SwapResult(MemoryProcess swappedOut, MemoryProcess swappedIn) {
        this.swappedOut = swappedOut;
        this.swappedIn = swappedIn;
    }
}