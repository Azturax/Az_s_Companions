package com.azscompanions.deposit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compact S2C payload for deposit chest selection.
 * Format: {@code mode\u001fdim@x,y,z;dim@x,y,z…}
 */
public record DepositSelectionSnapshot(boolean selecting, List<DepositChestRef> chests) {
    public static final DepositSelectionSnapshot OFF = new DepositSelectionSnapshot(false, List.of());

    public DepositSelectionSnapshot {
        chests = chests == null ? List.of() : List.copyOf(chests);
    }

    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(selecting ? '1' : '0').append('\u001f');
        for (int i = 0; i < chests.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(chests.get(i).encodePart());
        }
        return sb.toString();
    }

    public static DepositSelectionSnapshot decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return OFF;
        }
        String[] parts = raw.split("\u001f", 2);
        boolean selecting = parts.length > 0 && "1".equals(parts[0]);
        if (parts.length < 2 || parts[1].isBlank()) {
            return new DepositSelectionSnapshot(selecting, List.of());
        }
        List<DepositChestRef> list = new ArrayList<>();
        for (String piece : parts[1].split(";")) {
            DepositChestRef ref = DepositChestRef.parsePart(piece);
            if (ref != null) {
                list.add(ref);
            }
        }
        return new DepositSelectionSnapshot(selecting, Collections.unmodifiableList(list));
    }
}
