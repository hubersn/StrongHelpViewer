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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import com.hubersn.riscos.stronghelp.content.SHPage;
import com.hubersn.riscos.stronghelp.imagefile.SHIDir;
import com.hubersn.riscos.stronghelp.imagefile.SHIEntry;
import com.hubersn.riscos.stronghelp.imagefile.SHIFile;

/**
 * Converts a given StrongHelp manual or a directory of StrongHelp manuals into
 * an HTML representation for viewing e.g. on a server in a browser.
 */
public class StrongHelpConverter {

  private static boolean debug = false;

  private static boolean verbose = false;

  private static void createFile(final File targetDirectory, final SHIFile fileEntry, final StrongHelpManual shr) throws Exception {
    SHPage page = null;
    String targetFileName = "";
    byte[] data = null;
    data = shr.getData(fileEntry);
    // may be an unsupported filetype
    final String filetype = fileEntry.getLoadExec().getFiletypeAsString();
    if ("AFF".equals(filetype) || "FF9".equals(filetype) || "FCA".equals(filetype)) {
      targetFileName = fileEntry.getName().toLowerCase() + "," + filetype.toLowerCase();
      final File targetRawFile = new File(targetDirectory, targetFileName);
      if (targetRawFile.exists()) {
        System.err.println("Error: duplicate file " + targetRawFile.getName());
        return;
      }
      try (FileOutputStream fos = new FileOutputStream(targetRawFile)) {
        fos.write(data);
        fos.close();
        verbose("Created Raw source file " + targetRawFile.getAbsolutePath());
        return;
      }
    }
    page = new SHPage(data, shr, shr.getFontManager());
    targetFileName = fileEntry.getName().toLowerCase();
    final File targetHtmlFile = new File(targetDirectory, targetFileName + ".html");
    if (targetHtmlFile.exists()) {
      System.err.println("Error: duplicate file " + targetHtmlFile.getName());
      return;
    }
    try (FileOutputStream fos = new FileOutputStream(targetHtmlFile)) {
      fos.write(new String(page.getBodyAsHTML()).getBytes("WINDOWS-1252"));
      fos.close();
      verbose("Created file " + targetHtmlFile.getAbsolutePath());
      // duplicate !root as index.html
      if ("!root".equals(targetFileName)) {
        Files.copy(targetHtmlFile.toPath(), new File(targetDirectory, "index.html").toPath());
        verbose("Duplicated !root file as index.html");
      }
    } catch (final Exception ex) {
      System.err.println("Error creating file "+targetHtmlFile+" - continuing...stacktrace:");
      ex.printStackTrace();
    }
    if (debug) {
      final File targetSHFile = new File(targetDirectory, targetFileName + ".strong");
      try (FileOutputStream fos = new FileOutputStream(targetSHFile)) {
        fos.write(data);
        fos.close();
        verbose("Created SH source file " + targetSHFile.getAbsolutePath());
      }
    }
  }

  private static void createDirStructure(final File targetDirectory, final SHIDir dir, final StrongHelpManual shr) throws Exception {
    for (final SHIEntry entry : dir.getEntries()) {
      if (entry instanceof SHIDir) {
        File newDir = new File(targetDirectory, entry.getName().toLowerCase());
        newDir.mkdir();
        verbose("Created directory " + newDir.getAbsolutePath());
        createDirStructure(newDir, (SHIDir) entry, shr);
      } else {
        createFile(targetDirectory, (SHIFile) entry, shr);
      }
    }
  }

  private static void verbose(final String out) {
    if (verbose) {
      System.out.println(out);
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    if (args == null || args.length == 0) {
      printUsage();
      System.exit(0);
    }
    // filename that resolves to a file - converts single manual to HTML files inside
    // filename that resolves to a dir - converts all manuals inside dir to HTML
    debug = false;
    verbose = false;
    String targetDirName;
    String sourceFileOrDirName;
    File targetDir = null;
    File sourceFileOrDir = null;
    try {
      // parse CLI arguments
      for (int i = 0; i < args.length; i++) {
        final String arg = args[i];
        if ("-v".equalsIgnoreCase(arg)) {
          verbose = true;
        }
        if ("-debug".equalsIgnoreCase(arg)) {
          debug = true;
        }
        if ("-help".equalsIgnoreCase(arg) || "-?".equalsIgnoreCase(arg)) {
          printUsage();
          System.exit(0);
        }
        if ("-target".equalsIgnoreCase(arg)) {
          i++;
          targetDirName = args[i];
          targetDir = new File(targetDirName);
          if (targetDir.exists()) {
            if (!targetDir.isDirectory()) {
              error("Specified target directory already exists as a file: " + targetDirName);
            } else if (!targetDir.canWrite()) {
              error("Cannot write to specified target directory " + targetDirName);
            }
          }
          if (!targetDir.exists()) {
            boolean success = targetDir.mkdirs();
            if (!success) {
              error("Cannot create specified target directory " + targetDirName);
            }
          }
        }
        if (i == args.length - 1) {
          sourceFileOrDirName = arg;
          sourceFileOrDir = new File(sourceFileOrDirName);
          if (!sourceFileOrDir.exists() || (!sourceFileOrDir.isDirectory() && !sourceFileOrDir.isFile()) || !sourceFileOrDir.canRead()) {
            error("Cannot read specified manual file or directory " + sourceFileOrDir);
          }
        }
      }

      // check command parse result, error if incomplete
      if (targetDir == null) {
        error("Error: target directory not specified.");
      }
      if (sourceFileOrDir == null) {
        error("Error:source not specified.");
      }

      // now convert
      if (sourceFileOrDir.isFile()) {
        StrongHelpManual shr = new StrongHelpManual(sourceFileOrDir);
        verbose("Starting conversion of StrongHelp manual file " + sourceFileOrDir.getAbsolutePath());
        verbose("");
        createDirStructure(targetDir, shr.getRoot(), shr);
      } else if (sourceFileOrDir.isDirectory()) {
        verbose("Starting conversion of StrongHelp manual files in " + sourceFileOrDir.getAbsolutePath());
        verbose("");
        File[] allManuals = sourceFileOrDir.listFiles();
        if (allManuals == null || allManuals.length == 0) {
          error("No files found in source directory.");
        }
        int strongHelpFileCount = 0;
        for (final File f : allManuals) {
          try {
            StrongHelpManual shr = new StrongHelpManual(f);
            verbose("Starting conversion of StrongHelp manual file " + f.getAbsolutePath());
            verbose("");
            final File manualTargetDir = new File(targetDir, StrongHelp.getManualName(f));
            manualTargetDir.mkdir();
            createDirStructure(manualTargetDir, shr.getRoot(), shr);
          } catch (final IOException iox) {
            System.err.println(iox.getMessage());
          }
        }
        if (strongHelpFileCount == 0) {
          error("No StrongHelp files found in source directory.");
        }
      }
      verbose("");
      verbose("Conversion successful!");
    } catch (final Exception ex) {
      ex.printStackTrace();
      error(ex.getMessage());
    }
  }

  private static void error(final String errorMessage) {
    System.err.println(errorMessage);
    System.exit(1);
  }

  private static void printUsage() {
    System.out.println("Usage: StrongHelpConverter [-v] [-debug] -target <target directory> <source file or directory>");
    System.out.println("Options:");
    System.out.println("  -v      verbose console output");
    System.out.println("  -debug  also put source StrongHelp files into target dir");
    System.out.println("Examples:");
    System.out.println("  Open filechooser to choose manual to show: StrongHelp");
    System.out.println("  Produce this output: StrongHelpConverter -help");
    System.out.println("  Produce this output: StrongHelpConverter -?");
  }
}
