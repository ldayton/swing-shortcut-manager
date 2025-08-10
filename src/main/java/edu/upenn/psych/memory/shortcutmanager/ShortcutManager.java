package edu.upenn.psych.memory.shortcutmanager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

public class ShortcutManager extends JFrame {
  private final List<XAction> defaultXActions;
  private final UserDB userdb;

  @SuppressWarnings("UnusedVariable") // Used in nested ShortcutTable class
  private final XActionListener listener;

  private final ContentPane contentPane;

  public ShortcutManager(URL url, String namespace, XActionListener listener) {
    this.defaultXActions = new XActionParser(url).getXactions();
    this.userdb = new UserDB(namespace, defaultXActions, listener);
    this.listener = listener;

    userdb.persistDefaults(false);

    this.contentPane = new ContentPane();
    setSize(new Dimension(800, contentPane.getPreferredSize().height));
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new EscapeWindowListener());
    setTitle("Keyboard Shortcuts Manager");
    setContentPane(contentPane);

    Map<String, Shortcut> curShortMap = userdb.retrieveAll();
    for (String id : curShortMap.keySet()) {
      XAction defaultXAction = findXActionById(id);
      if (defaultXAction != null) {
        Shortcut shortOpt = curShortMap.get(id);
        XAction newXAction = defaultXAction.withShortcut(userdb.retrieve(id));
        listener.xActionUpdated(newXAction, shortOpt);
      }
    }
  }

  private XAction findXActionById(String id) {
    for (XAction xAction : defaultXActions) {
      if (xAction.getId().equals(id)) {
        return xAction;
      }
    }
    return null;
  }

  private class ContentPane extends JPanel {
    private final Scroller scroller;
    private final ResetButtonPanel resetButtonPanel;

    public ContentPane() {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      this.scroller = new Scroller();
      this.resetButtonPanel = new ResetButtonPanel();

      add(scroller);
      add(Box.createVerticalBox());
      add(resetButtonPanel);
    }

    private class Scroller extends JScrollPane {
      public Scroller() {
        setViewportView(
            new ShortcutTable(defaultXActions.toArray(new XAction[0]), userdb, listener));

        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        getHorizontalScrollBar().setUnitIncrement(15);
        getVerticalScrollBar().setUnitIncrement(15);

        String exit = "exit";
        KeyStroke escStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escStroke, exit);
        getActionMap()
            .put(
                exit,
                new AbstractAction() {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    WindowEvent event =
                        new WindowEvent(ShortcutManager.this, WindowEvent.WINDOW_CLOSING);
                    new EscapeWindowListener().windowClosing(event);
                  }
                });
      }
    }

    private class ResetButtonPanel extends JPanel {
      public ResetButtonPanel() {
        JButton button =
            new JButton(
                new AbstractAction("Restore Defaults") {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    int res =
                        JOptionPane.showConfirmDialog(
                            ShortcutManager.this, "Restore all shortcuts to defaults?");
                    if (res == JOptionPane.YES_OPTION) {
                      userdb.persistDefaults(true);
                      ContentPane.this.repaint();
                    }
                  }
                });

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());
        add(button);
        add(Box.createHorizontalGlue());
      }
    }
  }

  private final class EscapeWindowListener extends WindowAdapter {
    @Override
    public void windowClosing(WindowEvent e) {
      ShortcutManager.this.setVisible(false);
    }
  }
}

class ShortcutTable extends JTable {
  private final XAction[] defaultXActions;
  private final UserDB userdb;

  @SuppressWarnings("UnusedVariable") // Listener is passed to UserDB for notifications
  private final XActionListener listener;

  private final int leftRightPad = 10;
  private final ShortcutTableModel shortcutTableModel;

  public ShortcutTable(XAction[] defaultXActions, UserDB userdb, XActionListener listener) {
    this.defaultXActions = defaultXActions;
    this.userdb = userdb;
    this.listener = listener;
    this.shortcutTableModel = new ShortcutTableModel();

    setModel(shortcutTableModel);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setFillsViewportHeight(true);
    addKeyListener(new ShortcutKeyAdapter());

    // Set column widths
    for (int c = 0; c < getColumnCount(); c++) {
      int maxWidth = 0;
      for (int r = 0; r < getRowCount(); r++) {
        Object value = getValueAt(r, c);
        Component comp =
            getCellRenderer(r, c).getTableCellRendererComponent(this, value, false, false, r, c);
        maxWidth = Math.max(maxWidth, comp.getPreferredSize().width);
      }
      getColumnModel().getColumn(c).setMinWidth(maxWidth + 4 + 2 * leftRightPad);
    }

    getTableHeader().setReorderingAllowed(false);
    getTableHeader().setResizingAllowed(true);
  }

  @Override
  public ShortcutCellRenderer getCellRenderer(int row, int column) {
    return new ShortcutCellRenderer();
  }

  @Override
  public ShortcutCellRenderer getDefaultRenderer(Class<?> columnClass) {
    return new ShortcutCellRenderer();
  }

  @Override
  public ShortcutTableModel getModel() {
    return shortcutTableModel;
  }

  private final class ShortcutKeyAdapter extends KeyAdapter {
    private final Set<Integer> standaloneKeyCodes = Set.of(KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT);
    private final Set<Integer> maskKeyCodes =
        Set.of(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_ALT, KeyEvent.VK_META);

    @Override
    public void keyPressed(KeyEvent e) {
      int modifiers = e.getModifiersEx();
      int code = e.getKeyCode();
      int selectedRow = getSelectedRow();

      if (selectedRow >= 0) {
        if ((code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) && modifiers == 0) {
          XAction rowXAction = shortcutTableModel.xactionForRow(selectedRow);
          XAction newXAction = rowXAction.withShortcut(null);
          doSwap(newXAction);
        } else if (!maskKeyCodes.contains(code)) {
          var enteredShortcut = new Shortcut(KeyStroke.getKeyStroke(code, modifiers));
          var rowXAction = shortcutTableModel.xactionForRow(selectedRow);
          var newXAction = rowXAction.withShortcut(enteredShortcut);

          if (modifiers == InputEvent.SHIFT_DOWN_MASK || modifiers == 0) {
            if (standaloneKeyCodes.contains(code)) {
              doSwap(newXAction);
            }
          } else {
            doSwap(newXAction);
          }
        }
      }
    }

    private void doSwap(XAction toSwapIn) {
      Shortcut shortcut = toSwapIn.shortcut();

      // Check for duplicate shortcuts
      if (shortcut != null) {
        Map<String, Shortcut> allShortcuts = userdb.retrieveAll();
        for (Shortcut existingShortcut : allShortcuts.values()) {
          if (shortcut.equals(existingShortcut)) {
            String msg = shortcut + " is already taken.";
            JOptionPane.showMessageDialog(ShortcutTable.this, msg, "Error", JOptionPane.OK_OPTION);
            return;
          }
        }
      }

      userdb.store(toSwapIn);
      repaint();
    }
  }

  private final class ShortcutTableModel implements TableModel {
    private final List<String> headers = List.of("Action", "Shortcut", "Default");
    private final String noShortcutRepr = "";

    public XAction xactionForRow(int row) {
      return defaultXActions[row];
    }

    @Override
    public int getRowCount() {
      return defaultXActions.length;
    }

    @Override
    public int getColumnCount() {
      return headers.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
      return headers.get(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (rowIndex >= defaultXActions.length
          || columnIndex >= headers.size()
          || rowIndex < 0
          || columnIndex < 0) {
        return null;
      }

      XAction defXAction = defaultXActions[rowIndex];
      String key = defXAction.getId();
      Map<String, Shortcut> map = userdb.retrieveAll();
      Shortcut currentShortcut = map.get(key);

      return switch (columnIndex) {
        case 0 -> defXAction.name();
        case 1 -> currentShortcut != null ? currentShortcut.toString() : noShortcutRepr;
        case 2 -> defXAction.shortcut() != null ? defXAction.shortcut().toString() : noShortcutRepr;
        default -> null;
      };
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      // Not editable
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
      // Not implemented for this simple model
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
      // Not implemented for this simple model
    }
  }

  private final class ShortcutCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      int selectedRow = table.getSelectedRow();

      // Set borders
      if (selectedRow == row) {
        setBorder(
            new CompoundBorder(
                UIManager.getBorder("Table.focusCellHighlightBorder"),
                BorderFactory.createEmptyBorder(0, leftRightPad, 0, leftRightPad)));
      } else {
        setBorder(
            new CompoundBorder(
                BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createEmptyBorder(0, leftRightPad, 0, leftRightPad)));
      }

      // Set background color
      if (selectedRow == row && column == 1) {
        setBackground(table.getSelectionBackground());
      } else {
        setBackground(table.getBackground());
        setForeground(Color.BLACK);
      }

      return this;
    }
  }
}
