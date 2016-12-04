package com.spbau.bibaev.homework.torrent.client.ui;

import com.spbau.bibaev.homework.torrent.client.api.ClientFileInfo;
import com.spbau.bibaev.homework.torrent.client.ui.util.AbstractTableColumnDescriptor;
import com.spbau.bibaev.homework.torrent.client.ui.util.AbstractTableModelWithColumns;
import com.spbau.bibaev.homework.torrent.client.ui.util.SizeUtil;
import com.spbau.bibaev.homework.torrent.common.Details;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
class LocalFilesView extends JPanel {
  private final JTable myTable;
  private final List<FileInformation> myItems = new ArrayList<>();
  private final Map<Integer, Integer> myId2ItemIndex = new HashMap<>();

  LocalFilesView() {
    super(new BorderLayout());
    myTable = new JTable(new MyTableModelWithColumns());

    myTable.setDefaultRenderer(Long.class, new DefaultTableCellRenderer() {
      @Override
      protected void setValue(Object value) {
        long size = (Long) value;
        setHorizontalAlignment(RIGHT);
        setText(SizeUtil.getPrettySize(size));
      }
    });
    myTable.setShowVerticalLines(true);
    myTable.setShowHorizontalLines(false);

    add(new JLabel("Loaded files and progress", SwingConstants.CENTER), BorderLayout.NORTH);
    add(new JScrollPane(myTable), BorderLayout.CENTER);
  }

  void setFiles(@NotNull Map<Path, ClientFileInfo> files) {
    myItems.clear();
    myId2ItemIndex.clear();
    files.forEach((path, info) -> addFile(path.toString(), info));
  }

  void addFile(@NotNull String path, @NotNull ClientFileInfo info) {
    assert SwingUtilities.isEventDispatchThread();

    myId2ItemIndex.put(info.getId(), myItems.size());
    myItems.add(new FileInformation(info.getId(), path, info.getSize(),
        Details.partCount(info.getSize()), info.getParts().size()));
    ((MyTableModelWithColumns) myTable.getModel()).fireTableDataChanged();
  }

  void partLoaded(int fileId) {
    assert SwingUtilities.isEventDispatchThread();

    FileInformation info = myItems.get(myId2ItemIndex.get(fileId));
    assert info.loadedParts < info.totalParts;
    info.loadedParts++;
    ((MyTableModelWithColumns) myTable.getModel()).fireTableDataChanged();
  }

  private static class FileInformation {
    final int id;
    final String path;
    final long size;
    final int totalParts;
    int loadedParts;

    private FileInformation(int id, String path, long size, int totalParts, int loadedParts) {
      this.id = id;
      this.path = path;
      this.size = size;
      this.totalParts = totalParts;
      this.loadedParts = loadedParts;
    }
  }

  private class MyTableModelWithColumns extends AbstractTableModelWithColumns {
    MyTableModelWithColumns() {
      super(new AbstractTableColumnDescriptor[]{
          new AbstractTableColumnDescriptor("ID", Integer.class) {
            @Override
            public Object getValue(int ix) {
              return myItems.get(ix).id;
            }
          },
          new AbstractTableColumnDescriptor("Name", String.class) {
            @Override
            public Object getValue(int ix) {
              final String path = myItems.get(ix).path;
              return path.substring(path.lastIndexOf(File.separatorChar) + 1);
            }
          },
          new AbstractTableColumnDescriptor("Path", String.class) {
            @Override
            public Object getValue(int ix) {
              return myItems.get(ix).path;
            }
          },
          new AbstractTableColumnDescriptor("Size", Long.class) {
            @Override
            public Object getValue(int ix) {
              return myItems.get(ix).size;
            }
          },
          new AbstractTableColumnDescriptor("Status", String.class) {
            @Override
            public Object getValue(int ix) {
              FileInformation info = myItems.get(ix);
              if (info.loadedParts == info.totalParts) {
                return "Completed";
              }

              return String.format("%d / %d", info.loadedParts, info.totalParts);
            }
          }
      });
    }

    @Override
    public int getRowCount() {
      return myItems.size();
    }
  }
}
