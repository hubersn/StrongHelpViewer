/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.util.memory;

import java.io.Serializable;

/**
 * Simple memory access wrapper for signed and unsigned values based on byte array data. All non-specific functions assume little-endian
 * data, big-endian is postfixed "Big".
 */
public class Memory implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Extends the given source value with the given bit range to the given target bit range, so that the values are evenly distributed in the
   * target numerical range. E.g. 0b1111 as a 4 bit value becomes 0b11111111 as an 8bit value, 0b10101 as a 5 bit value becomes 0b10101101.
   * TODO incomplete, non- optimal results if targetNumberOfBits > 2* sourceNumberOfBits.
   *
   * @param source source value.
   * @param sourceNumberOfBits number of relevant bits in the source value.
   * @param targetNumberOfBits number of bits to expand the source value to.
   * @return extended value.
   */
  public static int extend(final int source, final int sourceNumberOfBits, final int targetNumberOfBits) {
    final int result = (source << sourceNumberOfBits) + source;
    final int resultBits = sourceNumberOfBits * 2;
    if (resultBits > targetNumberOfBits) {
      return result >>> (resultBits - targetNumberOfBits);
    }
    return result << (targetNumberOfBits - resultBits);
  }

  /**
   * Converts the given int arguments into a byte array converting the ints to their unsigned value.
   * 
   * @param data unsigned values to be put into the resulting byte array.
   * @return array containing the unsigned data.
   */
  public static byte[] getUnsignedData(final int ... data) {
    final byte[] returnData = new byte[data.length];
    for (int i = 0; i < data.length; i++) {
      int b = data[i];
      if (b > 255 || b < 0) {
        throw new IllegalArgumentException("data argument out of unsigned byte range - must be 0..255");
      }
      returnData[i] = (byte)(b & 255);
    }
    return returnData;
  }

  private byte[] data;

  /** Offset that can be set only in constructor, useful to provide a new "view" on the same byte array data. */
  private final int startOffset;

  /** internal offset, sum of startOffset and defaultOffset. */
  private int internalOffset;

  /**
   * Creates a new Memory instance with the given data.
   *
   * @param data data.
   */
  public Memory(final byte[] data) {
    this(data, 0);
  }

  /**
   * Creates a new Memory instance with the given data and fixed offset from start of data.
   *
   * @param data data.
   * @param startOffset fixed offset into given data.
   */
  public Memory(final byte[] data, final int startOffset) {
    this.data = data;
    this.startOffset = startOffset;
    this.internalOffset = startOffset;
  }

  /**
   * Sets the default offset that is always added to offsets given to data access calls.
   *
   * @param defaultOffset default offset added to all offsets.
   */
  public void setDefaultOffset(final int defaultOffset) {
    this.internalOffset = this.startOffset + defaultOffset;
  }

  /**
   * Returns the byte from the given offset, taking into account the start offset and the default offset.
   *
   * @param offset offset into data.
   * @return byte at given offset.
   */
  public byte getByte(final int offset) {
    return this.data[this.internalOffset + offset];
  }

  /**
   * Returns the unsigned byte value from the given offset, taking into account the start offset and the default offset.
   *
   * @param offset offset into data.
   * @return unsigned byte value at given offset.
   */
  public int getUnsignedByte(final int offset) {
    final byte value = this.data[this.internalOffset + offset];
    if (value < 0) {
      return 256 + value;
    }
    return value;
  }

  /**
   * Returns the unsigned 16bit little endian value from the given offset, taking into account the start offset and the default offset.
   *
   * @param offset offset into data.
   * @return unsigned 16bit little endian value at given offset.
   */
  public int getUnsignedHalfWord(final int offset) {
    int lowByte = getUnsignedByte(offset);
    int highByte = getUnsignedByte(offset + 1);
    return highByte * 256 + lowByte;
  }

  /**
   * Returns the unsigned 16bit big endian value from the given offset, taking into account the start offset and the default offset.
   *
   * @param offset offset into data.
   * @return unsigned 16bit big endian value at given offset.
   */
  public int getUnsignedHalfWordBig(final int offset) {
    int highByte = getUnsignedByte(offset);
    int lowByte = getUnsignedByte(offset + 1);
    return highByte * 256 + lowByte;
  }

  /**
   * Returns the unsigned 32bit little endian value from the given offset, taking into account the start offset and the default offset.
   *
   * @param offset offset into data.
   * @return unsigned 32bit little endian value at given offset.
   */
  public long getUnsignedWord(final int offset) {
    long lowByte = getUnsignedByte(offset);
    long lowHighByte = getUnsignedByte(offset + 1);
    long highLowByte = getUnsignedByte(offset + 2);
    long highByte = getUnsignedByte(offset + 3);
    return highByte * 256 * 256 * 256 + highLowByte * 256 * 256 + lowHighByte * 256 + lowByte;
  }

  /**
   * Returns the unsigned 32bit big endian value from the given offset, taking into account the start offset and the default offset.
   *
   * @param offset offset into data.
   * @return unsigned 32bit big endian value at given offset.
   */
  public long getUnsignedWordBig(final int offset) {
    long highByte = getUnsignedByte(offset);
    long highLowByte = getUnsignedByte(offset + 1);
    long lowHighByte = getUnsignedByte(offset + 2);
    long lowByte = getUnsignedByte(offset + 3);
    return highByte * 256 * 256 * 256 + highLowByte * 256 * 256 + lowHighByte * 256 + lowByte;
  }

  /**
   * Returns the 32bit little endian value from the given offset, taking into account the start offset and the default offset.
   *
   * @param offset offset into data.
   * @return 32bit little endian value at given offset.
   */
  public int getWord(final int offset) {
    long lowByte = getUnsignedByte(offset);
    long lowHighByte = getUnsignedByte(offset + 1);
    long highLowByte = getUnsignedByte(offset + 2);
    long highByte = getUnsignedByte(offset + 3);
    return (int) ((highByte * 256 * 256 * 256 + highLowByte * 256 * 256 + lowHighByte * 256 + lowByte) & 0xFFFFFFFF);
  }

  /**
   * Returns the 32bit big endian value from the given offset, taking into account the start offset and the default offset.
   *
   * @param offset offset into data.
   * @return 32bit big endian value at given offset.
   */
  public int getWordBig(final int offset) {
    long highByte = getUnsignedByte(offset);
    long highLowByte = getUnsignedByte(offset + 1);
    long lowHighByte = getUnsignedByte(offset + 2);
    long lowByte = getUnsignedByte(offset + 3);
    return (int) ((highByte * 256 * 256 * 256 + highLowByte * 256 * 256 + lowHighByte * 256 + lowByte) & 0xFFFFFFFF);
  }

  /**
   * Returns a newly allocated byte array with the given length, filled with data from the source starting from offset taking into account
   * start offset and default offset.
   *
   * @param offset offset into data.
   * @param length length in bytes to be copied.
   * @return newly allocated byte array.
   */
  public byte[] getBytes(final int offset, final int length) {
    final byte[] bytes = new byte[length];
    System.arraycopy(this.data, this.internalOffset + offset, bytes, 0, length);
    return bytes;
  }

  /**
   * Extracts 8 bit text until 0 is reached.
   *
   * @param offset
   * @return
   */
  public String getText(final int offset) {
    return getTerminatedText(offset, 0);
  }

  /**
   * Extracts 8 bit text until a control character (less than ASCII 32) is reached.
   *
   * @param offset
   * @return
   */
  public String getControlTerminatedText(final int offset) {
    return getTerminatedText(offset, 31);
  }

  /**
   * Extracts 8 bit text until a byte value less than or equal the given terminate value is reached.
   *
   * @param offset offset into memory to start from.
   * @param terminateIfEqualOrLess terminate string extraction if byte with a value equal or less than this is encountered.
   * @return text
   */
  private String getTerminatedText(final int offset, final int terminateIfEqualOrLess) {
    StringBuilder sb = new StringBuilder();
    int currentByte = getUnsignedByte(offset);
    int add = 0;
    while (currentByte > terminateIfEqualOrLess) {
      sb.append((char) currentByte);
      add++;
      currentByte = getUnsignedByte(offset + add);
    }
    return sb.toString();
  }

  /**
   * Extracts 8 bit text from given offset with given maximum length - if a control character is encountered, extraction is stopped.
   *
   * @param offset
   * @return
   */
  public String getText(final int offset, final int maxLength) {
    StringBuilder sb = new StringBuilder();
    for (int add = 0; add < maxLength; add++) {
      char toAdd = (char) getUnsignedByte(offset + add);
      if (toAdd < 32) {
        break;
      }
      sb.append(toAdd);
    }
    return sb.toString();
  }

  // hubersn implementation, work in progress...
  public long getValue2(final int offset, final int bitOffset, final int bitLength) {
    int realOffset = offset;
    int realBitOffset = bitOffset;
    while (realBitOffset >= 8) {
      realOffset++;
      realBitOffset -= 8;
    }
    long value = getUnsignedByte(realOffset);
    if (bitLength == 4) {
      if (realBitOffset == 0) {
        return value & 0xF;
      }
      return (value & 0xF0) >> 4;
    }
    return 0;
  }

  // FilecoreImageReader implementation...not completely understood...
  /**
   * Returns up to 32bit unsigned value from given byte offset (taking into account start offset and default offset) starting from given bit
   * offset comprising the given number of bits.
   *
   * @param offset byte offset into data.
   * @param bitOffset bit offset.
   * @param bitLength number of bits.
   * @return unsigned value comprised of bitLength bits.
   */
  public long getValue(final int offset, final int bitOffset, final int bitLength) {
    long start_byte;
    long start_bit;
    long bit;
    long b;
    long prev;
    long lastbyte;
    if (bitLength <= 0 || bitLength >= 33) {
      return 0;
    }
    //Reset the result
    long result = 0;
    prev = 0xFFFFFFFFL;
    lastbyte = 0;
    for (bit = 0; bit < bitLength; bit++) {
      //Work out the byte offset, and the bit within
      start_byte = (bitOffset + bit) / 8;
      start_bit = (bitOffset + bit) % 8;
      //And increase the result with the extracted bit, shifted right to account
      //for final position
      if (prev != offset + start_byte) {
        //To save re-reading the same byte over and over
        prev = offset + start_byte;
        lastbyte = getUnsignedByte((int) prev);
      }
      b = (lastbyte & (1 << start_bit)) >> start_bit;
      result += (b << bit);
    }
    return result;
  }

  /**
   * Returns the word-aligned offset for the given base offset.
   * 
   * @param offset offset to word-align.
   * @return word-aligned offset
   */
  public static int align(final int offset) {
    if (offset % 4 == 0) {
      return offset;
    }
    return offset + (4 - (offset % 4));
  }

  /**
   * Returns the complete underlying data of this Memory object.
   *
   * @return underlying data of this Memory object.
   */
  public byte[] getData() {
    return this.data;
  }

  /**
   * Returns a new byte array with the original data starting from
   * current internal offset (startOffset + defaultOffset) of given
   * length.
   * 
   * @param length length of slice.
   * @return new byte array with copied data.
   */
  public byte[] getDataSlice(final int length) {
    return getDataSlice(0, length);
  }

  /**
   * Returns a new byte array with the original data starting from
   * current internal offset (startOffset + defaultOffset) plus given
   * offset of given length.
   * 
   * @param offset additional offset added to startOffset and defaultOffset.
   * @param length length of slice.
   * @return new byte array with copied data.
   */
  public byte[] getDataSlice(final int offset, final int length) {
    final byte[] returnValue = new byte[length];
    System.arraycopy(this.data, this.internalOffset + offset, returnValue, 0, length);
    return returnValue;
  }
}