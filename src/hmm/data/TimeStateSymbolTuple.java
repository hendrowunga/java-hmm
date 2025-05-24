package hmm.data;

/*
 * © 2025 hendrowunga, University of Sanata Dharma
 * Created on 5/25/25
 */


public class TimeStateSymbolTuple {

    public final int time;
    public final int stateIndex;
    public final int symbolIndex;



    public TimeStateSymbolTuple(int time, int stateIndex, int symbolIndex) {
        this.time = time;
        this.stateIndex = stateIndex;
        this.symbolIndex = symbolIndex;
    }

    public int getTime() {
        return time;
    }

    public int getStateIndex() {
        return stateIndex;
    }

    public int getSymbolIndex() {
        return symbolIndex;
    }
}