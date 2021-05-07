/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.riscos.util;

import java.util.Calendar;
import java.util.Date;

import com.hubersn.riscos.util.fs.LoadExec;

/**
 * Collection of static utility methods to handle conversion of Acorn RISC OS 5 byte centiseconds-since-1900-01-01 date stamp to Java Date.
 */
public class DateStamp {

  // Acorn date stamps are "centiseconds since 1900-01-01", store this start time as milliseconds
  private static final long ACORN_START_TIME;

  static {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.YEAR, 1900);
    c.set(Calendar.MONTH, Calendar.JANUARY);
    c.set(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    ACORN_START_TIME = c.getTimeInMillis();
  }

  public static Date getDate(final LoadExec loadExec) {
    return getDate(loadExec.getLoadAddr(), loadExec.getExecAddr());
  }

  public static Date getDate(final long loadAddr, final long execAddr) {
    long newTime = ACORN_START_TIME;
    newTime += execAddr * 10;
    newTime += ((loadAddr & 0xFF) << 32) * 10;
    return new Date(newTime);
  }

  public static Date getDate(final long rotimestamp) {
    long newTime = ACORN_START_TIME;
    newTime += rotimestamp * 10;
    return new Date(newTime);
  }

  public static Date getDate(final byte[] data) {
    return getDate(data, 0);
  }

  public static Date getDate(final byte[] data, final int offset) {
    long newTime = ACORN_START_TIME;
    long rotimestamp = 0;
    rotimestamp |= ((long) data[0 + offset]) << 32;
    rotimestamp |= data[1 + offset] << 24;
    rotimestamp |= data[2 + offset] << 16;
    rotimestamp |= data[3 + offset] << 8;
    rotimestamp |= data[4 + offset];
    newTime += rotimestamp * 10;
    return new Date(newTime);
  }

  public static LoadExec getLoadExec(final Date date) {
    byte[] sourceForLoadExec = new byte[8];
    long javatime = date.getTime();
    long rotime = (javatime - ACORN_START_TIME) / 10; // centiseconds in RO time
    sourceForLoadExec[7] = 0;
    sourceForLoadExec[6] = 0;
    sourceForLoadExec[5] = 0;
    sourceForLoadExec[4] = (byte)((rotime & 0xFF00000000L) >> 32);
    sourceForLoadExec[3] = (byte)((rotime & 0xFF000000L) >> 24);
    sourceForLoadExec[2] = (byte)((rotime & 0xFF0000L) >> 16);
    sourceForLoadExec[1] = (byte)((rotime & 0xFF00L) >> 8);
    sourceForLoadExec[0] = (byte)(rotime & 0xFFL);
    return new LoadExec(sourceForLoadExec);
  }

  private DateStamp() {
    // no instances allowed!
    super();
  }
}
