/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.riscos.util.fs;

import com.hubersn.riscos.util.FileTypes;
import com.hubersn.util.string.StringUtils;

/**
 * Encapsulation of a RISC OS filetype, reuses the static things from com.hubersn.riscos.util.FileTypes but provide them in an object
 * context.
 */
public class FileType {

  private final String fileTypeStr;

  private final int fileType;

  /**
   * Creates a new instance of FileType.
   */
  public FileType(int fileType) {
    if (fileType < 0 || fileType > 0x1000) {
      throw new IllegalArgumentException("FileType must be within range 0..0x1000.");
    }
    this.fileTypeStr = StringUtils.valueToHexChars(fileType, 3, true);
    this.fileType = fileType;
  }

  /**
   * Creates a new instance of FileType based on given String representation which must contain exactly three hex characters. String will be
   * normalised to upper case.
   *
   * @param fileType 3 hex characters representing this FileType.
   */
  public FileType(final String fileType) {
    if (fileType == null || fileType.length() != 3) {
      throw new IllegalArgumentException("String representing FileType must contain exactly three hex characters. Argument: >" + fileType + "<");
    }
    this.fileTypeStr = fileType.toUpperCase();
    for (int i = 0; i < this.fileTypeStr.length(); i++) {
      final char toCheck = this.fileTypeStr.charAt(i);
      StringUtils.hexCharToValue(toCheck); // will throw IllegalArgumentException if no hex char!
    }
    this.fileType = getFileType();
  }

  /**
   * Returns the associated descriptive text for our FileType via com.hubersn.riscos.util.FileTypes class.
   *
   * @return associated description for this FileType.
   */
  public String getFileTypeText() {
    return FileTypes.getFileTypeText(this.fileTypeStr);
  }

  /**
   * Returns the three character upper case hex string representing this FileType.

   * @return three character upper case hex string representing this FileType.
   */
  public String getFileTypeStr() {
    return this.fileTypeStr;
  }

  /**
   * Returns the encapsulated filetype as a number.
   *
   * @return
   */
  public int getFileType() {
    int filetype = 0;
    for (int i = 0; i < 3; i++) {
      final char toCheck = this.fileTypeStr.charAt(i);
      int nibbleValue = StringUtils.hexCharToValue(toCheck);
      filetype += (nibbleValue << (2 - i));
    }
    return filetype;
  }

  public boolean isDirType() {
    return this.fileType == 0x1000;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.fileTypeStr == null) ? 0 : this.fileTypeStr.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    FileType other = (FileType) obj;
    if (this.fileTypeStr == null) {
      if (other.fileTypeStr != null) return false;
    } else if (!this.fileTypeStr.equals(other.fileTypeStr)) return false;
    return true;
  }

}
