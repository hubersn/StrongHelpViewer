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

package com.hubersn.riscos.stronghelp.content;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingConstants;

import com.hubersn.util.string.StringUtils;

/**
 * StrongHelp Font/Styles configuration file interpreter and encapsulation.
 */
public class SHFontConfig {

  private static final String FALLBACK_FONT = "serif";

  public static final Color DEFAULT_BACKGROUND = Color.white;

  private static Color[] WIMP_COLOURS = new Color[] {new Color(0xffffff),
      //new Color(0xdddddd), original WIMP colour 1, replaced with background tile, too dark for us
      // use WIMP colour 1 in a lighter grey variant to better match appearance of background tile
      new Color(0xf1f1f1), // TODO take from Swing "standard panel/frame background"?
      new Color(0xbbbbbb),
      new Color(0x999999),
      new Color(0x777777),
      new Color(0x555555),
      new Color(0x333333),
      new Color(0x000000),
      new Color(0x004499),
      new Color(0xeeee00),
      new Color(0x00cc00),
      new Color(0xdd0000),
      new Color(0xeeeebb),
      new Color(0x558800),
      new Color(0xffbb00),
      new Color(0x00bbff)};

  private Map<String, String> fontMap;

  private List<PhysicalFont> physicalFonts;

  private List<FontStyle> fontStyles;

  private Color backgroundColour = null;

  private static String toWebColour(final Color colour) {
    return "#" + StringUtils.valueToHexChars(colour.getRGB() & 0xFFFFFF, 6, true);
  }

  private static String getFontStyleRepresentation(final int fontStyle) {
    if (fontStyle == Font.PLAIN) {
      return "  font-style: normal;\n  font-weight: normal;\n";
    }
    if (fontStyle == Font.ITALIC) {
      return "  font-style: italic;\n  font-weight: normal;\n";
    }
    if (fontStyle == Font.BOLD) {
      return "  font-style: normal;\n  font-weight: bold;\n";
    }
    if (fontStyle == Font.BOLD + Font.ITALIC) {
      return "  font-style: italic;\n  font-weight: bold;\n";
    }
    return "";
  }

  private static class PhysicalFont {
    private int number = -1;
    private String acornFontName; // for debug purposes maybe
    private String htmlFontName = null;
    private int fontSize = -1;
    private int fontSizeY; // how to support in HTML?
    private int fontStyle; // Font style, i.e. plain, italic, bold, bold-italic
    private String getStyles() {
      return getStyles(this.fontStyle);
    }
    private String getStyles(final int replacementStyle) {
      int fontStyleToUse = this.fontStyle;
      if (replacementStyle != -1) {
        fontStyleToUse = replacementStyle;
      }
      return this.number == -1 ? "" :
        (this.htmlFontName == null ? "" : "  font-family: " + this.htmlFontName + ";\n")
      + (this.fontSize == -1 ? "" : "  font-size: " + this.fontSize + "pt;\n")
      + getFontStyleRepresentation(fontStyleToUse);
    }
    @Override
    public String toString() {
      return getId() + " {\n"
           + getStyles()
           + "}\n";
    }
    public String getId() {
      return this.number == -1 ? "" : "#fontid_" + this.number;
    }
  }

  private class FontStyle {
    private String name;
    private int physicalFontNumber = -1; // may or may not exist
    private int fontStyle = -1; // Font style, i.e. plain, italic, bold, bold-italic
    private Color colour = null; // foreground colour
    private Color backgroundColour = null; // background colour, only for body/Std style
    private int alignment = -1; // SwingConstants. LEFT, RIGHT, CENTER
    private boolean underline = false;
    public FontStyle() {
      super();
    }
    public FontStyle(final FontStyle baseStyle) {
      super();
      this.name = baseStyle.name;
      this.physicalFontNumber = baseStyle.physicalFontNumber;
      this.fontStyle = baseStyle.fontStyle;
      this.colour = baseStyle.colour;
      this.backgroundColour = baseStyle.backgroundColour;
      this.alignment = baseStyle.alignment;
      this.underline = baseStyle.underline;
    }
    private String getAlignmentRep() {
      if (this.alignment == SwingConstants.LEFT) {
        return "  text-align: left;\n";
      }
      if (this.alignment == SwingConstants.RIGHT) {
        return "  text-align: right;\n";
      }
      if (this.alignment == SwingConstants.CENTER) {
        return "  text-align: center;\n";
      }
      return "";
    }
    private String getFontStyleId() {
      // special casing for styles referring to well-known HTML tags:
      // H1-H6, Link, Strong, Emphasis, Code, body (Std)
      if ("h1".equalsIgnoreCase(this.name)) {
        return "h1";
      }
      if ("h2".equalsIgnoreCase(this.name)) {
        return "h2";
      }
      if ("h3".equalsIgnoreCase(this.name)) {
        return "h3";
      }
      if ("h4".equalsIgnoreCase(this.name)) {
        return "h4";
      }
      if ("h5".equalsIgnoreCase(this.name)) {
        return "h5";
      }
      if ("h6".equalsIgnoreCase(this.name)) {
        return "h6";
      }
      if ("code".equalsIgnoreCase(this.name)) {
        return "code";
      }
      if ("link".equalsIgnoreCase(this.name)) {
        return "a:link";
      }
      if ("std".equalsIgnoreCase(this.name)) {
        return "body";
      }
      if ("table".equalsIgnoreCase(this.name)) {
        return "table";
      }
      if ("strong".equalsIgnoreCase(this.name)) {
        return "b";
      }
      if ("emphasis".equalsIgnoreCase(this.name)) {
        return "i";
      }
      if ("underline".equalsIgnoreCase(this.name)) {
        return "u";
      }
      // default style name for non-standard styles
      return "#style_" + this.name;
    }
    @Override
    public String toString() {
      // inject background colour if we are the "body" style
      if ("body".equalsIgnoreCase(getFontStyleId()) && SHFontConfig.this.backgroundColour != null) {
        this.backgroundColour = SHFontConfig.this.backgroundColour;
      }
      // font style rep has to include style only if we don't have our own
      final PhysicalFont basePhysicalFont = (this.physicalFontNumber == -1 ? null : getPhysicalFont(this.physicalFontNumber));
      return getFontStyleId() + " {\n"
           + "  text-decoration: " + (this.underline ? "underline;\n" : "none;\n")
           + (basePhysicalFont == null ? getFontStyleRepresentation(this.fontStyle) : basePhysicalFont.getStyles(this.fontStyle))
           + (this.colour == null ? "" : "  color: " + toWebColour(this.colour) + ";\n")
           + (this.backgroundColour == null ? "" : "  background-color: " + toWebColour(this.backgroundColour) + ";\n")
           + getAlignmentRep()
           + "}\n";
    }
  }

  private PhysicalFont getPhysicalFont(final int number) {
    for (final PhysicalFont physicalFont : this.physicalFonts) {
      if (physicalFont.number == number) {
        return physicalFont;
      }
    }
    return new PhysicalFont();
  }

  /**
   * Creates a new instance of SHFontConfig that encapsulates font configs and styles.
   */
  public SHFontConfig() {
    this.physicalFonts = new ArrayList<>();
    this.fontStyles = new ArrayList<>();
    // TODO better RISC OS font mapping needed
    this.fontMap = new HashMap<>();
    this.fontMap.put("Trinity", "\"Times New Roman\", Times, Serif");
    this.fontMap.put("Homerton", "Arial, Helvetica, Sans-serif");
    this.fontMap.put("Corpus", "\"Courier New\", \"Lucida Console\", Courier, Monospace");
    this.fontMap.put("Selwyn", "Webdings, Wingdings, ZapfDingbats");
    this.fontMap.put("Sidney", "Symbol");
    // other Acorn !FontPrint default mappings:
    // Clare -> AvantGarde
    // Robinson -> Bookman
    // NewHall.Medium -> NewCenturySchlbk-Roman
    // Pembroke -> Palatino
    // Churchill.Medium.Italic -> ZapfChancery-MediumItalic
  }

  private String getHTMLFont(final String acornFontName) {
    final String htmlFont = this.fontMap.get(acornFontName);
    if (htmlFont == null) {
      return FALLBACK_FONT;
    }
    return htmlFont;
  }

  private void parsePhysicalFontConfig(final PhysicalFont pf, final String definition) {
    String[] params = definition.split("\\s+");
    // for now, just the font name and the size
    pf.acornFontName = params[0];
    pf.fontSize = Integer.parseInt(params[1]);
    // Acorn font names should always be FontName - Specifier (Medium, Bold, some-other) - Italic/Oblique
    String[] fontParts = StringUtils.split(pf.acornFontName, ".", true);
    pf.htmlFontName = getHTMLFont(fontParts[0]);
    pf.fontStyle = Font.PLAIN;
    for (int i = 1; i < fontParts.length; i++) {
      final String fontPart = fontParts[i];
      if ("italic".equalsIgnoreCase(fontPart) || "oblique".equalsIgnoreCase(fontPart)) {
        pf.fontStyle |= Font.ITALIC;
      }
      if ("bold".equalsIgnoreCase(fontPart)) {
        pf.fontStyle |= Font.BOLD;
      }
    }
  }

  private static int getPhysicalFontNumber(final String fontRef) {
    if (StringUtils.startsWithIgnoreCase(fontRef, "f")) {
      if (StringUtils.isOnlyDigits(fontRef.substring(1))) {
        return Integer.parseInt(fontRef.substring(1));
      }
    }
    return -1;
  }

  private static void fillFontStyle(final String fontStyleStr, final FontStyle fontStyle) {
    int fs = -1;
    if (StringUtils.startsWithIgnoreCase(fontStyleStr, "f")) {
      fs = Font.PLAIN;
      for (int i = 1; i < fontStyleStr.length(); i++) {
        switch (fontStyleStr.charAt(i)) {
          case '/':
            fs |= Font.ITALIC;
            break;
          case '*':
            fs |= Font.BOLD;
            break;
          case '_':
            fontStyle.underline = true;
            if (fontStyleStr.length() == 2) {
              // hack - we only have one character, so it is "underline, but not plain, so undefined fontStyle"
              fs = -1;
            }
            break;
        }
      }
    }
    fontStyle.fontStyle = fs;
  }

  private static void parseFontStyleConfig(final FontStyle fontStyle, final String definition) {
    String[] params = definition.split("\\s+");
    // physical base font?
    fontStyle.physicalFontNumber = getPhysicalFontNumber(params[0]);
    // no physical font means: possibly a special style
    if (fontStyle.physicalFontNumber == -1) {
      fillFontStyle(params[0], fontStyle);
    }
    // additional style parameters: alignment, colour
    for (int i = 1; i < params.length; i++) {
      final String param = params[i];
      if ("align".equalsIgnoreCase(param)) {
        i++;
        final String alignment = params[i];
        if ("left".equalsIgnoreCase(alignment)) {
          fontStyle.alignment = SwingConstants.LEFT;
        } else if ("right".equalsIgnoreCase(alignment)) {
          fontStyle.alignment = SwingConstants.RIGHT;
        } else if ("centre".equalsIgnoreCase(alignment)) {
          fontStyle.alignment = SwingConstants.CENTER;
        }
      } else if ("rgb".equalsIgnoreCase(param)) {
        i++;
        fontStyle.colour = StringUtils.stringToColor(params[i]);
      }
    }
  }

  /**
   * Reads and parses a StrongHelp !Configure file from given input stream.
   * 
   * @param is input stream providing !Configure content.
   */
  public void readConfig(final InputStream is) {
    // config has physical font definitions and style definitions
    try {
      final BufferedReader br = new BufferedReader(new InputStreamReader(is, "WINDOWS-1252"));
      // two-pass reading: read all physical fonts first, save style definitions for later and parse in second pass
      final List<String> styleLines = new ArrayList<>();
      String line = br.readLine();
      while (line != null && !StringUtils.startsWithIgnoreCase(line, "#End")) {
        line = line.trim();
        if (line.length() > 0 && !line.startsWith("#")) {
          final int equalsIndex = line.indexOf('=');
          // ignore non-font-definitions for now (e.g. background colour, pointer control, standard style...)
          if (StringUtils.startsWithIgnoreCase(line, "f") && equalsIndex > 0) {
            final String name = line.substring(1, equalsIndex).trim();
            final String definition = line.substring(equalsIndex + 1).trim();
            if (StringUtils.isOnlyDigits(name)) {
              final PhysicalFont pf = new PhysicalFont();
              pf.number = Integer.parseInt(name);
              parsePhysicalFontConfig(pf, definition);
              this.physicalFonts.add(pf);
            } else {
              styleLines.add(line);
            }
          } else if (StringUtils.startsWithIgnoreCase(line, "background")) {
            setBackgroundColourStr(line.substring(10).trim());
          }
        }
        line = br.readLine();
      }

      FontStyle stdStyle = null;
      for (final String styleLine : styleLines) {
        // we are guaranteed to only have true style lines, starting with f and an equals symbol, trimmed
        final int equalsIndex = styleLine.indexOf('=');
        String name = styleLine.substring(1, equalsIndex).trim();
        String definition = styleLine.substring(equalsIndex + 1).trim();
        final FontStyle fontStyle = new FontStyle();
        fontStyle.name = name;
        if ("std".equalsIgnoreCase(name)) {
          stdStyle = fontStyle;
        }
        parseFontStyleConfig(fontStyle, definition);
        this.fontStyles.add(fontStyle);
      }
      // post-process - duplicate std style - if it exists - as table style
      if (stdStyle != null) {
        FontStyle tableStyle = new FontStyle(stdStyle);
        tableStyle.name = "table";
        this.fontStyles.add(tableStyle);
      }
    } catch (UnsupportedEncodingException e) {
      // Will never happen, encoding is guaranteed to exist
      e.printStackTrace();
    } catch (IOException e) {
      // can this ever happen?
      e.printStackTrace();
    }
  }

  /**
   * Returns all style definitions (physical fonts and font styles) in a CSS style string.
   * 
   * @return styles as CSS style string.
   */
  public String getStyles() {
    return getStyles(getStylesMap());
  }

  /**
   * Returns a map of all style definitions (physical fonts and font styles).
   * 
   * @return map of all style definitions.
   */
  public Map<String, String> getStylesMap() {
    // special handling for body - introduce background colour if body is not explicitly defined
    boolean bodyDefined = false;
    final Map<String, String> stylesMap = new HashMap<>();
    for (final PhysicalFont physicalFont : this.physicalFonts) {
      stylesMap.put(physicalFont.getId(), physicalFont.toString());
    }
    for (final FontStyle fontStyle : this.fontStyles) {
      if ("body".equalsIgnoreCase(fontStyle.getFontStyleId())) {
        bodyDefined = true;
      }
      stylesMap.put(fontStyle.getFontStyleId(), fontStyle.toString());
    }
    if (!bodyDefined && this.backgroundColour != null) {
      FontStyle bodyFontStyle = new FontStyle();
      bodyFontStyle.name = "std";
      bodyFontStyle.backgroundColour = this.backgroundColour;
      stylesMap.put(bodyFontStyle.getFontStyleId(), bodyFontStyle.toString());
    }
    return stylesMap;
  }

  /**
   * Utility method to produce a CSS style string from a map of styles as e.g. returned by getStylesMap().
   * 
   * @param stylesMap input styles map.
   * @return CSS style string from given map of styles.
   */
  public static String getStyles(final Map<String, String> stylesMap) {
    final StringBuilder sb = new StringBuilder();
    for (final String key : stylesMap.keySet()) {
      sb.append(stylesMap.get(key));
    }
    return sb.toString();
  }

  /**
   * Returns the defined background colour from this font/style collection, or null.
   * 
   * @return background colour, null if undefined.
   */
  public Color getBackgroundColour() {
    return this.backgroundColour;
  }

  /**
   * Sets the background colour for this font/style collection.
   * 
   * @param background colour.
   */
  public void setBackgroundColour(final Color c) {
    this.backgroundColour = c;
  }

  /**
   * Sets the background colour for this font/style collection by its textual command.
   * 
   * @param colourDef string defining the background colour.
   */
  public void setBackgroundColourStr(final String colourDef) {
    // either a WIMP colour or an RGB colour
    // TODO tile with sprite not supported
    if (StringUtils.startsWithIgnoreCase(colourDef, "wimp")) {
      this.backgroundColour = getColourFromWimpColourNumber(colourDef.substring(4).trim());
    } else if (StringUtils.startsWithIgnoreCase(colourDef, "rgb")) {
      this.backgroundColour = StringUtils.stringToColor(colourDef.substring(4).trim());
    } else {
      System.err.println("Unsupported background parameter detected: "+colourDef);
    }
  }

  private static Color getColourFromWimpColourNumber(final String wimpColourNumberStr) {
    try {
      int wimpColourNumber = Integer.parseInt(wimpColourNumberStr);
      return WIMP_COLOURS[wimpColourNumber];
    } catch (final Exception ex) {
      // TODO logging
      ex.printStackTrace();
    }
    return DEFAULT_BACKGROUND;
  }
}
