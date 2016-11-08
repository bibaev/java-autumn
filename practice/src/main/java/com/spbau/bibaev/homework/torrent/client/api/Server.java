package com.spbau.bibaev.homework.torrent.client.api;

import com.spbau.bibaev.homework.torrent.common.ClientInfo;
import com.spbau.bibaev.homework.torrent.common.FileInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
public interface Server {
  Map<Integer, FileInfo> list();

  int upload(@NotNull FileInfo info);

  List<ClientInfo> sources(int fileId);

  boolean update(int port, @NotNull Collection<Integer> ids);
}