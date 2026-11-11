package com.magneticraft2.client.menu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Makes a disposable copy of a deliberately prepared singleplayer world.
 * Never runs gameplay against the player's editable source save.
 */
final class LivingMenuWorldCopy {
    static final String SOURCE_ID = "magneticraft2_menu_source";
    static final String RUNTIME_ID = "_magneticraft2_living_menu_runtime";
    private static final String STAGING_ID = "_magneticraft2_living_menu_staging";
    private static final String MARKER = ".magneticraft2-generated-menu-copy";
    private static final long MAX_FILES = 10_000;

    private LivingMenuWorldCopy() {}

    static boolean sourceExists(Path source) {
        return Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)
                && Files.isRegularFile(source.resolve("level.dat"), LinkOption.NOFOLLOW_LINKS);
    }

    static Path prepare(Path savesDirectory, LivingMenuScene scene) throws IOException {
        Path source = scene.worldDirectory;
        if (!sourceExists(source)) {
            throw new IOException("Missing or invalid living-menu world: " + source);
        }
        Path runtime = savesDirectory.resolve(RUNTIME_ID);
        Path staging = savesDirectory.resolve(STAGING_ID);
        long maxBytes = scene.maxWorldMiB * 1024L * 1024L;

        // A colliding user save must never be erased, even if it has the reserved name.
        deleteGeneratedDirectoryIfPresent(runtime);
        deleteGeneratedDirectoryIfPresent(staging);
        Files.createDirectory(staging);
        // Mark the new directory immediately so a failed copy can be cleaned on retry.
        Files.writeString(staging.resolve(MARKER), "Disposable Magneticraft2 menu world. Do not edit.\n");
        // Bytes, files and ACTUAL stored chunks, not a count of region filenames.
        final long[] totals = {0, 0, 0};
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                    if (attributes.isSymbolicLink()) {
                        throw new IOException("Symbolic links are not allowed in menu source world: " + directory);
                    }
                    Files.createDirectories(staging.resolve(source.relativize(directory)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    if (attributes.isSymbolicLink() || !attributes.isRegularFile()) {
                        throw new IOException("Unsupported file in menu source world: " + file);
                    }
                    // The source lock belongs to the source world and must not be cloned.
                    if (file.getFileName().toString().equals("session.lock")) {
                        return FileVisitResult.CONTINUE;
                    }
                    totals[0] += attributes.size();
                    totals[1]++;
                    if (totals[0] > maxBytes || totals[1] > MAX_FILES) {
                        throw new IOException("Menu world exceeds " + scene.maxWorldMiB
                                + " MiB or 10,000 source files: " + scene.id);
                    }
                    if (file.getParent().getFileName().toString().equals("region")
                            && file.getFileName().toString().endsWith(".mca")) {
                        totals[2] += countStoredChunks(file);
                        if (totals[2] > scene.maxStoredChunks) {
                            throw new IOException("Menu world exceeds " + scene.maxStoredChunks
                                    + " stored chunks: " + scene.id);
                        }
                    }
                    Files.copy(file, staging.resolve(source.relativize(file)));
                    return FileVisitResult.CONTINUE;
                }
            });
            try {
                Files.move(staging, runtime, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(staging, runtime);
            }
            return runtime.toAbsolutePath().normalize();
        } catch (IOException | RuntimeException error) {
            deleteGeneratedDirectoryIfPresent(staging);
            throw error;
        }
    }

    /**
     * Each Anvil region header contains 1024 4-byte location entries. A nonzero
     * entry means there is a saved chunk. Counting .mca filenames is incorrect.
     */
    private static int countStoredChunks(Path regionFile) throws IOException {
        if (Files.size(regionFile) < 8192) {
            throw new IOException("Invalid Minecraft region file: " + regionFile);
        }
        byte[] header;
        try (InputStream input = Files.newInputStream(regionFile)) {
            header = input.readNBytes(4096);
        }
        if (header.length != 4096) {
            throw new IOException("Truncated Minecraft region header: " + regionFile);
        }
        int count = 0;
        for (int i = 0; i < 4096; i += 4) {
            // The first three bytes store the chunk offset in 4-KiB sectors.
            if (header[i] != 0 || header[i + 1] != 0 || header[i + 2] != 0) {
                ++count;
            }
        }
        return count;
    }

    private static void deleteGeneratedDirectoryIfPresent(Path directory) throws IOException {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                || !Files.isRegularFile(directory.resolve(MARKER), LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Refusing to overwrite non-generated menu world: " + directory);
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                if (exception != null) {
                    throw exception;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
