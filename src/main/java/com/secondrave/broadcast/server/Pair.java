package com.secondrave.broadcast.server;

/**
 * Created by benstpierre on 14-12-02.
 */
public class Pair<V1, V2> {


    private V1 valueOne;

    private V2 valueTwo;

    public Pair() {
    }

    public Pair(V1 valueOne, V2 valueTwo) {
        this.valueOne = valueOne;
        this.valueTwo = valueTwo;
    }

    public V1 getValueOne() {
        return valueOne;
    }

    public void setValueOne(V1 valueOne) {
        this.valueOne = valueOne;
    }

    public V2 getValueTwo() {
        return valueTwo;
    }

    public void setValueTwo(V2 valueTwo) {
        this.valueTwo = valueTwo;
    }
}
