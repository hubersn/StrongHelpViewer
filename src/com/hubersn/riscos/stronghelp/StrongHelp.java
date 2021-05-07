/*
 * (c) hubersn Software
 * www.hubersn.com
 */

/*
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <http://unlicense.org/>
*/

package com.hubersn.riscos.stronghelp;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.hubersn.riscos.stronghelp.content.SHFontConfig;
import com.hubersn.riscos.stronghelp.content.SHFontManager;
import com.hubersn.riscos.stronghelp.content.SHPage;
import com.hubersn.riscos.stronghelp.imagefile.SHIDir;
import com.hubersn.riscos.stronghelp.imagefile.SHIEntry;
import com.hubersn.riscos.stronghelp.view.PagePanel;
import com.hubersn.ui.swing.lookandfeel.LookAndFeelUtils;
import com.hubersn.ui.swing.util.SwingUtils;
import com.hubersn.util.io.FileUtils;
import com.hubersn.util.string.StringUtils;

/**
 * Java Swing-based reimplementation of StrongHelp, a great piece of software
 * running on RISC OS originally created by Guttorm Vik, now maintained by
 * Fred Graute - see http://www.stronged.iconbar.com/fjg/
 */
public class StrongHelp {

  /** StrongHelp application icon. */
  public static final Image STRONGHELP;

  /** StrongHelp manual icon. */
  public static final Image STRONGHELP_MANUAL;

  private static final String APP_NAME = "StrongHelpViewer";

  private static boolean debug = false;

  private static SHFontConfig globalFontConfig;

  private String fileOrDirectoryNameFromMain;

  private String manualsDirectoryNameFromMain;

  private Map<File, StrongHelpManual> manualCache = new HashMap<>();

  private Map<String, File> allManuals = new HashMap<>();

  private JFrame mainManualsFrame;

  static {
    STRONGHELP = new ImageIcon(StrongHelp.class.getResource("/sh_16x16.png")).getImage();
    STRONGHELP_MANUAL = new ImageIcon(StrongHelp.class.getResource("/m_32x32.png")).getImage();
  }

  /**
   * Returns the currently active global font configuration as parsed from file /manuals/!Configure.
   * 
   * @return
   */
  public static SHFontConfig getGlobalFontConfig() {
    if (globalFontConfig == null) {
      globalFontConfig = new SHFontConfig();
      try (final InputStream mainConfigure = StrongHelpManual.class.getResourceAsStream("/manuals/!Configure")) {
        globalFontConfig.readConfig(mainConfigure);
      } catch (IOException e) {
        // non-essential style config
        //if someone ruined our packaging, there is nothing we can do - trace and continue
        System.err.println("Failed to read main !Configure - continuing.");
        e.printStackTrace();
      }
    }
    return globalFontConfig;
  }

  private void show() {
    if (this.fileOrDirectoryNameFromMain == null && this.manualsDirectoryNameFromMain == null) {
      showFileChooser();
    } else {
      if (this.fileOrDirectoryNameFromMain != null) {
        File f = new File(this.fileOrDirectoryNameFromMain);
        if (f.isFile()) {
          showFile(f);
        } else if (f.isDirectory()) {
          showManualDirectory(f.listFiles());
        }
      }
    }
  }

  private void showFile(final File file) {
    try {
      StrongHelpManual shr = this.manualCache.get(file);
      if (shr == null) {
        shr = new StrongHelpManual(this, file);
        this.manualCache.put(file, shr);
      }
      shr.show();
    } catch (final IOException iox) {
      // just trace...
      iox.printStackTrace();
    }
  }

  private void showFile(final File file, final String pagePath) {
    try {
      StrongHelpManual shr = this.manualCache.get(file);
      if (shr == null) {
        shr = new StrongHelpManual(this, file);
        this.manualCache.put(file, shr);
      }
      shr.showPage(pagePath);
    } catch (final IOException iox) {
      // just trace...
      iox.printStackTrace();
    }
  }

  String showSHPage(final String pagePath) {
    String manualPath = pagePath;
    // strip relative prefix if it exists (always???)
    if (manualPath.startsWith("../")) {
      manualPath = pagePath.substring("../".length());
    }
    // check manuals if target exists
    final int slashPosition = manualPath.indexOf('/');
    if (slashPosition > 0) {
      final String manualName = manualPath.substring(0, slashPosition);
      final File manualFile = this.allManuals.get(manualName);
      if (manualFile != null) {
        showFile(manualFile, manualPath.substring(slashPosition + 1));
        return null;
      } else {
        return manualName;
//        JOptionPane.showMessageDialog(this.mainManualsFrame,
//                                      "Error: manual " + manualName + " not found.",
//                                      "Error",
//                                      JOptionPane.ERROR_MESSAGE);
      }
    }
    // TODO is this an error to signal to caller?
    return null;
  }

  private JComponent createShowManualButton(final String s, final File file, final Image img) {
    final JLabel btn = new JLabel(s, new ImageIcon(img), SwingConstants.CENTER);
    btn.setHorizontalTextPosition(SwingConstants.CENTER);
    btn.setVerticalTextPosition(SwingConstants.BOTTOM);
    btn.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent mev) {
        showFile(file);
      }
    });
    return btn;
  }

  private void showFileChooser() {
    final JFileChooser jfc = new JFileChooser();
    jfc.setDialogTitle("Choose manual directory or one or more manual files");
    jfc.setMultiSelectionEnabled(true);
    jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    int result = jfc.showOpenDialog(null);
    if (result == JFileChooser.APPROVE_OPTION) {
      File[] chosenFiles = jfc.getSelectedFiles();
      for (final File file : chosenFiles) {
        List<File> filesForManualDir = new ArrayList<>();
        if (file.isFile()) {
          showFile(file);
        } else if (file.isDirectory()) {
          for (final File fileToAdd : file.listFiles()) {
            filesForManualDir.add(fileToAdd);
          }
        }
        showManualDirectory(filesForManualDir.toArray(new File[0]));
      }
    } else {
      System.exit(0);
    }
  }

  /**
   * Returns the manual name from the name of the given file.
   * 
   * @param sourceFile manual source file.
   * @return name of manual (based on file name).
   */
  public static String getManualName(final File sourceFile) {
    final String manualName = sourceFile.getName();
    if (manualName.indexOf(',') > 0) {
      return manualName.substring(0, manualName.indexOf(','));
    }
    return manualName;
  }

  private void showManualDirectory(final File[] files) {
    if (files == null || files.length == 0) {
      return;
    }

    this.mainManualsFrame = new JFrame("StrongHelpViewer Main Menu");
    final JFrame f = this.mainManualsFrame;
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setIconImage(STRONGHELP);

    final JMenuBar menuBar = new JMenuBar();
    final Action openAction = new AbstractAction("Open...") {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent e) {
        showFileChooser();
      }
    };
    final Action exitAction = new AbstractAction("Exit") {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent e) {
        System.exit(0);
      }
    };
    final JMenu fileMenu = new JMenu("File");
    fileMenu.add(openAction);
    fileMenu.addSeparator();
    fileMenu.add(exitAction);

    final Action actionShowReadMe = new AbstractAction("Show ReadMe...") {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent e) {
        try (InputStream is = StrongHelp.class.getResourceAsStream("/ReadMe.txt")) {
          final JDialog d = new JDialog(f, false);
          d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
          d.setTitle(APP_NAME + " - ReadMe");
          final SHFontManager localFontManager = new SHFontManager();
          localFontManager.setStrongHelpConfig(getGlobalFontConfig());
          // we known that our ReadMe does not contain any manual links, or even
          // #include commands, so "null" for page provider is OK.
          final PagePanel pp = new PagePanel(new SHPage(FileUtils.load(is),
                                        null,
                                        localFontManager),
                             null);
          d.setContentPane(pp.getStrongHelpView());
          d.pack();
          d.setVisible(true);
        } catch (final IOException e1) {
          // someone has ruined our packaging...
          e1.printStackTrace();
        }
      }
    };
    final Action actionAbout = new AbstractAction("About...") {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent e) {
        final JOptionPane pane;
        final String text = "<html><body><h1 align=center>" + APP_NAME + "</h1>"
                            + "<br><p align=center>" + Version.getVersionString() + "</p>"
                            + "<p align=center>Copyright 2021 hubersn Software</p>"
                            + "<p align=center><a href=\"https://www.hubersn-software.com/\">https://www.hubersn-software.com/</a></p>"
                            + "<br><br></body></html>";
        pane = new JOptionPane(text, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, new ImageIcon(STRONGHELP));
        final JDialog dialog = pane.createDialog(f, "About " + APP_NAME);
        SwingUtils.addMouseListenerToAllLabels(dialog.getContentPane(), new MouseAdapter() {
          @Override
          public void mouseReleased(MouseEvent ev) {
            SwingUtils.launchURL("https://www.hubersn-software.com/");
          }
        });
        dialog.setVisible(true);
      }
    };
    final JMenu helpMenu = new JMenu("Help");
    helpMenu.add(actionShowReadMe);
    helpMenu.addSeparator();
    helpMenu.add(actionAbout);

    menuBar.add(fileMenu);
    menuBar.add(helpMenu);
    f.setJMenuBar(menuBar);

    final JPanel cp = new JPanel(new BorderLayout());
    final JLabel titleLabel = new JLabel("StrongHelp Manuals");
    titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD + Font.ITALIC, 24));
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
    cp.add(titleLabel, BorderLayout.NORTH);
    final JPanel manualPanel = new JPanel(new GridLayout(0, 5, 32, 8));
    final int offsets = 10;
    manualPanel.setBorder(BorderFactory.createEmptyBorder(offsets, offsets, offsets, offsets));
    cp.add(new JScrollPane(manualPanel), BorderLayout.CENTER);
    // add, for every manual file, a button
    for (final File file : files) {
      if (file.isFile() && file.canRead()) {
        final String manualName = getManualName(file);
        // always lower-case for RISC OS like file matching
        this.allManuals.put(manualName.toLowerCase(), file);
        manualPanel.add(createShowManualButton(manualName, file, STRONGHELP_MANUAL));
      }
    }
    f.setContentPane(cp);
    f.pack();
    f.setVisible(true);
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    // depending on caller arguments, the following will happen:
    // no arguments - FileChooser is opened, user can choose StrongHelp files or a manual dir
    // filename that resolves to a file - open that single file in viewer
    // filename that resolves to a dir - show all files in view, ready to be opened
    final StrongHelp strongHelp = new StrongHelp();
    for (int i = 0; i < args.length; i++) {
      final String arg = args[i];
      if ("-v".equalsIgnoreCase(arg)) {
        strongHelp.debug = true;
      }
      if ("-help".equalsIgnoreCase(arg) || "-?".equals(arg)) {
        printUsage();
        System.exit(0);
      }
      if ("-manualdir".equalsIgnoreCase(arg)) {
        i++;
        strongHelp.manualsDirectoryNameFromMain = args[i];
        File check = new File(args[i]);
        if (!check.exists() || !check.isDirectory() || !check.canRead()) {
          error("Cannot read specified manuals directory.");
        }
      }
      if (i == args.length - 1) {
        strongHelp.fileOrDirectoryNameFromMain = args[i];
        File check = new File(args[i]);
        if (!check.exists() || (!check.isDirectory() && !check.isFile()) || !check.canRead()) {
          error("Cannot read specified file or directory.");
        }
      }
    }
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        LookAndFeelUtils.setSystemLookAndFeel();
        //LookAndFeelUtils.setLookAndFeel(LookAndFeelUtils.METAL_LOOK_AND_FEEL);
        //LookAndFeelUtils.setLookAndFeel(LookAndFeelUtils.WINDOWS_LOOK_AND_FEEL);
        //LookAndFeelUtils.setLookAndFeel(LookAndFeelUtils.NIMBUS_LOOK_AND_FEEL);
        //LookAndFeelUtils.setLookAndFeel(LookAndFeelUtils.GTK_LOOK_AND_FEEL);
        //LookAndFeelUtils.setLookAndFeel(LookAndFeelUtils.MOTIF_LOOK_AND_FEEL);
        //LookAndFeelUtils.setMetalTheme(LookAndFeelUtils.STEEL_METAL_THEME);
        //LookAndFeelUtils.setMetalTheme(LookAndFeelUtils.OCEAN_METAL_THEME);
        strongHelp.show();
      }
    });
  }

  private static void error(final String errorMessage) {
    System.err.println(errorMessage);
    System.exit(1);
  }

  private static void printUsage() {
    System.out.println("Usage: StrongHelp [-v] [-manualdir <directory with all known manuals inside>] [<file or directory pathname>]");
    System.out.println("Options:");
    System.out.println("  -v                activate debug/verbose mode");
    System.out.println("  -manualdir <path> adds given path to manual search path");
    System.out.println("Examples:");
    System.out.println("  Open filechooser to choose manual to show: StrongHelp");
    System.out.println("  Produce this output: StrongHelp -help");
  }

  // debug output

  /** debug value to sysout */
  public static void dv(final String s, final int value) {
    d(s + ": >" + value + "<");
  }

  /** debug to sysout if global debug flag is set. */
  public static void d(final String s) {
    if (debug) {
      System.out.println(s);
    }
  }

  /** debug to sysout with given indent if global debug flag is set. */
  public static void d(final String s, final int indent) {
    d(StringUtils.indent(s, indent));
  }

  public static void dumpDirStructure(final SHIDir dir, final int indent) {
    d("Dir " + dir, indent);
    for (final SHIEntry entry : dir.getEntries()) {
      if (entry instanceof SHIDir) {
        dumpDirStructure((SHIDir) entry, indent + 2);
      } else {
        d("File " + entry, indent + 2);
      }
    }
  }

}
