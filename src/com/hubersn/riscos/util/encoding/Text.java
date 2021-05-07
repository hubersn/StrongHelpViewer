package com.hubersn.riscos.util.encoding;

import java.io.UnsupportedEncodingException;

public class Text {

  public static String getText(final byte[] data) {
    try {
      // TODO proper Latin1 encoding
      return new String(data, "WINDOWS-1252");
    } catch (UnsupportedEncodingException e) {
      // this encoding is available EVERYWHERE!
    }
    // platform encoding, probably wrong, but a fallback
    return new String(data);
  }
}
