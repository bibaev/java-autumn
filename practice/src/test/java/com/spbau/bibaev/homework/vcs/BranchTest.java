package com.spbau.bibaev.homework.vcs;

import com.spbau.bibaev.homework.vcs.repository.api.Branch;
import com.spbau.bibaev.homework.vcs.repository.api.Repository;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class BranchTest extends RepositoryTestCase {
  private static String myBranchName = "develop";

  @Test
  public void createNewBranch() throws IOException {
    Repository repository = openRepository();

    assertNull(repository.getBranchByName(myBranchName));

    Branch developBranch = repository.createNewBranch(myBranchName);
    assertNotNull(repository.getBranchByName(myBranchName));
    assertNotEquals("Need only create branch, without checkout", developBranch, repository.getCurrentBranch());
    assertEquals(myBranchName, developBranch.getName());

    Branch currentBranch = repository.getCurrentBranch();
    assertEquals(currentBranch.getRevisions().size(), developBranch.getRevisions().size());
    assertEquals(currentBranch.getLastRevision().getDate(), currentBranch.getLastRevision().getDate());
  }
}