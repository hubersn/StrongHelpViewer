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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JOptionPane;

import com.hubersn.riscos.stronghelp.content.SHFontConfig;
import com.hubersn.riscos.stronghelp.content.SHFontManager;
import com.hubersn.riscos.stronghelp.content.SHPage;
import com.hubersn.riscos.stronghelp.content.SHPageProviderIF;
import com.hubersn.riscos.stronghelp.imagefile.SHIDir;
import com.hubersn.riscos.stronghelp.imagefile.SHIEntry;
import com.hubersn.riscos.stronghelp.imagefile.SHIFile;
import com.hubersn.riscos.stronghelp.imagefile.SHILink;
import com.hubersn.riscos.stronghelp.view.PageFrame;
import com.hubersn.riscos.util.fs.LoadExec;
import com.hubersn.util.io.FileUtils;
import com.hubersn.util.memory.Memory;

/**
 * Encapsulation for one StrongHelp manual - parses the image fs content and
 * allows opening the !Root of the manual.
 */
public class StrongHelpManual implements SHPageProviderIF {

  private static final boolean showInSameFrame = false;

  private Memory strongHelpData;

  private SHIDir root;

  private PageFrame mainView;

  private StrongHelp mainHelpApplication;

  private SHFontManager fontManager;

  /**
   * Creates a new instance of StrongHelpReader, representing the given StrongHelp image file.
   * 
   * @param sourceFile source file for StrongHelp manual image.
   * @throws IOException on errors reading the file.
   */
  public StrongHelpManual(final File sourceFile) throws IOException {
    this(null, sourceFile);
  }

  /**
   * Creates a new instance of StrongHelpReader, representing the given StrongHelp image file.
   * 
   * @param mainHelpApplication reference to parent application, might be null.
   * @param sourceFile source file for StrongHelp manual image.
   * @throws IOException on errors reading the file.
   */
  public StrongHelpManual(final StrongHelp mainHelpApplication, final File sourceFile) throws IOException {
    this.mainHelpApplication = mainHelpApplication;
    this.strongHelpData = new Memory(FileUtils.load(sourceFile));
    final String sourceFileName = sourceFile.getAbsolutePath();
    if (!"HELP".equals(this.strongHelpData.getText(0, 4))) {
      // we might have a ZIP file instead
      if ("PK".equals(this.strongHelpData.getText(0, 2))) {
        this.strongHelpData = null;
        // try to read first element
        try (final ZipFile zipFile = new ZipFile(sourceFile, ZipFile.OPEN_READ, Charset.forName("WINDOWS-1252"))) {
          for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
            ZipEntry zipEntry = entries.nextElement();
            try (final InputStream zipEntryInputStream = zipFile.getInputStream(zipEntry)) {
              this.strongHelpData = new Memory(FileUtils.load(zipEntryInputStream));
              break;
            }
          }
        } catch (final Exception ex) {
          throw new IOException("Cannot read Zip file: " + sourceFileName);
        }
        if (this.strongHelpData == null) {
          throw new IOException("Cannot read Zip file: " + sourceFileName);
        }
      } else {
        throw new IOException("Not a StrongHelp file or a Zip file: " + sourceFileName);
      }
    }
    StrongHelp.d("Interpreting file "+sourceFile.getAbsolutePath());
    StrongHelp.dv("Size of root block", gw(4));
    StrongHelp.dv("Version", gw(8));
    StrongHelp.dv("Offset to first free block", gw(12));
    StrongHelp.dv("Offset to root dir", gw(16));
    LoadExec loadExec = new LoadExec(guw(20), guw(24));
    this.root = new SHIDir("HELP", "$", gw(16), loadExec);
    readDir(this.root);
    StrongHelp.dumpDirStructure(this.root, 0);
    initFontManager();
  }

  /**
   * Returns the root directory of the represented StrongHelp image file.
   * 
   * @return root directory.
   */
  public SHIDir getRoot() {
    return this.root;
  }

  /**
   * Returns the font manager for this StrongHelp image file.
   * 
   * @return font manager.
   */
  public SHFontManager getFontManager() {
    return this.fontManager;
  }

  /**
   * Returns the binary data from the given file in the context of this StrongHelp image.
   * 
   * @param file file from inside StrongHelp image.
   * @return binary data.
   */
  public byte[] getData(final SHIFile file) {
    return file.getData(this.strongHelpData);
  }

  private void initFontManager() {
    this.fontManager = new SHFontManager();
    this.fontManager.setStrongHelpConfig(StrongHelp.getGlobalFontConfig());
    // search for local !Configure
    SHIEntry entry = this.root.getEntry("!Configure");
    if (entry instanceof SHIFile) {
      SHFontConfig manualFontConfig = new SHFontConfig();
      manualFontConfig.readConfig(new ByteArrayInputStream(getData((SHIFile) entry)));
      this.fontManager.setManualConfig(manualFontConfig);
    }
  }

  /**
   * Shows the !Root page of the represented manual.
   */
  public void show() {
    if (this.mainView != null) {
      this.mainView.getFrame().setVisible(true);
      this.mainView.getFrame().toFront();
      return;
    }

    this.mainView = show("!Root");
  }

  /**
   * Shows the specified page of the represented manual.
   */
  public void showPage(final String pagePath) {
    showSHPage(pagePath);
  }

  @Override
  public SHPage showSHPage(final String pageName) {
    // called from a hyperlink that is not an internal link - check if it is a global link.
    if (pageName.startsWith("../")) {
      // ask parent to show, or if parent does not exist, show warning dialog
      if (this.mainHelpApplication == null) {
        JOptionPane.showMessageDialog(this.mainView.getFrame(),
                                      "Link to different manual cannot be followed.",
                                      "Error opening link",
                                      JOptionPane.INFORMATION_MESSAGE);
        return null;
      } else {
        final String possibleError = this.mainHelpApplication.showSHPage(pageName);
        if (possibleError != null) {
          // there might be a specific "notfound" page for such a case
          final String specialErrorPageName = "notfound_" + possibleError;
          // check if specific error page exists
          if (getSHPage(specialErrorPageName) == null) {
            // StrongHelp says "Manual is not installed" in such cases!
            JOptionPane.showMessageDialog(this.mainView.getFrame(),
                                          "Manual >" + possibleError + "< is not installed.",
                                          "Error opening page",
                                          JOptionPane.INFORMATION_MESSAGE);
          } else {
            show(specialErrorPageName);
          }
        }
        return null;
      }
    }
    SHPage page = getSHPage(pageName);
    if (page == null) {
      // TODO this always pulls the main frame to the foreground, not the frame where this error actually happened!
      // TODO why not use StrongHelp syntax to show an error page...
      JOptionPane.showMessageDialog(this.mainView.getFrame(),
                                    "Requested page data (!Root) not found: >" + pageName + "<",
                                    "Error opening page",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
    if (showInSameFrame) {
      // caller knows to update itself
      return page;
    }
    show(page);
    // signal caller that opening is already handled
    return null;
  }

  @Override
  public SHPage getSHPage(final String pageName) {
    // might be a dir or a file - if dir, use !Root
    //System.out.println("Looking for a page called >"+pageName+"<");
    SHIEntry entry = this.root.getEntry(pageName);
    if (entry == null) {
      // now do a deep search - it might be a prefixed/postfixed pageName
      entry = this.root.getGlobalEntry(pageName);
    }
    if (entry == null) {
      // not found, just trace, let caller handle
      StrongHelp.d("Requested page not found: >" + pageName + "<");
      return null;
    }
    SHIFile data;
    if (entry instanceof SHIDir) {
      data = (SHIFile) ((SHIDir)entry).getEntry("!Root");
    } else {
      data = (SHIFile)entry;
    }
    // search result might still fail
    if (data == null) {
      return null;
    }
    SHPage page = new SHPage(data.getData(this.strongHelpData), this, this.fontManager);
    return page;
  }

  private PageFrame show(final String filename) {
    SHIFile fileToShow = (SHIFile) this.root.getEntry(filename);
    SHPage pageToShow = new SHPage(fileToShow.getData(this.strongHelpData), this, this.fontManager);
    return show(pageToShow);
  }

  private PageFrame show(final SHPage page) {
    PageFrame pageFrame = new PageFrame(page, this);
    pageFrame.show();
    pageFrame.getFrame().setIconImage(StrongHelp.STRONGHELP);
    return pageFrame;
  }

  private void readDir(final SHIDir dir) throws IOException {
    try {
      String dirName = dir.getName();
      int offset = dir.getOffset();
      if (!"DIR$".equals(this.strongHelpData.getText(offset + 0, 4))) {
        throw new IOException("Not a StrongHelpDir at offset" + offset);
      }
      StrongHelp.dv("Parsing dir >" + dirName + "< from offset ", offset);
      // "size of dir" is only relevant for write info, for read the "size of dir used" is important
      final int size = gw(offset + 4);
      StrongHelp.dv("Size of Dir: ", size);
      final int sizeUsed = gw(offset + 8);
      StrongHelp.dv("Size of Dir used: ", sizeUsed);
      // skip dir header
      int entryStart = offset + 12;
      while (entryStart < offset + sizeUsed - 20) {
        LoadExec loadExec = new LoadExec(guw(entryStart + 4), guw(entryStart + 8));
        int objectOffset = gw(entryStart + 0);
        System.out.println("is legal object offset: "+objectOffset+": "+(objectOffset < this.strongHelpData.getData().length));
        int objectSize = gw(entryStart + 12);
        String entryName = gs(entryStart + 24);
        // calculate start of next entry
        entryStart += Memory.align(24 + entryName.length() + 1);
        // may be a special "link" file of size 0
        if (objectSize > 0) {
          String marker = this.strongHelpData.getText(objectOffset + 0, 4);
          SHIEntry entry;
          if ("DIR$".equals(marker)) {
            entry = new SHIDir(marker, entryName, objectOffset, loadExec);
            // recurse
            readDir((SHIDir) entry);
          } else if ("DATA".equals(marker)) {
            int dataObjectSize = this.strongHelpData.getWord(objectOffset + 4) - 8;
            entry = new SHIFile(marker, entryName, objectOffset, loadExec, dataObjectSize);
          } else if ("FREE".equals(marker)) {
            // FREE blocks are only for internal ImageFS processing, we just ignore them
            StrongHelp.d("FREE block found, ignoring.");
            continue;
          } else {
            System.err.println("Unknown entry encountered: marker=" + marker + ", name=" + entryName);
            continue;
          }
          dir.add(entry);
          StrongHelp.d("Entry found: ID=" + entry.getId() + ", name=" + entryName + " @dataoffset " + gw(entryStart) + ", next entry possibly from " + entryStart);
        } else {
          dir.add(new SHILink("LINK", entryName, -1, loadExec));
          StrongHelp.d("Link Entry found: " + entryName);
        } // end if
      } // end while
    } catch (final Exception ex) {
      System.err.println("Fatal data access error happened, continuing...");
      ex.printStackTrace();
    }
  }

  private int gw(final int offset) {
    return this.strongHelpData.getWord(offset);
  }

  private long guw(final int offset) {
    return this.strongHelpData.getUnsignedWord(offset);
  }

  private String gs(final int offset) {
    return this.strongHelpData.getText(offset);
  }
}
