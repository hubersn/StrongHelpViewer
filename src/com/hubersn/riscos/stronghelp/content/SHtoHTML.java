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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hubersn.util.string.StringUtils;

/**
 * Conversion of StrongHelp page data to HTML-3.2-as-Java-supports-it format.
 */
public class SHtoHTML {

  private static final boolean DEBUG = true;

  private static final PrintStream DEBUG_CHANNEL = System.out;

  /** URL protocol to use for internal links. */
  public static final String FAKE_PROTOCOL = "";
  //public static final String FAKE_PROTOCOL = "ftp://";

  // list character, found by experimentation, probably an artifact of the "wrong" encoding handling
  private static final char LIST_CHAR = 65533;

  private static final String BULLET = "&#8226;";

  // try to imitate StrongHelp table visualization - no border, and everything as compact as possible
  private static final String TABLE_PARAMS = " border=0 cellpadding=0 cellspacing=0";

  // try to imitate StrongHelp table visualization - text is always vertically top-aligned
  private static final String TABLE_ROW_PARAMS = " valign=\"top\"";

  private static final String NBSP = "&nbsp;";

  // try to imitate StrongHelp table visualization - pad with 3 nbsps at the end of every cell
  private static final String TABLE_CELL_END_PAD = "&nbsp;&nbsp;&nbsp;";

  private static final String SUBPAGE_TITLE_START = "<h2>";

  private static final String SUBPAGE_TITLE_END = "</h2>";

  private static enum WrapMode {
    On,
    Off,
    NoJoin
  }

  /**
   * Simple record-like collection to encapsulate row/cell information needed for postprocessing.
   */
  private static class TableRowInfo {
    /** Global offset to last td start tag. */
    private int lastTdTagOffset = -1;
    /** Index of last cell in this row. */
    private int lastTdColumnIndex = -1;
    /** Collection of td start tag global offsets. */
    private List<Integer> tdStartTagOffsets = new ArrayList<>();
  }

  /** Current offset into source string to convert. */
  private int offs;

  /** Source string to convert. */
  private String s;

  /** Holds the HTML result. */
  private final StringBuilder sb;

  /** Closing tag to write at the end. */
  private String ct;

  /** Currently active alignment. */
  private String currentAlignment;

  /** Currently in an italicised style run? */
  private boolean italicState = false;

  /** Currently in a bolded style run? */
  private boolean boldState = false;

  /** Currently in an underlined style run? */
  private boolean underlineState = false;

  /** Current char to be handled. */
  private char c;

  /** Previous char. */
  private char prevC;

  /** Possible pending end tag to write (used for fonts done by real commands). */
  private String pendingEndTag;

  /** Possible pending table cell end tag to write (used for fonts inside tables from #tab definition). */
  private String pendingTableCellEndTag;

  private boolean tabAsTableState;

  /** Collection of tag names on this page - perhaps useful later? */
  private List<String> listOfNamedTags;

  /** Currently active wrap mode. */
  private WrapMode activeWrapMode;

  /** Previously active wrap mode. */
  private WrapMode previousWrapMode;

  private String currentPrefix;

  private String currentPostfix;

  private StringBuilder activeOutputChannel;

  private String parentPage;

  private String rootPage;

  private int currentIndent;

  private String[] currentTableFontFormats;

  /** Track the current column number - needed for correct font handling. */
  private int currentColumn;

  private int lastTableMaxColumns;

  private Set<String> usedStyles;

  private SHFontConfig pageFontConfig;

  private List<TableRowInfo> currentTableCorrectionData;

  private TableRowInfo currentTableRowInfo;

  /** Indicates the mode e.g. for code where every space (if more than one) is inserted as non-breakable space. */
  private boolean spaceAsNonBreakableSpace;

  /**
   * Creates a new instance of SHtoHTML.
   * 
   * @param source StrongHelp source page data.
   */
  public SHtoHTML(final String source) {
    this(source, new SHFontConfig());
  }

  /**
   * Creates a new instance of SHtoHTML.
   * 
   * @param source StrongHelp source page data.
   * @param pageFontConfig to store additional font/style information while parsing page.
   */
  public SHtoHTML(final String source, final SHFontConfig pageFontConfig) {
    assert(pageFontConfig != null);
    this.s = source;
    this.pageFontConfig = pageFontConfig;
    this.c = 0;
    this.prevC = this.c;
    this.offs = 0;
    this.ct = "";
    this.sb = new StringBuilder();
    this.currentAlignment = "";
    this.italicState = false;
    this.boldState = false;
    this.underlineState = false;
    this.pendingEndTag = "";
    this.tabAsTableState = false;
    this.listOfNamedTags = new ArrayList<>();
    this.activeWrapMode = WrapMode.NoJoin;
    this.previousWrapMode = WrapMode.NoJoin;
    this.currentPrefix = "";
    this.currentPostfix = "";
    this.parentPage = "";
    this.rootPage = "";
    this.currentIndent = 0;
    this.currentTableFontFormats = null;
    this.currentColumn = 0;
    this.pendingTableCellEndTag = "";
    this.lastTableMaxColumns = -1;
    this.currentTableRowInfo = null;
    this.currentTableCorrectionData = new ArrayList<>();
    this.usedStyles = new HashSet<>();
    this.spaceAsNonBreakableSpace = false;
    setActiveOutputChannel(this.sb);
  }

  private void correctColspanValues() {
    // go through our table correction data structure and correct offsets from back to front
    d("Assessing colspan range...");
    int maxColspan = 0;
    for (int i = this.currentTableCorrectionData.size() - 1; i >= 0; i--) {
      final TableRowInfo tri = this.currentTableCorrectionData.get(i);
      if (this.lastTableMaxColumns > tri.lastTdColumnIndex) {
        if (tri.lastTdTagOffset < 0) {
          System.err.println("Strange tag offset encountered - table row info no "+i);
        } else {
          // currently, we just add a colspan=number_of_missing_columns_to_the_right parameter to the
          // last td tag, but this does not lead to a good emulation of StrongHelp tab-tables.
          maxColspan = Math.max(maxColspan, this.lastTableMaxColumns - tri.lastTdColumnIndex + 1);
          this.activeOutputChannel.insert(tri.lastTdTagOffset, " colspan=" + (this.lastTableMaxColumns - tri.lastTdColumnIndex + 1));
        }
      }
    }
    d("maxColspan for this table: "+maxColspan);
    this.currentTableCorrectionData = new ArrayList<>();
    this.currentTableRowInfo = null;
  }

  private void setActiveOutputChannel(final StringBuilder activeOutputChannel) {
    this.activeOutputChannel = activeOutputChannel;
  }

  /**
   * Writes the raw string (i.e. unescaped, no entity replacement) to active output channel.
   * 
   * @param out string to write to current output channel.
   */
  private void out(final String out) {
    this.activeOutputChannel.append(out);
  }

  /**
   * Writes the raw character (i.e. unescaped, no entity replacement) to active output channel.
   * 
   * @param out character to write to current output channel.
   */
  private void out(final char out) {
    this.activeOutputChannel.append(out);
  }

  private void outHorizontalLine() {
    // if in table context, put <hr> inside pending table cell and create a new row
    // if outside table context, a simple <hr> output will suffice
    if (this.tabAsTableState) {
      out("<hr>");
      outNewlineInsideTable();
    } else {
      out("<hr>\n");
    }
  }

  private void outHorizontalLineWithPercentageWidth(final int percentage) {
    // if in table context, put <hr> inside pending table cell and create a new row
    // if outside table context, a simple <hr> output will suffice
    if (this.tabAsTableState) {
      out("<hr " + this.currentAlignment + " width=\"" + percentage + "%\">");
      outNewlineInsideTable();
    } else {
      out("<hr " + this.currentAlignment + " width=\"" + percentage + "%\">\n");
    }
  }

  private void outNewlineInsideTable() {
    // always add one nbsp here to enforce a real table row in browsers like Firefox
    out(getAndClearPendingTableCellEndTag() + NBSP + "</td></tr>\n<tr" +  TABLE_ROW_PARAMS + "><td");
    handleFirstTableCellInRow();
  }

  private void outNewlineOutsideTable() {
    out("<br>\n");
  }

  private String getIndentString() {
    if (this.currentIndent == 0) {
      return "";
    }
    StringBuilder temp = new StringBuilder();
    for (int i = 0; i < this.currentIndent; i++) {
      temp.append(NBSP);
    }
    return temp.toString();
  }

  private String getAndClearPendingTableCellEndTag() {
    final String endTag = this.pendingTableCellEndTag;
    this.pendingTableCellEndTag = "";
    setSpaceAsNonBreakableSpace(false);
    return endTag;
  }

  private String getTableCellHTMLFontTag() {
    final String fontFormatForCurrentColumn = getFontFormatForCurrentColumn();
    if ("fstd".equalsIgnoreCase(fontFormatForCurrentColumn)) {
      this.pendingTableCellEndTag = "";
      return "";
    } else if ("fcode".equalsIgnoreCase(fontFormatForCurrentColumn)) {
      this.pendingTableCellEndTag = "</code>";
      setSpaceAsNonBreakableSpace(true);
      return "<code>";
    } else if ("f*".equalsIgnoreCase(fontFormatForCurrentColumn)) {
      this.pendingTableCellEndTag = "</b>";
      return "<b>";
    } else if ("f_".equalsIgnoreCase(fontFormatForCurrentColumn)) {
      this.pendingTableCellEndTag = "</u>";
      return "<u>";
    } else if ("f/".equalsIgnoreCase(fontFormatForCurrentColumn)) {
      this.pendingTableCellEndTag = "</i>";
      return "<i>";
    } else {
      if (!StringUtils.isEmptyOrNull(fontFormatForCurrentColumn)) {
        this.pendingTableCellEndTag = "</span>";
        return "<span id=\"style_"+fontFormatForCurrentColumn.substring(1) + "\">";
      }
    }
    return "";
  }

  private String getFontFormatForCurrentColumn() {
    return getFontFormatForColumn(this.currentColumn);
  }

  private String getFontFormatForColumn(final int col) {
    if (this.currentTableFontFormats == null) {
      return "";
    }
    if (col >= this.currentTableFontFormats.length) {
      return "";
    }
    return this.currentTableFontFormats[col];
  }

  private void setFontFormatForColumns(final String sourceParam) {
    if (StringUtils.isEmptyOrNull(sourceParam)) {
      this.currentTableFontFormats = null;
    }
    this.currentTableFontFormats = StringUtils.split(sourceParam, ",", true);
  }

  private void handleFirstTableCellInRow() {
    // keep record of table row information to later correct colspans of last table cell
    if (this.currentTableRowInfo != null) {
      this.currentTableCorrectionData.add(this.currentTableRowInfo);
    }
    this.currentTableRowInfo = new TableRowInfo();
    this.currentTableRowInfo.lastTdColumnIndex = 0;
    this.currentTableRowInfo.lastTdTagOffset = this.activeOutputChannel.length();
    this.currentTableRowInfo.tdStartTagOffsets.add(this.currentTableRowInfo.lastTdTagOffset);
    out(">");
    // always in first column now
    this.currentColumn = 0;
    out(getTableCellHTMLFontTag() + getIndentString());
  }

  /**
   * Returns the StrongHelp source page data converted to HTML-3.2-as-Java-supports-it format.
   * 
   * @return page data as HTML.
   */
  public String getHTML() throws SHContentParseException {
    try {
      // every file starts with an "implicit #tab command", but we start only when the first tabbed line arrives, or when #tab is done
      if (peekNextLineForTab()) {
        beginTable();
      }
      while (this.offs < this.s.length()) {
        nextChar();
        switch (this.c) {
          case 9:
            // TODO TAB support is not complete - if there is a line of text that has no TABs, it is required to be
            // not in the table - "Paragraphs without TAB's in them will not be influenced"
            // this could be done via a ridiculously high column span (idea by Steve Drain - colspan=99)?
            // does not visualize well in JEditorPane - count max column in every table and backtrack to colspan values?
            handleTab();
            break;
          case '#':
            // commands only valid on start of line
            if (this.prevC == 0 || this.prevC == 10 || this.prevC == '\n') {
              parseCommands();
            } else {
              out(this.c);
            }
            break;
          case '<':
            // certain characters preceding or following the link-start-character actually mean "go ahead, no link"
            if (this.prevC == '\\' || this.prevC == '<' || peekNextChar() == '=' || peekNextChar() == '-' || peekNextChar() == '<') {
              // escaped character, insert as entity
              append(this.c);
              // to handle \<<link> correctly, manipulated prevChar to something never checked
              this.prevC = '_';
            } else {
              parseLink();
            }
            break;
          case 10:
            if (this.tabAsTableState) {
              // once we are in a table structure, we keep it forever.
              outNewlineInsideTable();
            } else {
              // do not "break" if we have wrap=on and no lf or hash command follows
              if (this.activeWrapMode != WrapMode.On || peekNextChar() == 10 || peekNextChar() == '#') {
                outNewlineOutsideTable();
                out(getIndentString());
              }
              if (peekNextLineForTab()) {
                beginTable();
              }
            }
            break;
          case '/':
            // only valid if preceded by whitespace
            if (this.prevC == 32 || this.prevC == 10) {
              italic();
              break;
            }
            append('/');
            break;
          case '*':
            // only valid as bold if preceded by whitespace
            if (this.prevC == 32 || this.prevC == 10) {
              bold();
              break;
            }
            append('*');
            break;
          case '_':
            // only valid if preceded by whitespace
            if (this.prevC == 32 || this.prevC == 10) {
              underline();
              break;
            }
            append('_');
            break;
          case '{':
            inlineCommand();
            break;
          case '\\':
            // escape character - skip and add next as literal
            nextChar();
            if (this.c == 't') {
              // special semantics: \t is defined as being a TAB
              handleTab();
            } else {
              // might need to be replaced by entity
              append(this.c);
              if (this.c == '<') {
                // to handle \<<link> correctly, manipulated prevChar to something never checked
                this.c = '_';
              }
            }
            break;
          default:
            if (StringUtils.isEmptyOrNull(this.ct)) {
              // first start of text
              this.ct = "<br>";
            }
            // just fill in characters, maybe replaced by their entity
            append(this.c);
        }
      }
      // check if we had an alignment switch, and hence need to write out an end-div tag
      if (!StringUtils.isEmptyOrNull(this.currentAlignment)) {
        out("</div>\n");
      }
      if (this.tabAsTableState) {
        endTable();
      }
      out(this.ct);
      writeFooter();
    } catch (final Exception ex) {
      e("Error encountered...state: offs=" + this.offs
        + ", current result=\n" + this.sb.toString()
        + "\nfor source=\n" + this.s);
      throw new SHContentParseException("Fatal parse error - current state: offs=" + this.offs, ex);
    }
    return this.sb.toString();
  }

  private String getFragment(final String source) {
    final StringBuilder temp = new StringBuilder();
    parseFragment(source, temp);
    return temp.toString();
  }

  private void parseFragment(final String source, final StringBuilder output) {
    // save parts of main context
    final String savedString = this.s;
    final int savedOffs = this.offs;
    final char savedC = this.c;
    final char savedPrevC = this.prevC;

    // like if we were fresh from the start
    this.offs = 0;
    this.s = source;
    this.c = 0;
    this.prevC = 0;
    setActiveOutputChannel(output);

    // stripped-down version of main parse loop
    while (this.offs < this.s.length()) {
      nextChar();
      switch (this.c) {
        case '<':
          if (this.prevC == '\\' || this.prevC == '<' || peekNextChar() == '=' || peekNextChar() == '-' || peekNextChar() == '<') {
            // escaped character, insert as entity
            append(this.c);
            // to handle \<<link> correctly, manipulated prevChar to something never checked
            this.prevC = '_';
          } else {
            parseLink();
          }
          break;
        case '{':
          inlineCommand();
          break;
        case '\\':
          // escape character - skip and add next as literal
          nextChar();
          // might need to be replaced by entity
          append(this.c);
          break;
        default:
          // just fill in characters, maybe replaced by their entity
          append(this.c);
      }
    }

    // restore main context
    setActiveOutputChannel(this.sb);
    this.s = savedString;
    this.offs = savedOffs;
    this.c = savedC;
    this.prevC = savedPrevC;
    // caller is responsible to output the output
  }

  private void nextChar() {
    this.prevC = this.c;
    if (this.offs < this.s.length()) {
      this.c = this.s.charAt(this.offs++);
      if (this.c > 255) {
        // for debugging of "special characters", probably unmapped in WINDOWS-1252
        //d("Character encountered: "+(int)this.c+" at offset "+(this.offs - 1));
      }
    } else {
      this.c = 0;
    }
  }

  private char peekNextChar() {
    if (this.offs < this.s.length()) {
      return this.s.charAt(this.offs);
    }
    return 0;
  }

  private char peekNextNextChar() {
    if (this.offs < this.s.length() - 1) {
      return this.s.charAt(this.offs + 1);
    }
    return 0;
  }

  private boolean peekNextLineForTab() {
    int offset = this.offs;
    while (offset < this.s.length()) {
      char myChar = this.s.charAt(offset);
      if (myChar == 10) {
        // next newline, no tab found
        return false;
      }
      if (myChar == 9) {
        return true;
      }
      offset++;
    }
    // end of the world, no tab found
    return false;
  }

  /**
   * Replaces all reserved HTML characters with their entities and appends to target stringbuilder.
   * 
   * @param cToAdd character to add to stringbuilder possibly as entity.
   */
  private void append(final char cToAdd) {
    switch (cToAdd) {
      case 0:
        break;
      case ' ':
        // only handle multiple spaces as nbsp, or else wrapping won't work good enough
        if (this.spaceAsNonBreakableSpace && peekNextChar() == ' ') {
          out(NBSP);
        } else {
          out(' ');
        }
        break;
      case '"':
        out("&quot;");
        break;
      // apos not supported by Java HTML?
      //case '\'':
      //  out("&apos;");
      //  break;
      case '&':
        out("&amp;");
        break;
      case '<':
        out("&lt;");
        break;
      case '>':
        out("&gt;");
        break;
      case 'ü':
        out("&uuml;");
        break;
      case 'Ü':
        out("&Uuml;");
        break;
      case 'ö':
        out("&ouml;");
        break;
      case 'Ö':
        out("&Ouml;");
        break;
      case 'ä':
        out("&auml;");
        break;
      case 'Ä':
        out("&Auml;");
        break;
      case 'ß':
        out("&szlig;");
        break;
      case LIST_CHAR:
        out(BULLET);
        break;
      default:
        out(cToAdd);
    }
  }

  private void append(final String toAppend) {
    // always start with a paragraph
    if (StringUtils.isEmptyOrNull(this.ct)) {
      // first start of text
      //out("<p " + this.currentAlignment + ">");
      this.ct = "<br>";
    }
    for (int i = 0; i < toAppend.length(); i++) {
      // escape character handling
      char candidate = toAppend.charAt(i);
      if (candidate != '\\') {
        append(candidate);
      } else {
        // TODO handle possible overflow - how???
        if (i < toAppend.length() - 1) {
          i++;
          append(toAppend.charAt(i));
        }
      }
    }
  }

  private void updateMaxTableColumns() {
    this.lastTableMaxColumns = Math.max(this.lastTableMaxColumns, this.currentColumn);
  }

  private void handleTab() {
    if (this.tabAsTableState) {
      // only small padding in table cell for single bullet characters - why was this a good idea?
      if (this.prevC == LIST_CHAR) {
        out(NBSP + getAndClearPendingTableCellEndTag() + "</td><td>");
      } else {
        out(TABLE_CELL_END_PAD + getAndClearPendingTableCellEndTag() + "</td><td>");
      }
      this.currentColumn++;
      this.currentTableRowInfo.lastTdColumnIndex = this.currentColumn;
      this.currentTableRowInfo.lastTdTagOffset = this.activeOutputChannel.length() - 1;
      this.currentTableRowInfo.tdStartTagOffsets.add(this.currentTableRowInfo.lastTdTagOffset);
      updateMaxTableColumns();
      out(getTableCellHTMLFontTag());
      // consume all following tabs - they have no semantic meaning
      while (peekNextChar() == 9 || (peekNextChar() == '\\' && peekNextNextChar() == 't')) {
        nextChar();
        if (this.c != 9) {
          nextChar();
        }
      }
    } else {
      // TODO are there TAB characters outside of "tabAsTableState" mode?
      out(NBSP);
    }
  }

  /**
   * Check if the given source string is a valid simple style run for the
   * simple form of bold, underline and italic.
   * 
   * @param toCheck string to check.
   * @return valid simple style run?
   */
  private static boolean isValidSimpleFontStyleRun(final String toCheck) {
    if (StringUtils.isEmptyOrNull(toCheck)) {
      // TODO can this ever happen?
      return false;
    }
    for (int pos = 0; pos < toCheck.length(); pos++) {
      char charToCheck = toCheck.charAt(pos);
      // run must only consist of letters or spaces
      if (!Character.isLetter(charToCheck) && charToCheck != ' ') {
        return false;
      }
    }
    // first character must be a letter
    if (!Character.isLetter(toCheck.charAt(0))) {
      return false;
    }
    // last character must be a letter
    if (!Character.isLetter(toCheck.charAt(toCheck.length() - 1))) {
      return false;
    }
    return true;
  }

  private void underline() {
    handleFontStyle('_', "<u>", "</u>");
  }

  private void bold() {
    handleFontStyle('*', "<b>", "</b>");
  }

  private void italic() {
    handleFontStyle('/', "<i>", "</i>");
  }

  private void handleFontStyle(final char marker, final String startTag, final String endTag) {
    final int end = this.s.indexOf(marker, this.offs);
    if (end != -1) {
      // we have to check that all characters between the starting marker and the ending marker should
      // actually be styled according to the rules laid down in "Strong, Italic and Underline"
      final String possiblyStyledText = this.s.substring(this.offs, end);
      if (isValidSimpleFontStyleRun(possiblyStyledText)) {
        out(startTag);
        append(possiblyStyledText);
        out(endTag);
        this.offs = end + 1;
      } else {
        out(marker);
      }
    } else {
      // no end found, so surely just "add plain character"
      out(marker);
    }
  }

  // TODO fusion with parseCommands and/or table font style handling
  private void inlineCommand() {
    final int endCommand = this.s.indexOf('}', this.offs);
    if (endCommand >= 0) {
      final String inlineCommand = this.s.substring(this.offs, endCommand);
      //d("Inline command parsed: >" + command + "<");
      this.offs += inlineCommand.length() + 1;
      final String[] commands = StringUtils.split(inlineCommand, ";", true);
      for (final String command : commands) {
        if (command.equals("/")) {
          toggleItalic();
        } else if (command.equals("_")) {
          toggleUnderline();
        } else if (command.equals("*")) {
          toggleBold();
        } else if (StringUtils.startsWithIgnoreCase(command, "f")) {
          // various forms - fxx: - use font xx for the text following the colon
          //                 f - switch back to standard font
          //                 fxx - use font xx until switched back to standard
          final int endFontIndex = command.indexOf(':');
          if (endFontIndex > 0) {
            final String fontToUse = command.substring(1, endFontIndex);
            final String text = command.substring(endFontIndex + 1);
            if (fontToUse.equalsIgnoreCase("code")) {
              out("<code>");
              setSpaceAsNonBreakableSpace(true);
              append(text);
              setSpaceAsNonBreakableSpace(false);
              out("</code>");
            } else if (fontToUse.equalsIgnoreCase("std")) {
                append(text);
            } else if (StringUtils.startsWithIgnoreCase("h", fontToUse) && fontToUse.length() == 2) {
              // directly use h1..h6
              out("<" + fontToUse + ">");
              append(text);
              out("</" + fontToUse + ">\n");
            } else if (fontToUse.equalsIgnoreCase("*")) {
              out("<b>");
              append(text);
              out("</b>");
            } else if (fontToUse.equalsIgnoreCase("_")) {
              out("<u>");
              append(text);
              out("</u>");
            } else if (fontToUse.equalsIgnoreCase("/")) {
              out("<i>");
              append(text);
              out("</i>");
            } else {
              // TODO correct font handling for configured styles
              // use span tag
              out("<span id=\"+style_"+fontToUse+"\">");
              append(text);
              out("</span>");
            }
          } else if (command.equalsIgnoreCase("fcode")) {
            out("<code>");
            setSpaceAsNonBreakableSpace(true);
            this.pendingEndTag = "</code>";
          } else if (command.equalsIgnoreCase("f*")) {
            out("<b>");
            this.pendingEndTag = "</b>";
          } else if (command.equalsIgnoreCase("f_")) {
            out("<u>");
            this.pendingEndTag = "</u>";
          } else if (command.equalsIgnoreCase("f/")) {
            out("<i>");
            this.pendingEndTag = "</i>";
          } else if (command.equalsIgnoreCase("f")) {
            // reset to standard font
            writePendingEndTag();
          }
        } else if (StringUtils.startsWithIgnoreCase(command, "align")) {
          final String parameter = command.substring(5).trim();
          handleAlignment(parameter);
        } else {
          // unknown?
          d("Unidentified inline command: " + command);
          if (DEBUG) {
            out("{" + command + "}");
          }
        }
      }
    } else {
      e("!!! No inline command end found!");
    }
  }

  private void writePendingEndTag() {
    if (!StringUtils.isEmptyOrNull(this.pendingEndTag)) {
      out(this.pendingEndTag);
      this.pendingEndTag = "";
      setSpaceAsNonBreakableSpace(false);
    }
  }

  private void toggleItalic() {
    if (this.italicState) {
      out("</i>");
    } else {
      out("<i>");
    }
    this.italicState = !this.italicState;
  }

  private void toggleBold() {
    if (this.boldState) {
      out("</b>");
    } else {
      out("<b>");
    }
    this.boldState = !this.boldState;
  }

  private void toggleUnderline() {
    if (this.underlineState) {
      out("</u>");
    } else {
      out("<u>");
    }
    this.underlineState = !this.underlineState;
  }

  private void setSpaceAsNonBreakableSpace(final boolean spaceAsNonBreakableSpace) {
    this.spaceAsNonBreakableSpace = spaceAsNonBreakableSpace;
  }

  private boolean hasAlignmentChanged(final String newAlignmentString) {
    return !this.currentAlignment.equals(newAlignmentString);
  }

  private static String shAlignToHTMLAlign(final String alignmentParameter) {
    if (alignmentParameter.equalsIgnoreCase("centre")) {
      // TODO check if SH additionally does "center"
      return "align=\"center\"";
    } else if (alignmentParameter.equalsIgnoreCase("left")) {
      return "align=\"left\"";
    } else if (alignmentParameter.equalsIgnoreCase("right")) {
      return "align=\"right\"";
    } else if (StringUtils.isEmptyOrNull(alignmentParameter)) {
      return "align=\"left\"";
    }
    e("Illegal alignment >" + alignmentParameter +"<, continuing with no alignment.");
    return "";
  }

  private void handleAlignment(final String alignmentParameter) {
    // there is a current alignment stored, and if that changes, we write out a new alignment.
    final String htmlAlignment = shAlignToHTMLAlign(alignmentParameter);
    if (StringUtils.isEmptyOrNull(htmlAlignment)) {
      // error case - unknown alignment parameter
      return;
    }
    if (hasAlignmentChanged(htmlAlignment)) {
      // we already had an active alignment?
      if (!StringUtils.isEmptyOrNull(this.currentAlignment)) {
        out("</div>\n");
        // if in table, we need a new line - or not???
        if (this.tabAsTableState) {
          outNewlineInsideTable();
        }
      }
      // produce new alignment
      this.currentAlignment = htmlAlignment;
      // TODO or also write out left aligned divs?
      if (alignmentParameter.equals("left")) {
        this.currentAlignment = "";
      } else {
        out("<div " + this.currentAlignment + ">");
        // there was now at least one div written, due to the content of currentAlignment a </div>
        // will be written at the end in the main parse loop
      }
    }
  }

  private void parseCommands() {
    final String line = line(); // offs automatically adjusted
    if (line.startsWith(" ")) {
      addComment(line);
    } else {
      boolean tabCommandState = false;
      // there might be multiple commands separated by ";"
      d("Command detected: "+line);
      final String[] commands = StringUtils.split(line, ";", true);
      for (final String t : commands) {
        d("Iterating...command: " + t);
        if (StringUtils.startsWithIgnoreCase(t, "f")) {
          // various forms - fxx: - use font xx for the text following the colon
          //                 f - switch back to standard font
          //                 fxx - use font xx until switched back to standard
          // special predefined styles like fcode and fh1 are translated into their direct
          // HTML counterparts.
          final int endFontIndex = t.indexOf(':');
          if (endFontIndex > 0) {
            // text to write follows font to choose
            final String fontToUse = t.substring(1, endFontIndex);
            final String text = t.substring(endFontIndex + 1);
            // directly use code, h1..h6, bold/italic/underline
            if (fontToUse.equalsIgnoreCase("code")) {
              out("<code>");
              setSpaceAsNonBreakableSpace(true);
              append(text);
              setSpaceAsNonBreakableSpace(false);
              out("</code>");
            } else if (StringUtils.startsWithIgnoreCase(fontToUse, "h") && fontToUse.length() == 2) {
              out("<" + fontToUse + ">");
              append(text);
              out("</" + fontToUse + ">\n");
            } else {
              // use span to support font style
              out("<span id=\"+style_"+fontToUse+"\">");
              append(text);
              out("</span>");
              d("special font/style using span: >" + fontToUse + "<");
            }
          } else if (t.equalsIgnoreCase("fcode")) {
            out("<code>");
            setSpaceAsNonBreakableSpace(true);
            this.pendingEndTag = "</code>";
          } else if (t.equalsIgnoreCase("f")) {
            // reset to standard/previous font TODO support proper font stack
            writePendingEndTag();
          } else if (StringUtils.startsWithIgnoreCase(t, "fh") && t.length() == 3) {
            // h1..h6 (no error checking, could also be hx or h9...)
            out("<" + t.substring(1) + ">");
            this.pendingEndTag = "</" + t.substring(1) + ">";
          } else {
            // use span to support font style
            // TODO support physical fonts also?
            final String fontToUse = t.substring(1);
            this.usedStyles.add("style_" + fontToUse);
            out("<span id=\"+style_"+fontToUse+"\">");
            this.pendingEndTag = "</span>";
          }
        } else if (StringUtils.startsWithIgnoreCase(t, "line")) {
          final String parameter = t.substring(4).trim();
          if (StringUtils.isEmptyOrNull(parameter)) {
            outHorizontalLine();
          } else {
            try {
              int percentage = Integer.parseInt(parameter);
              outHorizontalLineWithPercentageWidth(percentage);
            } catch (final NumberFormatException nfe) {
              outHorizontalLine();
            }
          }
        } else if (StringUtils.startsWithIgnoreCase(t, "align")) {
          final String parameter = t.substring(5).trim();
          handleAlignment(parameter);
        } else if (StringUtils.startsWithIgnoreCase(t, "subpage")) {
          // close table if still active
          if (this.tabAsTableState) {
            endTable();
          }
          // separate clearly with double horizontal rule
          out("<hr>\n<hr>\n");
          // read title and insert
          String title = line();
          out(SUBPAGE_TITLE_START + title + SUBPAGE_TITLE_END + "\n");
          // add named link for navigation
          final String parameter = t.substring(7).trim().toLowerCase();
          out("<a name=\"" + parameter + "\"></a>\n");
        } else if (StringUtils.startsWithIgnoreCase(t, "tag")) {
          final String parameter = t.substring(3).trim();
          out("<a name=\"" + parameter + "\"></a>");
          this.listOfNamedTags.add(parameter);
        } else if (StringUtils.startsWithIgnoreCase(t, "below")) {
          // should we support "below", and how?
          d("Command ignored: #below");
        } else if (StringUtils.startsWithIgnoreCase(t, "bottom")) {
          // this aligns content to the bottom, impossible in simple HTML
          d("Command ignored: #bottom");
        } else if (StringUtils.startsWithIgnoreCase(t, "draw")) {
          // TODO support draw files
          d("Command ignored: #draw");
        } else if (StringUtils.startsWithIgnoreCase(t, "indent")) {
          // TODO indent is currently faked with non-breakable spaces
          // try to use CSS margin-left as an alternative
          final String parameter = t.substring(6).trim();
          if (StringUtils.isEmptyOrNull(parameter)) {
            this.currentIndent = 0;
          } else {
            try {
              this.currentIndent = (parameter.charAt(0) == '+') ? this.currentIndent + Integer.parseInt(parameter)
                  : Integer.parseInt(parameter);
            } catch (final Exception ex) {
              // reset indent
              this.currentIndent = 0;
            }
          }
        } else if (StringUtils.startsWithIgnoreCase(t, "spritefile")) {
          // TODO defines a spritefile, might be squashed
          d("Command ignored: #spritefile");
        } else if (StringUtils.startsWithIgnoreCase(t, "sprite")) {
          // TODO uses a sprite from a previously defined spritefile, might be squashed
          d("Command ignored: #sprite");
        } else if (StringUtils.startsWithIgnoreCase(t, "prefix")) {
          final String parameter = t.substring(6).trim();
          this.currentPrefix = parameter;
        } else if (StringUtils.startsWithIgnoreCase(t, "postfix")) {
          final String parameter = t.substring(7).trim();
          this.currentPostfix = parameter;
        } else if (StringUtils.startsWithIgnoreCase(t, "table")) {
          final String parameter = t.substring(5).trim();
          if (this.tabAsTableState) {
            endTable();
          }
          handleTable(parameter);
        } else if (StringUtils.startsWithIgnoreCase(t, "tab")) {
          tabCommandState = true;
          // check for special font formats in tab definition
          final String parameter = t.substring(3).trim();
          setFontFormatForColumns(parameter);
          // TODO is the following still needed? Wouldn't simple "end table if we are in table" suffice?
          if (this.tabAsTableState) {
            endTable();
            if (peekNextLineForTab()) {
              beginTable();
            }
          } else {
            if (peekNextLineForTab()) {
              beginTable();
            }
          }
        } else if (StringUtils.startsWithIgnoreCase(t, "wrap")) {
          // On = wrap line to fit window (lines are joined if not separated by blank line or #command line
          // Off = no wrap, no join - always wrap on encountering LF
          // NoJoin = wrap, but don't join
          final String parameter = t.substring(4).trim();
          if ("on".equalsIgnoreCase(parameter)) {
            this.previousWrapMode = this.activeWrapMode;
            this.activeWrapMode = WrapMode.On;
          } else if ("off".equalsIgnoreCase(parameter)) {
            // TODO not properly implemented...
            this.previousWrapMode = this.activeWrapMode;
            this.activeWrapMode = WrapMode.Off;
          } else if ("nojoin".equalsIgnoreCase(parameter)) {
            this.previousWrapMode = this.activeWrapMode;
            this.activeWrapMode = WrapMode.NoJoin;
          } else if (StringUtils.isEmptyOrNull(parameter)) {
            this.activeWrapMode = this.previousWrapMode;
          }
        } else if (StringUtils.startsWithIgnoreCase(t, "root")) {
          this.rootPage = t.substring(4).trim();
        } else if (StringUtils.startsWithIgnoreCase(t, "parent")) {
          this.parentPage = t.substring(6).trim();
        } else if (StringUtils.startsWithIgnoreCase(t, "background")) {
          this.pageFontConfig.setBackgroundColourStr(t.substring(10).trim());
        } else {
          e("Unknown command encountered:" + t);
          if (DEBUG) {
            out("#");
            append(t);
            out("<br>\n");
          }
        }
      }

      // post-command processing
      // we have consumed the final LF also, so explicitly check for a tabbed line next if we are not in tab command
      if (!tabCommandState && !this.tabAsTableState && peekNextLineForTab()) {
        beginTable();
      } else if (peekNextChar() != '#') {
        // next line is not again a command, so we need to indent, but only if not a table
        if (!this.tabAsTableState) {
          // d("End of command >"+line+"<, next line has no command, outputting nbsp to indent next line");
          out(getIndentString());
        }
      }
    }
  }

  /**
   * Starts a table because of a detected tab character or because of a #tab command.
   */
  private void beginTable() {
    this.tabAsTableState = true;
    out("<table" + TABLE_PARAMS + ">\n<tr" + TABLE_ROW_PARAMS + ">\n<td");
    this.currentColumn = 0;
    this.lastTableMaxColumns = 0;
    handleFirstTableCellInRow();
  }

  /**
   * End a currently open table.
   */
  private void endTable() {
    this.tabAsTableState = false;
    out(getAndClearPendingTableCellEndTag() + "</td></tr>\n</table>\n");
    this.currentTableCorrectionData.add(this.currentTableRowInfo);
    correctColspanValues();
    this.currentColumn = 0;
    this.currentTableFontFormats = null;
    this.lastTableMaxColumns = -1;
  }

  /**
   * Handles a complete #table-#endtable command section.
   */
  private void handleTable(final String parameter) {
    // #table-#endtable does not support column fonts/styles, so don't count currentColumn
    final List<String> data = new ArrayList<>();
    String line = line();
    while (!"#endtable".equalsIgnoreCase(line)) {
      data.add(line);
      line = line();
    }
    int noOfColumns = 0;
    int noOfLines = 0;
    if (StringUtils.startsWithIgnoreCase(parameter, "columns")) {
      noOfColumns = Integer.parseInt(parameter.substring(7).trim());
      noOfLines = data.size() / noOfColumns;
      if (data.size() % noOfColumns > 0) {
        noOfLines++;
      }
    } else if (StringUtils.startsWithIgnoreCase(parameter, "lines")) {
      noOfLines = Integer.parseInt(parameter.substring(5).trim());
      noOfColumns = data.size() / noOfLines;
      if (data.size() % noOfLines > 0) {
        noOfColumns++;
      }
    } else {
      noOfColumns = Integer.parseInt(parameter);
      noOfLines = data.size() / noOfColumns;
      if (data.size() % noOfColumns > 0) {
        noOfLines++;
      }
    }
    if (noOfColumns == 0 && noOfLines == 0) {
      e("Failed to parse #table structure!");
      return;
    }
    out("<table" + TABLE_PARAMS + ">\n");
    for (int row = 0; row < noOfLines; row++) {
      out("<tr" + TABLE_ROW_PARAMS + ">");
      for (int column = 0; column < noOfColumns; column++) {
        int dataIndex = noOfLines * column + row;
        out("<td>"); // no global column font support, but maybe inline styles
        if (dataIndex >= data.size()) {
          out(NBSP);
        } else {
          // mini-parse
          final String source = data.get(dataIndex);
          final StringBuilder target = new StringBuilder();
          parseFragment(source, target);
          out(target.toString() + TABLE_CELL_END_PAD);
        }
        out("</td>");
      }
      out("</tr>");
    }
    out("</table>\n");
  }

  private void parseLink() {
    // links are of form <Text=>Link Target> or <DirectLink>
    // external urls are of form <Text=>#URL url>
    // or even <TextThatIsAnURL=>#url>
    // we used to create fake URLs for internal targets with ftp protocol, but not anymore
    // TODO proper solution for internal links - why not localhost with a meaningful path?
    // why not relative links with ../?
    final int endText = this.s.indexOf("=>", this.offs);
    final int possibleEndLink = this.s.indexOf(">", this.offs);
    int endLink = possibleEndLink;
    if (endText == possibleEndLink - 1) {
      // complex form <Text=>Link>
      endLink = this.s.indexOf('>', endText + 2);
      String url = this.s.substring(endText + 2, endLink);
      String text = getFragment(this.s.substring(this.offs, endText));
      if (StringUtils.startsWithIgnoreCase(url, "#url ")) {
        url = url.substring(5);
      } else if (url.equalsIgnoreCase("#url")) {
        // undocumented in StrongHelp ref manual, but seen in the wild
        url = text;
      } else {
        url = FAKE_PROTOCOL + toFileLink(url);
      }
      out(getLinkTag(url, text));
    } else {
      // easy form <Link>
      String text = this.s.substring(this.offs, endLink);
      String link = text;
      // by observation (Basalt): Links get a cut off by e.g. space
      if (link.indexOf(' ') > 0) {
        link = link.substring(0, link.indexOf(' '));
      }
      String url = FAKE_PROTOCOL + this.currentPrefix + link + this.currentPostfix;
      out(getLinkTag(toFileLink(url), text));
    }
    //out("\n");
    this.offs = endLink + 1;
  }

  private void addComment(final String comment) {
    out("<!--" + comment + " -->\n");
    // offset already adjusted in line() call from parseCommands
  }

  private void list() {
    out("<ul>\n");
    // backtrack one character to include list bullet again
    this.offs--;
    do {
      // skip list char itself
      nextChar();
      out("<li>");
      // mini-parse like table lines
      String source = line().trim();
      StringBuilder listContent = new StringBuilder();
      parseFragment(source, listContent);
      out(listContent.toString());
      //append(line().trim());
      out("</li>\n");
    } while (peekNextChar() == LIST_CHAR);
    out("</ul>\n");
  }

  private String line() {
    final StringBuilder line = new StringBuilder();
    while (this.offs < this.s.length() && this.s.charAt(this.offs) != 10) {
      nextChar();
      line.append(this.c);
    }
    // also skip following newline
    if (this.offs < this.s.length() && this.s.charAt(this.offs) == 10) {
      nextChar();
    }
    return line.toString();
  }

  /**
   * Possibly write parent and root link if page had them defined.
   */
  private void writeFooter() {
    if (StringUtils.isEmptyOrNull(this.parentPage) && StringUtils.isEmptyOrNull(this.rootPage)) {
      return;
    }

    out("\n<br>\n<hr>\n<div align=\"left\">\n");
    if (!StringUtils.isEmptyOrNull(this.parentPage)) {
      out(getLinkTag(FAKE_PROTOCOL + toFileLink(this.parentPage), "[Parent]"));
    }
    if (!StringUtils.isEmptyOrNull(this.rootPage)) {
      out(getLinkTag(FAKE_PROTOCOL + toFileLink(this.rootPage), "[Root]"));
    }
    out("\n</div>");
  }

  private static String toFileLink(final String sourceUrl) {
    // general URL processing:
    // replace "." with "/
    // convert to lower-case
    // add ".html" at end
    // special case: starts with "." -> no .html and internal #
    if (sourceUrl.startsWith(".")) {
      return "#" + sourceUrl.substring(1).toLowerCase();
    }
    // check for special something: prefix - this needs a special relative link!
    boolean externalManualLink = sourceUrl.indexOf(':') > 0;
    // TODO there are manuals like the Assembly manual that use a syntax like BASIC.blabla to link to an external manual!
    String newUrl = sourceUrl.replace('.', '/').toLowerCase() + ".html";
    if (externalManualLink) {
      newUrl = newUrl.replace(':', '/');
      return "../" + newUrl;
    }
    return newUrl;
  }

  private static String getLinkTag(final String url, final String text) {
    return "<a href=\"" + url + "\">" + text + "</a>";
  }

  private static void d(final String s) {
    if (DEBUG) {
      DEBUG_CHANNEL.println(s);
    }
  }

  private static void e(final String s) {
    System.err.println(s);
  }
}
