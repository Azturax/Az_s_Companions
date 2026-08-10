package com.azscompanions.deposit;

import java.util.Objects;

/** Dimension-scoped chest block for gather deposit selection (loader-agnostic). */
public record DepositChestRef(String dimension, int x, int y, int z) {
    public DepositChestRef {
        dimension = dimension == null || dimension.isBlank() ? "minecraft:overworld" : dimension.trim();
    }

    public int manhattan(int ox, int oy, int oz) {
        return Math.abs(x - ox) + Math.abs(y - oy) + Math.abs(z - oz);
    }

    public String encodePart() {
        return dimension + "@" + x + "," + y + "," + z;
    }

    public static DepositChestRef parsePart(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int at = raw.lastIndexOf('@');
        if (at <= 0 || at >= raw.length() - 1) {
            return null;
        }
        String dim = raw.substring(0, at);
        String[] xyz = raw.substring(at + 1).split(",", 3);
        if (xyz.length != 3) {
            return null;
        }
        try {
            return new DepositChestRef(dim,
                    Integer.parseInt(xyz[0].trim()),
                    Integer.parseInt(xyz[1].trim()),
                    Integer.parseInt(xyz[2].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DepositChestRef that)) {
            return false;
        }
        return x == that.x && y == that.y && z == that.z && Objects.equals(dimension, that.dimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, x, y, z);
    }
}
