package edu.mit.compilers.dataflow.regAllocation;

public class LiveInterval {
    public String varName;
    public int startpoint;
    public int endpoint;

    public LiveInterval(String varName, int startpoint, int endpoint) {
        this.varName = varName;
        this.startpoint = startpoint;
        this.endpoint = endpoint;
    }

    public int compareStartpoint(LiveInterval other) {
        if(startpoint == other.startpoint)
            return 0;
        return startpoint < other.startpoint ? -1 : 1;
    }

    public int compareEndpoint(LiveInterval other) {
        if(endpoint == other.endpoint)
            return 0;
        return endpoint < other.endpoint ? -1 : 1;
    }

}
