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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.hubersn.riscos.stronghelp.Version;
import com.hubersn.riscos.util.encoding.Text;
import com.hubersn.util.string.StringUtils;

/**
 * Encapsulates StrongHelp page data and provides access to HTML-3.2-as-Java-supports-it format conversion.
 */
public class SHPage {

  private String fullText;

  private String title;

  private String body;

  private String htmlBody;

  private SHPageProviderIF pageProvider;

  private SHFontManager fontManager;

  private SHFontConfig pageFontConfig;

  /**
   * Creates a new instance of SHPage - HTML conversion is done on demand, not on instance creation.
   *
   * @param sourceText StrongHelp source text.
   * @param pageProvider callback to get arbitrary pages during preprocess/import resolve.
   * @param fontManager to provide font and style definitions for the created HTML.
   */
  public SHPage(final String sourceText, final SHPageProviderIF pageProvider, final SHFontManager fontManager) {
    // TODO page-local font config not yet filled
    this.pageFontConfig = new SHFontConfig();
    this.pageProvider = pageProvider;
    this.fontManager = fontManager;
    this.fullText = sourceText;
  }

  /**
   * Creates a new instance of SHPage - HTML conversion is done on demand, not on instance creation.
   *
   * @param sourceData StrongHelp DATA file block data.
   * @param pageProvider callback to get arbitrary pages during preprocess/import resolve.
   * @param fontManager to provide font and style definitions for the created HTML.
   */
  public SHPage(final byte[] sourceData, final SHPageProviderIF pageProvider, final SHFontManager fontManager) {
    this(getPageDataAsText(sourceData), pageProvider, fontManager);
  }

  private static String getPageDataAsText(final byte[] pageData) {
    // robust processing of possibly non-text data
    try {
      return Text.getText(pageData);
    } catch (final Exception ex) {
      return "Error parsing file\n" + ex.getMessage() + "\n";
    }
  }

  private void createTitleAndBody() {
    final int firstLineFeed = this.fullText.indexOf(10);
    if (firstLineFeed < 0) {
      this.title = "No title found.";
    } else {
      this.title = this.fullText.substring(0, firstLineFeed);
    }
    // without preprocess/import resolve this.body = this.fullText.substring(firstLineFeed + 1);
    this.body = preprocess(this.fullText.substring(firstLineFeed + 1));
    this.fullText = null; // no longer needed
  }

  /**
   * Returns the title of this page (i.e. the first line).
   * 
   * @return title of this page.
   */
  public String getTitle() {
    if (this.title == null) {
      createTitleAndBody();
    }
    return this.title;
  }

  /**
   * Returns the body of this page (i.e. everything below the first line) as source text.
   * 
   * @return body of this page as source text.
   */
  public String getBody() {
    if (this.body == null) {
      createTitleAndBody();
    }
    return this.body;
  }

  /**
   * Returns the body of this page converted to HTML-3.2-as-Java-supports-it format.
   * 
   * @return body of this page as HTML.
   */
  public String getBodyAsHTML() throws SHContentParseException {
    if (this.htmlBody == null) {
      this.htmlBody = createHTML();
    }
    return this.htmlBody;
  }

  private String createHTML() throws SHContentParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.US);
    String date = sdf.format(new Date());
    SHtoHTML htmlCreator = new SHtoHTML(getBody(), this.pageFontConfig);
    // create HTML to force parsing to fill all local styles before writing out header
    final String html = htmlCreator.getHTML();
    StringBuilder htmlPage = new StringBuilder("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">\n"
                                             + "<!-- Created " + date + " by StrongHelpReader " + Version.getVersionString() + " -->\n"
                                             + "<html>\n<head>\n<title>"+this.title+"</title>\n"
                                             + "<style>\n" + this.fontManager.getActiveStyles(this.pageFontConfig) + "\n"
                                             + "</style>\n</head>\n");
    htmlPage.append("<body>\n");
    htmlPage.append(html);
    htmlPage.append("\n</body>\n");
    htmlPage.append("</html>");
    return htmlPage.toString();
  }

  private String preprocess(final String input) {
    int possibleIncludeCommandOffset = input.indexOf("#");
    if (possibleIncludeCommandOffset < 0) {
      return input;
    }
    final StringBuilder result = new StringBuilder(input);
    do {
      // note from reading docs: prefix/postfix commands only manipulate links, not include path
      if (possibleIncludeCommandOffset >= 0) {
        char prevC = possibleIncludeCommandOffset == 0 ? 10 : result.charAt(possibleIncludeCommandOffset - 1);
        if (prevC == 10) {
          // read next characters and compare
          if (StringUtils.startsWithIgnoreCase(result.substring(possibleIncludeCommandOffset + 1), "include")) {
            // search for next LF
            int endOfLine = possibleIncludeCommandOffset + 8;
            while (endOfLine < result.length() && result.charAt(endOfLine) != 10) {
              endOfLine++;
            }
            final String fileToInclude = result.substring(possibleIncludeCommandOffset + 8, endOfLine).trim();
            final SHPage p = this.pageProvider.getSHPage(fileToInclude);
            // according to doc, it is valid to NOT find a page, we replace the include and just go on...
            if (p != null) {
              result.replace(possibleIncludeCommandOffset, endOfLine, p.fullText);
            } else {
              result.replace(possibleIncludeCommandOffset, endOfLine, "");
            }
            // allow recursive includes, continue from old offset!
          }
        }
      }
      possibleIncludeCommandOffset = result.indexOf("#", possibleIncludeCommandOffset + 1);
    } while (possibleIncludeCommandOffset >= 0);
    return result.toString();
  }

}
