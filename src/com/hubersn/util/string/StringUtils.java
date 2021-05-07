/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.util.string;

import java.awt.Color;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Various string operations badly missing from String/StringBuffer/StringBuilder class.
 */
public class StringUtils {

  /** Start of placeholder definition for replaceNamedPlaceholders */
  private static final String PLACEHOLDER_START = "${";

  /** End of placeholder definition for replaceNamedPlaceholders */
  private static final String PLACEHOLDER_END = "}";

  /**
   * Case-insensitive variant of String.startsWith.
   * 
   * @param s source string to check against.
   * @param prefix possible prefix to check.
   */
  public static boolean startsWithIgnoreCase(final String s, final String prefix) {
    if (s.length() < prefix.length()) {
      return false;
    }
    return prefix.equalsIgnoreCase(s.substring(0, prefix.length()));
  }

  /**
   * Returns true if all characters in given source string are digits, i.e. Integer.parseInt will probably work.
   * @param s source string.
   * @return all characters of source string digits?
   */
  public static boolean isOnlyDigits(final String s) {
    if (isEmptyOrNull(s)) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Splits the given String at the given separators and possibly trims the parts it returns.
   *
   * @param str input string.
   * @param separators separators to split at (every char of this String).
   * @param trim trim splitted result.
   * @return parts of the string.
   */
  public static String[] split(final String str, final String separators, final boolean trim) {
    final ArrayList<String> parts = new ArrayList<>();
    int startPart = 0;

    for (int i = 0; i < str.length(); i++) {
      char it = str.charAt(i);
      if (separators.indexOf(it) >= 0) {
        String part = str.substring(startPart, i);
        if (trim) {
          parts.add(part.trim());
        } else {
          parts.add(part);
        }
        startPart = i + 1;
      }
    }

    if (startPart < str.length()) {
      String part = str.substring(startPart);
      if (trim) {
        parts.add(part.trim());
      } else {
        parts.add(part);
      }
    }

    return parts.toArray(new String[0]);
  }

  /**
   * Joins the elements of the given string array to one string, separated by the given separator.
   *
   * @param strings input strings.
   * @param separator separator to put between input string elements, may be empty string.
   * @return joined elements separated with separator, null on null input.
   */
  public static String join(final String[] strings, final String separator) {
    if (separator == null) {
      throw new IllegalArgumentException("Separator must not be null.");
    }
    if (strings == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < strings.length - 1; i++) {
      sb.append(strings[i]);
      sb.append(separator);
    }
    if (strings.length > 0) {
      sb.append(strings[strings.length - 1]);
    }
    return sb.toString();
  }

  /**
   * Joins the elements of the given iterator to one string, separated by the given separator.
   *
   * @param strings input strings.
   * @param separator separator to put between input string elements, may be empty string.
   * @return joined elements separated with separator, null on null input.
   */
  public static String join(final Iterator<?> strings, final String separator) {
    if (separator == null) {
      throw new IllegalArgumentException("Separator must not be null.");
    }
    if (strings == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    while (strings.hasNext()) {
      sb.append(strings.next().toString());
      if (strings.hasNext()) {
        sb.append(separator);
      }
    }
    return sb.toString();
  }

  /**
   * Extends the given source string with the given filler character to the given length (to the right).
   *
   * @param source source string.
   * @param length length of result.
   * @param filler filler character.
   * @return extended string.
   */
  public static String extend(final String source, final int length, final char filler) {
    if (length < source.length()) {
      return source.substring(0, length);
    }
    final StringBuilder sb = new StringBuilder(source);
    for (int i = sb.length(); i < length; i++) {
      sb.append(filler);
    }
    return sb.toString();
  }

  /**
   * Indents the given source string with the given number of spaces.
   *
   * @param source source string.
   * @param indentation number of spaces to add to front of source string.
   * @return indented string.
   */
  public static String indent(final String source, final int indentation) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < indentation; i++) {
      sb.append(' ');
    }
    sb.append(source);
    return sb.toString();
  }

  /**
   * Returns the given number as a string, filling up leading zeroes to the given length.
   *
   * @param number number to convert
   * @param fillUpLength length to fill up
   * @return number as string with leading zeroes
   */
  public static String format(final int number, final int fillUpLength) {
    final String result = "" + number;
    return extend("", fillUpLength - result.length(), '0') + result;
  }

  /**
   * Returns an integer value for a character representing a hex nibble. Both lower and upper case letters a-f are accepted. An
   * IllegalArgumentException is thrown when a non-hex char is given.
   *
   * @param c hex char (0-9,a-f,A-F)
   * @return integer value for hex char(0-15)
   */
  public static int hexCharToValue(char c) {
    switch (c) {
      case '0':
        return 0;
      case '1':
        return 1;
      case '2':
        return 2;
      case '3':
        return 3;
      case '4':
        return 4;
      case '5':
        return 5;
      case '6':
        return 6;
      case '7':
        return 7;
      case '8':
        return 8;
      case '9':
        return 9;
      case 'a':
      case 'A':
        return 10;
      case 'b':
      case 'B':
        return 11;
      case 'c':
      case 'C':
        return 12;
      case 'd':
      case 'D':
        return 13;
      case 'e':
      case 'E':
        return 14;
      case 'f':
      case 'F':
        return 15;
      default:
        throw new IllegalArgumentException("No Hex Code: " + c);
    }
  }

  /**
   * Converts a value between 0 and 15 into the corresponding hex character, either lower or upper case letters will be returned - throws an
   * IllegalArgumentException on illegal input values.
   *
   * @param value value to convert.
   * @param useUpperCase use upper case letters A-F?
   * @return hex character representing value.
   */
  public static char valueToHexChar(final long value, final boolean useUpperCase) {
    if (value < 0 || value > 15) {
      throw new IllegalArgumentException("Value " + value + " out of range - must be 0-15");
    }
    if (value < 10) return (char) ('0' + value);
    if (useUpperCase) {
      return (char) ('A' + (value - 10));
    }
    return (char) ('a' + (value - 10));
  }

  /**
   * Converts the given value to a number of hex characters specified in nibble count value.
   *
   * @param value
   * @param nibbleCount amount of nibbles (characters) to return.
   * @param useUpperCase use upper case hex characters.
   * @return hex representation of value.
   */
  public static String valueToHexChars(final long value, final int nibbleCount, final boolean useUpperCase) {
    char[] hexChars = new char[nibbleCount];
    for (int i = 0; i < nibbleCount; i++) {
      hexChars[i] = valueToHexChar(0b1111 & (value >> (4 * (nibbleCount - 1 - i))), useUpperCase);
    }
    return new String(hexChars);
  }

  /**
   * Removes all control characters including spaces from the left of the given string.
   *
   * @param source source string.
   * @return left-trimmed source string.
   */
  public static final String trimOnLeft(final String source) {
    if (source == null) {
      return null;
    }
    final char[] cstr = source.toCharArray();
    int i = 0;
    final int len = cstr.length;
    for (i = 0; i < len; ++i) {
      if (cstr[i] > '\u0020') {
        break;
      }
    }
    if (i >= len) {
      return "";
    }
    return new String(cstr, i, len - i);
  }

  /**
   * Replaces all occurrences of given string-to-replace with new string.
   *
   * @param sourceStr source string to operate on - null is not an error
   * @param strToReplace string that should be replaced - if null, sourceStr is returned
   * @param newStr replacement - if null, empty string is used
   * @return the processed string
   */
  public static String replace(final String sourceStr, final String strToReplace, String newStr) {
    if (strToReplace == null) {
      return sourceStr;
    }

    if (newStr == null) {
      newStr = "";
    }
    StringBuilder strBuf = null;
    if (sourceStr != null) {
      final int oldLen = strToReplace.length();
      int nextPos = sourceStr.indexOf(strToReplace);
      int lastPos = 0;

      if (nextPos != -1) {
        // guess initial length
        strBuf = new StringBuilder(sourceStr.length() + newStr.length());

        strBuf.append(sourceStr.substring(0, nextPos));
        strBuf.append(newStr);

        lastPos = nextPos + oldLen;
        nextPos = sourceStr.indexOf(strToReplace, lastPos);
        while (nextPos != -1) {
          strBuf.append(sourceStr.substring(lastPos, nextPos));
          strBuf.append(newStr);
          lastPos = nextPos + oldLen;
          nextPos = sourceStr.indexOf(strToReplace, lastPos);
        }
        strBuf.append(sourceStr.substring(lastPos));
      }
    }
    return strBuf == null ? sourceStr : strBuf.toString();
  }

  /**
   * Escapes all characters in the given string for saving in property files that are read via ResourceBundle.
   *
   * @param source source string
   * @return escaped string
   */
  public static String escapeForProperties(final String source) {
    return replace(source, "\\", "\\\\");
  }

  /**
   * Returns a human readable form of memory amount representation for given plain number of bytes.
   *
   * @param bytes number of bytes.
   * @param siUnits true for SI (base 1000 - k, M, G etc.) or false for IEC (base 1024 - Ki, Mi, Gi).
   * @param decimalPlaces amount of decimal places in output.
   * @return human readable form.
   */
  public static String getHumanReadableByteCount(final long bytes, final boolean siUnits, final int decimalPlaces) {
    return getHumanReadableByteCount(bytes, siUnits, decimalPlaces, 0, 1000);
  }

  /**
   * Returns a human readable form of memory amount representation for given plain number of bytes, can be forced and limited to a minimum
   * and maximum power. Supports up to Exabyte/Exbibyte.
   *
   * @param bytes number of bytes.
   * @param siUnits true for SI (base 1000 - k, M, G etc.) or false for IEC (base 1024 - Ki, Mi, Gi etc.) units.
   * @param decimalPlaces amount of decimal places in output.
   * @param minPower minimum power of result - e.g. 1 for at least kB/KiB, 2 for MB/MiB etc.
   * @param maxPower maximum power of result - e.g. 1 for at most kB/KiB, 2 for MB/MiB etc.
   * @return human readable form.
   */
  public static String getHumanReadableByteCount(final long bytes,
                                                 final boolean siUnits,
                                                 final int decimalPlaces,
                                                 final int minPower,
                                                 final int maxPower) {
    int unit = siUnits ? 1000 : 1024;
    if (bytes < unit && minPower == 0) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    if (exp < minPower) {
      exp = minPower;
    }
    if (exp > maxPower) {
      exp = maxPower;
    }
    String pre = (siUnits ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (siUnits ? "" : "i");
    return String.format("%." + decimalPlaces + "f %sB", bytes / Math.pow(unit, exp), pre);
  }

  /**
   * Returns true if the given string is either null or completely empty - whitespace is not empty!
   *
   * @param s string to check
   * @return string empty or null?
   */
  public static boolean isEmptyOrNull(final String s) {
    return s == null || s.length() == 0;
  }

  private static final String COLOUR_REP_SEP = ",";

  /**
   * Converts a colour into a string representation - format: r,g,b or r,g,b,a if alpha value != 0 exists.
   *
   * @param colour colour
   * @return string representation of given colour.
   */
  public static String colorToString(final Color colour) {
    String colourString = colour.getRed() + COLOUR_REP_SEP + colour.getGreen() + COLOUR_REP_SEP + colour.getBlue();
    if (colour.getAlpha() > 0) {
      colourString += COLOUR_REP_SEP + colour.getAlpha();
    }
    return colourString;
  }

  /**
   * Parses a colour string representation (r,g,b or r,g,b,a) and creates a colour, or throws an exception if format is invalid.
   *
   * @param colstr string containing colour representation - if null, null is returned.
   * @return colour parsed from given string
   * @throws NumberFormatException if not in correct format
   */
  public static Color stringToColor(final String colstr) throws NumberFormatException {
    if (colstr == null) {
      return null;
    }
    final int firstSeparatorPosition = colstr.indexOf(COLOUR_REP_SEP);
    if (firstSeparatorPosition == -1) {
      throw new NumberFormatException("No separator found.");
    }
    final int secondSeparatorPosition = colstr.indexOf(COLOUR_REP_SEP, firstSeparatorPosition + COLOUR_REP_SEP.length());
    if (secondSeparatorPosition == -1) {
      throw new NumberFormatException("Only one separator found - no valid colour parseable.");
    }
    final int r = Integer.parseInt(colstr.substring(0, firstSeparatorPosition).trim());
    final int g = Integer.parseInt(colstr.substring(firstSeparatorPosition + COLOUR_REP_SEP.length(), secondSeparatorPosition).trim());
    // check for alpha
    final int thirdSeparatorPosition = colstr.indexOf(COLOUR_REP_SEP, secondSeparatorPosition + COLOUR_REP_SEP.length());
    if (thirdSeparatorPosition == -1) {
      final int b = Integer.parseInt(colstr.substring(secondSeparatorPosition + COLOUR_REP_SEP.length(), colstr.length()).trim());
      return new Color(r, g, b);
    } else {
      final int b = Integer.parseInt(colstr.substring(secondSeparatorPosition + COLOUR_REP_SEP.length(), thirdSeparatorPosition).trim());
      final int a = Integer.parseInt(colstr.substring(thirdSeparatorPosition + COLOUR_REP_SEP.length(), colstr.length()).trim());
      return new Color(r, g, b, a);
    }
  }

  /**
   * Replaces placeholders {0}, {1} etc. in the source string with the given replacement values using MessageFormat.
   *
   * @param source source string.
   * @param replacementValues replacement values for placeholders.
   * @return source string with replaced placeholders.
   */
  public static String replacePlaceholders(final String source, final Object ... replacementValues) {
    return MessageFormat.format(source, replacementValues);
  }

  /**
   * Does the given string contain a named placeholder of form ${name_of_placeholder}?
   *
   * @param s string to check.
   * @return string contains named placeholder?
   */
  public static boolean containsNamedPlaceholder(final String s) {
    final int placeholderStartOffset = s.indexOf(StringUtils.PLACEHOLDER_START);
    if (placeholderStartOffset >= 0) {
      return s.indexOf(StringUtils.PLACEHOLDER_END, placeholderStartOffset) >= 0;
    }
    return false;
  }

  /**
   * Replaces placeholders of form ${key} (empty prefix) and ${prefixkey} (non-empty prefix - note that, for flexibility, there is no
   * standard separator defined between prefix and key) with help of given replacementValues map,
   *
   * @param s source string.
   * @param replacementValues key/value map to be used to replace placeholders.
   * @param prefix prefix to consider.
   * @return source string with replaced placeholders.
   */
  public static String replaceNamedPlaceholders(final String s, final Map<String, String> replacementValues, final String prefix) {
    // replace placeholders of form ${prefix:placeholderName} with values provided by replacementValues map
    // similar functionality: Apache commons-lang4 "StrInterpolator"
    String returnString = s;
    final String placeholderToSearchFor = PLACEHOLDER_START + prefix;
    final int placeholderLength = (placeholderToSearchFor).length();
    int startOfPlaceholder = returnString.indexOf(placeholderToSearchFor);
    while (startOfPlaceholder >= 0) {
      int endOfPlaceholder = returnString.indexOf(PLACEHOLDER_END, startOfPlaceholder + placeholderLength);
      if (endOfPlaceholder >= 0) {
        String keyToReplace = returnString.substring(startOfPlaceholder + placeholderLength, endOfPlaceholder);
        String replacementValue = replacementValues.get(keyToReplace);
        if (replacementValue != null) {
          returnString = returnString.substring(0, startOfPlaceholder) + replacementValue + returnString.substring(endOfPlaceholder + 1);
          // adjust for placeholder length vs. replacement length
          endOfPlaceholder = endOfPlaceholder + (placeholderLength + keyToReplace.length() + 1) - replacementValue.length();
          startOfPlaceholder = returnString.indexOf(placeholderToSearchFor, endOfPlaceholder);
        } else {
          // advance to next placeholder
          startOfPlaceholder = returnString.indexOf(placeholderToSearchFor, endOfPlaceholder);
        }
      }
    }
    return returnString;
  }

  /**
   * TODO support nested placeholders! Replaces placeholders of form ${key} (empty prefix) and ${prefixkey} (non-empty prefix - note that, for flexibility, there is no
   * standard separator defined between prefix and key) with help of given replacementValues map,
   *
   * @param s source string.
   * @param replacementValues key/value map to be used to replace placeholders.
   * @param prefix prefix to consider.
   * @return source string with replaced placeholders.
   */
  public static String replaceNamedNestedPlaceholders(final String s, final Map<String, String> replacementValues, final String prefix) {
    // replace placeholders of form ${prefix:placeholderName} with values provided by replacementValues map
    // similar functionality: Apache commons-lang4 "StrInterpolator"
    String returnString = s;
    final String placeholderToSearchFor = PLACEHOLDER_START + prefix;
    final int placeholderLength = (placeholderToSearchFor).length();
    int startOfPlaceholder = returnString.indexOf(placeholderToSearchFor);
    while (startOfPlaceholder >= 0) {
      int endOfPlaceholder = returnString.indexOf(PLACEHOLDER_END, startOfPlaceholder + placeholderLength);
      int possibleNextStartOfPlaceholder = returnString.indexOf(placeholderToSearchFor, startOfPlaceholder + 1);
      if (possibleNextStartOfPlaceholder >= 0 && possibleNextStartOfPlaceholder < endOfPlaceholder) {
        // jump to next placeholder to resolve nested placeholder first!
      }
      if (endOfPlaceholder >= 0) {
        String keyToReplace = returnString.substring(startOfPlaceholder + placeholderLength, endOfPlaceholder);
        String replacementValue = replacementValues.get(keyToReplace);
        if (replacementValue != null) {
          returnString = returnString.substring(0, startOfPlaceholder) + replacementValue + returnString.substring(endOfPlaceholder + 1);
          // adjust for placeholder length vs. replacement length
          endOfPlaceholder = endOfPlaceholder + (placeholderLength + keyToReplace.length() + 1) - replacementValue.length();
          startOfPlaceholder = returnString.indexOf(placeholderToSearchFor, endOfPlaceholder);
        } else {
          // advance to next placeholder
          startOfPlaceholder = returnString.indexOf(placeholderToSearchFor, endOfPlaceholder);
        }
      }
    }
    return returnString;
  }

  /**
   * Processes the given source string to resolve all Java standard escape sequences, see
   * https://docs.oracle.com/javase/tutorial/java/data/characters.html for doc.
   *
   * @param source source string.
   * @return processed string with all escape sequences resolved.
   */
  public static String unescapeString(final String source) {
    final int sourceLength = source.length();
    final StringBuilder sb = new StringBuilder(sourceLength);
    for (int i = 0; i < sourceLength; i++) {
      char character = source.charAt(i);
      if (character == '\\') {
        if (i < sourceLength - 1) {
          i++;
          char nextCharacter = source.charAt(i);
          switch (nextCharacter) {
            case 't':
              character = '\t';
              break;
            case 'b':
              character = '\b';
              break;
            case 'n':
              character = '\n';
              break;
            case 'r':
              character = '\r';
              break;
            case 'f':
              character = '\f';
              break;
            case '\'':
              character = '\'';
              break;
            case '"':
              character = '"';
              break;
            case '\\':
              character = '\\';
              break;
            case 'u':
              try {
                int value = 0;
                for (int c = 0; c < 4; c++) {
                  char codepointChar = source.charAt(i + 1 + c);
                  switch (codepointChar) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                      value = (value << 4) + codepointChar - '0';
                      break;
                    case 'a':
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                      value = (value << 4) + 10 + codepointChar - 'a';
                      break;
                    case 'A':
                    case 'B':
                    case 'C':
                    case 'D':
                    case 'E':
                    case 'F':
                      value = (value << 4) + 10 + codepointChar - 'A';
                      break;
                    default:
                      // nothing...illegal unicode sequence, ignore, no recovery possible
                      break;
                  }
                }
                i += 4;
                character = (char) value;
              } catch (Exception ex) {
                character = 'u';
              }
              break;
            default:
              // failure to detect valid escape sequence, just add the backslash "as is"
              i--;
              break;
          }
        }
      }
      sb.append(character);
    }
    return sb.toString();
  }

  /**
   * Decodes the given BinHex-coded string and returns the resulting binary data - can handle letters in lower case and upper case, throws
   * IllegalArgumentException on malformed BinHex data.
   *
   * @param binHex BinHex-coded string.
   * @return binary data.
   */
  public static final byte[] binHexToBinary(final String binHex) {
    final int binHexLen = binHex.length();
    if (binHexLen % 2 != 0) {
      throw new IllegalArgumentException("Illegal BinHex format.");
    }
    final byte[] result = new byte[binHexLen / 2];
    int binIndex = 0;
    for (int index = 0; index < binHexLen; index += 2) {
      final char highByteChar = binHex.charAt(index);
      final char lowByteChar = binHex.charAt(index + 1);
      result[binIndex] = (byte) (hexCharToValue(highByteChar) * 16 + hexCharToValue(lowByteChar));
      binIndex++;
    }
    return result;
  }

  /**
   * Convert given byte array to a simple (first generation, non-length-limited) BinHex-encoded string.
   *
   * @param bin binary data in byte array.
   * @param useUpperCase use upper case letters A,B,...,F for hex values?
   * @return binHex-encoded string.
   */
  public static final String binaryToBinHex(final byte[] bin, final boolean useUpperCase) {
    final StringBuilder sb = new StringBuilder(bin.length * 2);
    for (int i = 0; i < bin.length; i++) {
      byte value = bin[i];
      int highByte = (value & 0b11110000) >> 4;
      int lowByte = value & 0b1111;
      sb.append(valueToHexChar(highByte, useUpperCase));
      sb.append(valueToHexChar(lowByte, useUpperCase));
    }
    return sb.toString();
  }

  /**
   * Tests whether or not a string matches against a pattern.
   * The pattern may contain two special characters:<br>
   * '*' means zero or more characters<br>
   * '?' means one and only one character
   *
   * @param pattern The pattern to match against.
   *                Must not be <code>null</code>.
   * @param str     The string which must be matched against the pattern.
   *                Must not be <code>null</code>.
   * @param isCaseSensitive Whether or not matching should be performed
   *                        case sensitively.
   *
   *
   * @return <code>true</code> if the string matches against the pattern,
   *         or <code>false</code> otherwise.
   */
  public static boolean match(String pattern,
                              String str,
                              boolean isCaseSensitive) {
    char[] patArr = pattern.toCharArray();
    char[] strArr = str.toCharArray();
    int patIdxStart = 0;
    int patIdxEnd = patArr.length - 1;
    int strIdxStart = 0;
    int strIdxEnd = strArr.length - 1;
    char ch;

    boolean containsStar = false;
    for (int i = 0; i < patArr.length; i++) {
      if (patArr[i] == '*') {
        containsStar = true;
        break;
      }
    }

    if (!containsStar) {
      // No '*'s, so we make a shortcut
      if (patIdxEnd != strIdxEnd) {
        return false; // Pattern and string do not have the same size
      }
      for (int i = 0; i <= patIdxEnd; i++) {
        ch = patArr[i];
        if (ch != '?') {
          if (isCaseSensitive && ch != strArr[i]) {
            return false; // Character mismatch
          }
          if (!isCaseSensitive
              && Character.toUpperCase(ch) != Character.toUpperCase(strArr[i])) {
            return false; // Character mismatch
          }
        }
      }
      return true; // String matches against pattern
    }

    if (patIdxEnd == 0) {
      return true; // Pattern contains only '*', which matches anything
    }

    // Process characters before first star
    while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
      if (ch != '?') {
        if (isCaseSensitive && ch != strArr[strIdxStart]) {
          return false; // Character mismatch
        }
        if (!isCaseSensitive
            && Character.toUpperCase(ch) != Character
                .toUpperCase(strArr[strIdxStart])) {
          return false; // Character mismatch
        }
      }
      patIdxStart++;
      strIdxStart++;
    }
    if (strIdxStart > strIdxEnd) {
      // All characters in the string are used. Check if only '*'s are
      // left in the pattern. If so, we succeeded. Otherwise failure.
      for (int i = patIdxStart; i <= patIdxEnd; i++) {
        if (patArr[i] != '*') {
          return false;
        }
      }
      return true;
    }

    // Process characters after last star
    while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
      if (ch != '?') {
        if (isCaseSensitive && ch != strArr[strIdxEnd]) {
          return false; // Character mismatch
        }
        if (!isCaseSensitive
            && Character.toUpperCase(ch) != Character
                .toUpperCase(strArr[strIdxEnd])) {
          return false; // Character mismatch
        }
      }
      patIdxEnd--;
      strIdxEnd--;
    }
    if (strIdxStart > strIdxEnd) {
      // All characters in the string are used. Check if only '*'s are
      // left in the pattern. If so, we succeeded. Otherwise failure.
      for (int i = patIdxStart; i <= patIdxEnd; i++) {
        if (patArr[i] != '*') {
          return false;
        }
      }
      return true;
    }

    // process pattern between stars. padIdxStart and patIdxEnd point
    // always to a '*'.
    while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
      int patIdxTmp = -1;
      for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
        if (patArr[i] == '*') {
          patIdxTmp = i;
          break;
        }
      }
      if (patIdxTmp == patIdxStart + 1) {
        // Two stars next to each other, skip the first one.
        patIdxStart++;
        continue;
      }

      // Find the pattern between padIdxStart & padIdxTmp in str between
      // strIdxStart & strIdxEnd
      int patLength = (patIdxTmp - patIdxStart - 1);
      int strLength = (strIdxEnd - strIdxStart + 1);
      int foundIdx = -1;
      strLoop: for (int i = 0; i <= strLength - patLength; i++) {
        for (int j = 0; j < patLength; j++) {
          ch = patArr[patIdxStart + j + 1];
          if (ch != '?') {
            if (isCaseSensitive && ch != strArr[strIdxStart + i + j]) {
              continue strLoop;
            }
            if (!isCaseSensitive
                && Character.toUpperCase(ch) != Character
                    .toUpperCase(strArr[strIdxStart + i + j])) {
              continue strLoop;
            }
          }
        }

        foundIdx = strIdxStart + i;
        break;
      }

      if (foundIdx == -1) {
        return false;
      }

      patIdxStart = patIdxTmp;
      strIdxStart = foundIdx + patLength;
    }

    // All characters in the string are used. Check if only '*'s are left
    // in the pattern. If so, we succeeded. Otherwise failure.
    for (int i = patIdxStart; i <= patIdxEnd; i++) {
      if (patArr[i] != '*') {
        return false;
      }
    }
    return true;
  }

  private StringUtils() {
    // no instances allowed
  }
}