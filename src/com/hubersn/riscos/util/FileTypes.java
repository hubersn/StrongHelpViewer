/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.riscos.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hubersn.util.string.StringUtils;

/**
 * Managing class for filetype/text translations. Reads a text file filetypes.txt from classpath based on a ROOL wiki
 * page and provides a mapping of a filetype to a descriptive text for this filetype.
 */
public class FileTypes {

  private static Map<String, String> filetypeMap = new HashMap<>();

  static {
    clearMapping();
    readFileTypesMapping();
  }

  public static String getFileTypeText(final String fileType) {
    if (fileType == null) {
      return "null";
    }
    String fileTypeText = filetypeMap.get(fileType.toUpperCase());
    if (fileTypeText == null) {
      return "unknown";
    }
    return fileTypeText;
  }

  public static String getFileTypeText(final int fileType) {
    if (fileType < 0) {
      return "null";
    }
    String fileTypeText = filetypeMap.get(StringUtils.valueToHexChars(fileType, 3, true));
    if (fileTypeText == null) {
      return "unknown";
    }
    return fileTypeText;
  }

  public static void clearMapping() {
    filetypeMap = Collections.synchronizedMap(new HashMap<String,String>());
  }

  /**
   * Reads a filetype mapping file, mapping filetypes to descriptive text - duplicates allowed, first entry wins.
   *
   * @param filepath path to filetype mapping file.
   * @param encoding encoding of the filetype mapping file.
   * @throws IOException on error (e.g. file not found, broken content...)
   */
  public static void readFileTypesMapping(final Path filepath, final String encoding) throws IOException {
    List<String> allLines = Files.readAllLines(filepath, Charset.forName(encoding));
    for (final String line : allLines) {
      if (line.startsWith("#")) {
        continue;
      }
      final String key = line.substring(3, 6);
      // never override existing keys
      if (!filetypeMap.containsKey(key)) {
        filetypeMap.put(key, line.substring(7, line.indexOf('|', 7)));
      }
    }
  }

  // TODO is exception catch/fallback really necessary?
  private static void readFileTypesMapping() {
    try {
      readFileTypesMapping(Paths.get(FileTypes.class.getResource("/filetypes.txt").toURI()), "UTF-8");
    } catch (Exception ex) {
      ex.printStackTrace();
      if (!filetypeMap.containsKey("FFF")) {
        filetypeMap.put("FFF", "Text");
      }
    }
  }

  private FileTypes() {
    // no instances allowed
  }

}