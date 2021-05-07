/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.riscos.util.fs;

import java.util.Date;

import com.hubersn.riscos.util.DateStamp;
import com.hubersn.util.string.StringUtils;

/**
 * Encapsulation for Load/Exec address pair (file attributes - either true Load/Exec addresses, or filetype/datestamp).
 */
public class LoadExec {

  private long loadAddr = 0;

  private long execAddr = 0;

  /**
   * Creates a new LoadExec instance based on given load address and exec address.
   *
   * @param loadAddr load address.
   * @param execAddr exec address.
   */
  public LoadExec(final long loadAddr, final long execAddr) {
    this.loadAddr = loadAddr;
    this.execAddr = execAddr;
  }

  /**
   * Creates a new LoadExec instance based on given data (first 8 bytes of byte array).
   *
   * @param data data.
   */
  public LoadExec(final byte[] data) {
    this(data, 0);
  }

  /**
   * Creates a new LoadExec instance based on given data (8 bytes of byte array starting with given offset).
   *
   * @param data data.
   * @param offset offset into given data.
   */
  public LoadExec(final byte[] data, final int offset) {
    this.loadAddr |= ((data[3 + offset] & 0xFF) << 24);
    this.loadAddr |= ((data[2 + offset] & 0xFF) << 16);
    this.loadAddr |= ((data[1 + offset] & 0xFF) << 8);
    this.loadAddr |= (data[0 + offset] & 0xFF);
    this.execAddr |= ((data[7 + offset] & 0xFF) << 24);
    this.execAddr |= ((data[6 + offset] & 0xFF) << 16);
    this.execAddr |= ((data[5 + offset] & 0xFF) << 8);
    this.execAddr |= (data[4 + offset] & 0xFF);
  }

  /**
   * Creates a new LoadExec instance based on fileType, dateStamp and attributes.
   *
   * @param fileType file type.
   * @param dateStamp date stamp.
   * @param attributes attributes.
   */
  public LoadExec(final FileType fileType, final Date dateStamp, final Attributes attributes) {

  }

  /**
   * Returns the load address.
   *
   * @return load address.
   */
  public long getLoadAddr() {
    return this.loadAddr;
  }

  /**
   * Returns the exec address.
   *
   * @return exec address.
   */
  public long getExecAddr() {
    return this.execAddr;
  }

  /**
   * Is the load/exec address data in fact a filetype/datestamp?
   *
   * @return filetype/datestamp data?
   */
  public boolean isFiletypeDatestamp() {
    return ((this.loadAddr >>> 20) & 0xFFF) == 0xFFF;
  }

  /**
   * Returns the filetype represented by this load/exec address pair.
   *
   * @return filetype.
   */
  public int getFiletype() {
    return (int) ((this.loadAddr & 0xFFF00) >> 8);
  }

  public void setFiletype(int filetype) {
    this.loadAddr = 0xFFF00000L | (filetype << 8) | (this.loadAddr & 0xFFL);
  }

  /**
   * Convenience method to return the filetype as a three-character hex representation in upper case.
   *
   * @return three hex characters representing filetype.
   */
  public String getFiletypeAsString() {
    return StringUtils.valueToHexChars(getFiletype(), 3, true);
  }

  /**
   * Returns the 5-byte-RISC OS datestamp (centiseconds since 1900-01-01 00:00:00.000) represented
   * by this load/exec address pair.
   *
   * @return datestamp.
   */
  public long getDateStamp() {
    long dateStamp = 0;
    dateStamp |= this.execAddr;
    dateStamp |= (this.loadAddr & 0xFFL) << 32;
    return dateStamp;
  }

  /**
   * Returns the RISC OS datestamp as a Java Date object.
   *
   * @return datestamp.
   */
  public Date getJavaDateStamp() {
    return DateStamp.getDate(getDateStamp());
  }

  @Override
  public String toString() {
    if (isFiletypeDatestamp()) {
      return getFiletypeAsString() + "|" + getJavaDateStamp();
    }
    return StringUtils.valueToHexChars(this.loadAddr, 8, true) + "/"
         + StringUtils.valueToHexChars(this.execAddr, 8, true);
  }
}