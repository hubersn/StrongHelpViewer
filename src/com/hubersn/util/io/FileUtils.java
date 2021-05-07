/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.util.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple utility methods working on whole files. Not optimised nor tested for large files!
 */
public class FileUtils {

  private FileUtils() {
    // only statics
  }

  public static byte[] load(final File file) throws IOException {
    return Files.readAllBytes(file.toPath());
  }

  public static byte[] loadViaStream(final File file) throws IOException {
    final FileInputStream fis = new FileInputStream(file);
    final BufferedInputStream bis = new BufferedInputStream(fis);
    try {
      return FileUtils.load(bis, (int) file.length());
    } finally {
      bis.close();
    }
  }

  public static byte[] load(final InputStream is) throws IOException {
    return load(is, 0);
  }

  public static byte[] load(final InputStream is, int bufferSize) throws IOException {
    final int DEFAULT_BUFFER_SIZE = 1024 * 1024;
    if (bufferSize <= 0) {
      bufferSize = DEFAULT_BUFFER_SIZE;
    }
    byte[] tmpData = new byte[bufferSize];
    int offs = 0;
    int addOn = DEFAULT_BUFFER_SIZE * 2;

    try {
      do {
        final int readLen = is.read(tmpData, offs, tmpData.length - offs);
        if (readLen == -1) {
          break;
        }
        offs += readLen;
        if (offs == tmpData.length) {
          final byte[] newres = new byte[tmpData.length + addOn];
          if (addOn < 1048576) {
            addOn = addOn * 2;
          }
          System.arraycopy(tmpData, 0, newres, 0, tmpData.length);
          tmpData = newres;
        }
      } while (true);
    } finally {
      is.close();
    }

    final byte[] data = new byte[offs];
    System.arraycopy(tmpData, 0, data, 0, offs);
    return data;
  }

  public static String[] readTextLines(final File file) throws IOException {
    return readTextLines(file, null);
  }

  public static String[] readTextLines(final File file, final String encoding) throws IOException {
    final FileInputStream fis = new FileInputStream(file);
    final List<String> lines = new ArrayList<>();
    try (final BufferedReader br = new BufferedReader(file == null ? new InputStreamReader(fis)
                                                                   : new InputStreamReader(fis, encoding))) {
      String line = null;
      while ((line = br.readLine()) != null) {
        lines.add(line);
      }
    }
    return lines.toArray(new String[0]);
  }
}
