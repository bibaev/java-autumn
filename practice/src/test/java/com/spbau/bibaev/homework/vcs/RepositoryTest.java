package com.spbau.bibaev.homework.vcs;

import com.spbau.bibaev.homework.vcs.repository.impl.v2.RepositoryImpl;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RepositoryTest extends RepositoryTestCase {

  @Test
  public void openFromSubdirectory() throws IOException {
    final Path srcDir = myRule.getRoot().toPath().resolve("src");
    final RepositoryImpl repo = RepositoryImpl.openRepository(srcDir);
    assertNotNull(repo);
    assertEquals(repo.getWorkingDirectory().getRootDirectory(), myRule.getRoot().toPath());
  }
}
