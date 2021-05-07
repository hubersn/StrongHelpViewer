/*
 * (c) hubersn Software
 * www.hubersn.com
 */

package com.hubersn.riscos.util.fs;

/**
 * Class encapsulating RISC OS filesystem attributes like Locked, User Read/Write, Public Read/Write...
 */
public class Attributes {

  private int source;

  /**
   * Creates a new Attributes instance with default "all access" attributes.
   */
  public Attributes() {
    this.source = 0b110011;
  }

  /**
   * Creates a new Attributes instance based on given int (actually a byte).
   *
   * @param source source value for attributes.
   */
  public Attributes(final int source) {
    this.source = source;
  }

  public boolean isUserRead() {
    return (this.source & 1) != 0;
  }

  public boolean isUserWrite() {
    return (this.source & 2) != 0;
  }

  public boolean isLocked() {
    return (this.source & 8) != 0;
  }

  public boolean isPublicRead() {
    return (this.source & 16) != 0;
  }

  public boolean isPublicWrite() {
    return (this.source & 32) != 0;
  }

  public boolean isBitSet(final int bitNumber) {
    return (this.source & 1 << bitNumber) != 0;
  }
}
