package com.azscompanions.entity;

/**
 * Plugin-style particle shape for Jindujun (Flying Nimbus).
 * <p>
 * Pattern is a compact silhouette of the Desktop {@code Jindujun.png} texture:
 * vertical spine, three cross-bars, and a right-side ladder — spawned as
 * {@code ParticleTypes.ENCHANT} at cloud / foot height (never into first-person face).
 * Pure math; loaders call {@link #shapePointCount()} / {@link #localOffset} then
 * {@code level.addParticle(ENCHANT, ...)}.
 */
public final class JindujunParticleSupport {
    /**
     * Occupancy grid (rows top→bottom, cols left→right). {@code '#'} = particle site.
     * Derived from the colorful cross/ladder silhouette in the Jindujun reference texture.
     */
    public static final String[] SHAPE = {
            "..#####...",
            "..#####...",
            "..#####...",
            "##########",
            "..####.###",
            "..####.###",
            "##########",
            "..####.###",
            "##########",
            "..####.##.",
            "#########.",
    };

    /** Blocks per shape pixel — compact trail (not a full-size glyph billboard). */
    public static final float PIXEL = 0.045f * JindujunSupport.SCALE;
    /** Keep stream at cloud-foot height (never rider torso / eyes). */
    public static final float STREAM_Y = 0.04f * JindujunSupport.SCALE;
    /** Push pattern well behind the cloud along facing. */
    public static final float BEHIND = 0.85f * JindujunSupport.SCALE;
    /** Flatten vertical centering so glyphs stay under the deck. */
    public static final float SHAPE_CENTER_Y = 0.06f * JindujunSupport.SCALE;
    /** Min speed² (blocks/tick) before the stream densifies. */
    public static final double MOVE_SPEED_SQ = 0.08d * 0.08d;

    private static final int COLS;
    private static final int ROWS;
    private static final float[] LOCAL_X;
    private static final float[] LOCAL_Y;
    private static final float[] LOCAL_Z;
    private static final int POINT_COUNT;

    static {
        ROWS = SHAPE.length;
        int cols = 0;
        for (String row : SHAPE) {
            cols = Math.max(cols, row.length());
        }
        COLS = cols;
        int count = 0;
        for (String row : SHAPE) {
            for (int c = 0; c < row.length(); c++) {
                if (row.charAt(c) == '#') {
                    count++;
                }
            }
        }
        POINT_COUNT = count;
        LOCAL_X = new float[POINT_COUNT];
        LOCAL_Y = new float[POINT_COUNT];
        LOCAL_Z = new float[POINT_COUNT];
        int i = 0;
        float midCol = (COLS - 1) * 0.5f;
        float midRow = (ROWS - 1) * 0.5f;
        for (int r = 0; r < ROWS; r++) {
            String row = SHAPE[r];
            for (int c = 0; c < row.length(); c++) {
                if (row.charAt(c) != '#') {
                    continue;
                }
                // Local: +X right, +Y up, +Z behind (trail)
                LOCAL_X[i] = (c - midCol) * PIXEL;
                LOCAL_Y[i] = (midRow - r) * PIXEL + SHAPE_CENTER_Y;
                LOCAL_Z[i] = BEHIND;
                i++;
            }
        }
    }

    private JindujunParticleSupport() {
    }

    public static int shapePointCount() {
        return POINT_COUNT;
    }

    public static int shapeRows() {
        return ROWS;
    }

    public static int shapeCols() {
        return COLS;
    }

    /** Local X (right) for packed shape index. */
    public static float localX(int index) {
        return LOCAL_X[index];
    }

    /** Local Y (up) for packed shape index. */
    public static float localY(int index) {
        return LOCAL_Y[index];
    }

    /** Local Z (behind) for packed shape index. */
    public static float localZ(int index) {
        return LOCAL_Z[index];
    }

    /**
     * Show enchant stream only on the nimbus while someone is riding.
     */
    public static boolean shouldSpawn(boolean hasPassenger) {
        return hasPassenger;
    }

    public static boolean movingFastEnough(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz >= MOVE_SPEED_SQ;
    }

    /**
     * How many shape sites to emit this tick (subset cycles through the grid).
     */
    public static int particlesThisTick(boolean movingFast, int tickAge) {
        if (!movingFast) {
            // Idle hover: very sparse ambient glyphs
            return Math.max(1, POINT_COUNT / 18);
        }
        // Moving: light trail — never a purple face-blast
        return Math.max(2, POINT_COUNT / 10 + (tickAge & 1));
    }

    /**
     * Shape index to spawn for slot {@code slot} on {@code tickAge}.
     */
    public static int pointIndex(int tickAge, int slot) {
        if (POINT_COUNT <= 0) {
            return 0;
        }
        return Math.floorMod(tickAge * 3 + slot * 7, POINT_COUNT);
    }

    /**
     * World-space offset from entity origin for a local (right, up, behind) point.
     *
     * @param yawDegrees entity yaw (Minecraft degrees)
     * @param out        length ≥ 3; filled with dx, dy, dz
     */
    public static void worldOffset(float localRight, float localUp, float localBehind, float yawDegrees, float[] out) {
        float yawRad = yawDegrees * ((float) Math.PI / 180.0f);
        // Minecraft: yaw 0 looks south (+Z); rotate local right/behind into world XZ
        float sin = (float) Math.sin(yawRad);
        float cos = (float) Math.cos(yawRad);
        // right = (-cos, 0, -sin) when facing yaw… use standard entity basis:
        // forward = (-sin, 0, cos), right = (cos, 0, sin), behind = -forward
        float fx = -sin;
        float fz = cos;
        float rx = cos;
        float rz = sin;
        out[0] = rx * localRight + (-fx) * localBehind;
        out[1] = STREAM_Y + localUp;
        out[2] = rz * localRight + (-fz) * localBehind;
    }

    /**
     * Enchant particle velocity toward the cloud center (enchant-table style drift).
     *
     * @param out length ≥ 3; filled with vx, vy, vz relative offsets used by ENCHANT
     */
    public static void enchantVelocity(float offsetX, float offsetY, float offsetZ, float[] out) {
        // ENCHANT floats interpret these as target offsets (enchant-table style).
        // Pull horizontally toward the cloud; keep Y calm so glyphs do not rise into the rider.
        out[0] = -offsetX * 0.55f;
        out[1] = -offsetY * 0.2f;
        out[2] = -offsetZ * 0.55f;
    }
}
