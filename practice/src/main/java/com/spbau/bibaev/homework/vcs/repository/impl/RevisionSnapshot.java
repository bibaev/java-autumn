package com.spbau.bibaev.homework.vcs.repository.impl;

import com.spbau.bibaev.homework.vcs.repository.api.Snapshot;
import com.spbau.bibaev.homework.vcs.util.FilesUtil;
import com.spbau.bibaev.homework.vcs.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class RevisionSnapshot implements Snapshot {
  private final Path myPathToSnapshot;
  private final Map<String, Pair<Long, Long>> myPositionMapping;

  RevisionSnapshot(@NotNull Path file, @NotNull Map<String, Pair<Long, Long>> positionsMapping) {
    myPathToSnapshot = file;
    myPositionMapping = new HashMap<>(positionsMapping);
  }

  public void restore(@NotNull Path directory) throws IOException {
    for (String pathSuffix : myPositionMapping.keySet()) {
      long offset = myPositionMapping.get(pathSuffix).first;
      long length = myPositionMapping.get(pathSuffix).second;

      Path path = directory.resolve(pathSuffix);
      Files.createDirectories(path.getParent());
      File outputFile = Files.createFile(path).toFile();

      FileInputStream stream = new FileInputStream(myPathToSnapshot.toFile());
      stream.skip(offset);
      writeToFile(stream, outputFile, length);
    }
  }

  private static void writeToFile(@NotNull InputStream in, @NotNull File file, long len) throws IOException {
    OutputStream out = new FileOutputStream(file);

    byte[] buffer = new byte[4096];
    long remain = len;
    while (remain > 0) {
      int readBytes = in.read(buffer, 0, (int) Math.min(buffer.length, remain));
      out.write(buffer, 0, readBytes);
      remain -= readBytes;
    }

    out.close();
  }
}
