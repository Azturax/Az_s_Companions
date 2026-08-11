package com.azscompanions.entity;

/**
 * Ring buffer of world-space trail samples (foot / cloud height). Pure Java.
 */
public final class FlightAuraTrailBuffer {
    private final double[] xs;
    private final double[] ys;
    private final double[] zs;
    private final int capacity;
    private int size;
    private int head;

    public FlightAuraTrailBuffer(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.xs = new double[this.capacity];
        this.ys = new double[this.capacity];
        this.zs = new double[this.capacity];
    }

    public void clear() {
        size = 0;
        head = 0;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return capacity;
    }

    /** Push newest sample (overwrites oldest when full). */
    public void push(double x, double y, double z) {
        xs[head] = x;
        ys[head] = y;
        zs[head] = z;
        head = (head + 1) % capacity;
        if (size < capacity) {
            size++;
        }
    }

    /**
     * Read sample where {@code indexFromNewest == 0} is the newest point.
     *
     * @return false if out of range
     */
    public boolean getFromNewest(int indexFromNewest, double[] out3) {
        if (indexFromNewest < 0 || indexFromNewest >= size || out3 == null || out3.length < 3) {
            return false;
        }
        int idx = head - 1 - indexFromNewest;
        while (idx < 0) {
            idx += capacity;
        }
        out3[0] = xs[idx];
        out3[1] = ys[idx];
        out3[2] = zs[idx];
        return true;
    }

    /** Skip push when nearly identical to newest (reduces fill when hovering). */
    public boolean shouldAccept(double x, double y, double z, double minDistSq) {
        if (size == 0) {
            return true;
        }
        double[] newest = new double[3];
        getFromNewest(0, newest);
        double dx = x - newest[0];
        double dy = y - newest[1];
        double dz = z - newest[2];
        return dx * dx + dy * dy + dz * dz >= minDistSq;
    }
}
