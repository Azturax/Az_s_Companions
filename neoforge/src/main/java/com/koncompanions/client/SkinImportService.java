package com.koncompanions.client;

import com.koncompanions.KonCompanions;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Local PNG skin import only — URL downloads are intentionally disabled.
 * Supports resource-location skins and files under config/koncompanions/skins.
 */
@OnlyIn(Dist.CLIENT)
public final class SkinImportService {
    private SkinImportService() {
    }

    public static Optional<Path> importLocalPng(Path gameDir, Path sourcePng, String companionKey) {
        if (sourcePng == null || !Files.isRegularFile(sourcePng)) {
            return Optional.empty();
        }
        String name = sourcePng.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".png")) {
            KonCompanions.LOGGER.warn("Rejected non-PNG skin import: {}", sourcePng);
            return Optional.empty();
        }
        String uri = sourcePng.toUri().toString();
        if (uri.startsWith("http:") || uri.startsWith("https:")) {
            KonCompanions.LOGGER.warn("URL skin import is disabled");
            return Optional.empty();
        }
        try {
            Path destDir = gameDir.resolve("config").resolve(KonCompanions.MOD_ID).resolve("skins");
            Files.createDirectories(destDir);
            Path dest = destDir.resolve(sanitize(companionKey) + ".png");
            Files.copy(sourcePng, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return Optional.of(dest);
        } catch (IOException e) {
            KonCompanions.LOGGER.error("Failed importing local skin", e);
            return Optional.empty();
        }
    }

    /** Opens a native OS file chooser off-thread; result is delivered on the MC client thread. */
    public static void pickPngAsync(java.util.function.Consumer<Optional<Path>> onClientThread) {
        Minecraft mc = Minecraft.getInstance();
        CompletableFuture.supplyAsync(SkinImportService::pickPngBlocking)
                .whenComplete((result, err) -> mc.execute(() -> {
                    if (err != null) {
                        KonCompanions.LOGGER.error("Skin file dialog failed", err);
                        onClientThread.accept(Optional.empty());
                    } else {
                        onClientThread.accept(result == null ? Optional.empty() : result);
                    }
                }));
    }

    private static Optional<Path> pickPngBlocking() {
        try {
            FileDialog dialog = new FileDialog((Frame) null, "Select companion skin PNG", FileDialog.LOAD);
            dialog.setFilenameFilter((dir, name) -> name != null && name.toLowerCase(Locale.ROOT).endsWith(".png"));
            dialog.setFile("*.png");
            dialog.setVisible(true);
            String file = dialog.getFile();
            String dir = dialog.getDirectory();
            if (file != null && dir != null) {
                Path path = Path.of(dir, file);
                if (Files.isRegularFile(path)) {
                    return Optional.of(path);
                }
            }
        } catch (Throwable awtFail) {
            KonCompanions.LOGGER.debug("AWT FileDialog unavailable, trying JFileChooser", awtFail);
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select companion skin PNG");
                chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File selected = chooser.getSelectedFile();
                    if (selected != null && selected.isFile()) {
                        return Optional.of(selected.toPath());
                    }
                }
            } catch (Throwable swingFail) {
                KonCompanions.LOGGER.error("No native file dialog available", swingFail);
            }
        }
        return Optional.empty();
    }

    public static List<String> listLocalSkinFiles(Path gameDir) {
        Path dir = gameDir.resolve("config").resolve(KonCompanions.MOD_ID).resolve("skins");
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return names;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.naturalOrder())
                    .forEach(names::add);
        } catch (IOException e) {
            KonCompanions.LOGGER.warn("Failed listing local skins", e);
        }
        return names;
    }

    public static Optional<ResourceLocation> parseResourceSkin(String value) {
        return Optional.ofNullable(ResourceLocation.tryParse(value));
    }

    private static String sanitize(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }
}
