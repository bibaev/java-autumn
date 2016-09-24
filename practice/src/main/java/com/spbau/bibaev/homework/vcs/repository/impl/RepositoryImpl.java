package com.spbau.bibaev.homework.vcs.repository.impl;

import com.spbau.bibaev.homework.vcs.repository.api.*;
import com.spbau.bibaev.homework.vcs.util.FilesUtil;
import com.spbau.bibaev.homework.vcs.util.XmlSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class RepositoryImpl implements Repository{
  static final String VCS_DIRECTORY_NAME = ".my_vcs";
  static final String DEFAULT_USER_NAME = System.getProperty("user.name");
  private static final String DEFAULT_BRANCH_NAME = "master";
  private static final String METADATA_FILENAME = "metadata.xml";
  private final Map<String, BranchImpl> myName2Branch;
  private final ProjectImpl myProject;
  private String myCurrentBranchName;
  private String myUserName;
  private Path myRepositoryMetadataDirectory;

  @NotNull
  public static RepositoryImpl open(@NotNull File directory) throws IOException {
    File currentDirectory = directory;
    while (currentDirectory != null && !FilesUtil.isContainsDirectory(currentDirectory, VCS_DIRECTORY_NAME)) {
      currentDirectory = currentDirectory.getParentFile();
    }

    if (currentDirectory == null) {
      throw new IOException("Could not find repository root in " + directory.getAbsolutePath());
    }

    return openHere(currentDirectory);
  }

  private static RepositoryImpl openHere(@NotNull File directory) throws IOException {
    File metadataDirectory = FilesUtil.findDirectoryByName(directory, VCS_DIRECTORY_NAME);
    if (metadataDirectory == null) {
      throw new IOException("Could not find repository root in " + directory.getAbsolutePath());
    }

    File[] branchDirectories = metadataDirectory.listFiles(File::isDirectory);
    if (branchDirectories == null) {
      throw new IOException("Could not read any branch information");
    }

    Map<String, BranchImpl> branches = new HashMap<>();
    for (File file : branchDirectories) {
      BranchImpl branch = BranchImpl.read(file);
      branches.put(branch.getName(), branch);
    }

    File metadataFile = FilesUtil.findFileByName(metadataDirectory, METADATA_FILENAME);
    if (metadataFile == null) {
      throw new IOException("RepositoryImpl metadata file not found");
    }
    RepositoryMetadata meta;
    try {
      meta = XmlSerializer.deserialize(metadataFile, RepositoryMetadata.class);
    } catch (JAXBException e) {
      throw new IOException("Could not read from repository metadata file");
    }

    ProjectImpl project = ProjectImpl.open(directory, metadataDirectory);
    return new RepositoryImpl(metadataDirectory.toPath(), meta, branches, project);
  }

  @Nullable
  public Branch getBranchByName(@NotNull String name) {
    return myName2Branch.getOrDefault(name, null);
  }

  @NotNull
  public Branch getCurrentBranch() {
    return myName2Branch.get(myCurrentBranchName);
  }

  @Override
  public Revision checkout(@NotNull Branch branch) throws IOException {
    Path tmpDirectory = Files.createTempDirectory("revision");
    branch.getLastRevision().getSnapshot().restore(tmpDirectory);
    myProject.clean();
    FilesUtil.recursiveCopyDirectory(tmpDirectory, myProject.getRootDirectory());
    myCurrentBranchName = branch.getName();
    save();
    return branch.getLastRevision();
  }

  @NotNull
  @Override
  public Revision checkout(@NotNull Revision revision) throws IOException {
    String newBranchName = myCurrentBranchName + revision.getHash();
    BranchImpl branch = myName2Branch.get(myCurrentBranchName);
    Collection<RevisionImpl> earlierRevisions = branch.getRevisionImpls().stream()
        .filter(rev -> rev.getDate().compareTo(revision.getDate()) <= 0)
        .collect(Collectors.toList());
    BranchImpl newBranch = BranchImpl.createNewBranch(myRepositoryMetadataDirectory, newBranchName, earlierRevisions);
    myCurrentBranchName = newBranch.getName();
    myName2Branch.put(myCurrentBranchName, newBranch);
    checkout(branch);

    return branch.getLastRevision();
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  @NotNull
  @Override
  public List<Branch> getBranches() {
    return myName2Branch.values().stream().collect(Collectors.toList());
  }

  public static void createNewRepository(@NotNull File directory) throws IOException {
    File metadataDirectory = new File(directory.getAbsolutePath() + File.separator + VCS_DIRECTORY_NAME);
    if (metadataDirectory.exists() || !metadataDirectory.mkdir()) {
      throw new IOException("RepositoryImpl in \"" + directory.getAbsolutePath() + "\" already exists");
    }

    BranchImpl.createNewBranch(metadataDirectory.toPath(), DEFAULT_BRANCH_NAME, Collections.emptyList());

    File metadataFile = new File(metadataDirectory.getAbsolutePath() + File.separator + METADATA_FILENAME);
    try {
      if (!metadataFile.createNewFile()) {
        throw new IOException("RepositoryImpl metadata file already exists");
      }

      XmlSerializer.serialize(metadataFile, RepositoryMetadata.class, RepositoryMetadata.defaultMeta());
    } catch (JAXBException e) {
      throw new IOException("Could not serialize repository metadata", e);
    }
  }

  private RepositoryImpl(@NotNull Path metaDirectory, @NotNull RepositoryMetadata meta,
                         @NotNull Map<String, BranchImpl> branches, @NotNull ProjectImpl project) {
    myRepositoryMetadataDirectory = metaDirectory;
    myName2Branch = branches;
    myProject = project;
    myCurrentBranchName = meta.currentBranch;
    myUserName = meta.userName;
  }

  private void save() throws IOException {
    try {
      File metadataFile = new File(myRepositoryMetadataDirectory.toAbsolutePath().toString() + File.separator +
          METADATA_FILENAME);
      XmlSerializer.serialize(metadataFile, RepositoryMetadata.class,
          new RepositoryMetadata(myCurrentBranchName, myUserName));
    } catch (JAXBException e) {
      throw new IOException("Could not save repository meta", e);
    }
  }

  @NotNull
  public String getUserName() {
    return myUserName;
  }

  public void setUserName(@NotNull String newName) throws IOException {
    myUserName = newName;
    save();
  }

  @NotNull
  public Branch createNewBranch(@NotNull String name) throws IOException {
    BranchImpl branch = BranchImpl.createNewBranch(myRepositoryMetadataDirectory.toFile(),
        name, myName2Branch.get(myCurrentBranchName));
    myName2Branch.put(name, branch);
    return branch;
  }

  @NotNull
  public Revision commitChanges(@NotNull String message) throws IOException {
    Date date = new Date();
    BranchImpl currentBranch = myName2Branch.get(myCurrentBranchName);
    return currentBranch.commitChanges(myProject, message, date, myUserName);
  }

  @XmlRootElement
  private static class RepositoryMetadata {
    @XmlElement(name = "branch")
    String currentBranch;
    @XmlElement(name = "user")
    String userName;

    @SuppressWarnings("unused")
    RepositoryMetadata() {
    }

    RepositoryMetadata(@NotNull String branch, @NotNull String user) {
      currentBranch = branch;
      userName = user;
    }

    static RepositoryMetadata defaultMeta() {
      return new RepositoryMetadata(DEFAULT_BRANCH_NAME, DEFAULT_USER_NAME);
    }
  }
}