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

package com.hubersn.riscos.stronghelp.view;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.hubersn.riscos.stronghelp.content.SHContentParseException;
import com.hubersn.riscos.stronghelp.content.SHPage;
import com.hubersn.riscos.stronghelp.content.SHPageProviderIF;
import com.hubersn.riscos.stronghelp.content.SHtoHTML;
import com.hubersn.util.string.StringUtils;

/**
 * A panel visualizing a StrongHelp page.
 */
public class PagePanel extends JPanel {

  private static final long serialVersionUID = 1L;

  private static final String parseErrorSubstitutionText = "<html><body>Parse Error!</body></html>";

  private JEditorPane strongHelpView;

  private JScrollPane sp;

  private boolean valid;

  public PagePanel(final SHPage sourcePage, final SHPageProviderIF pageProvider) {
    super(new BorderLayout());
    this.valid = false;
    final HyperlinkListener hyper = new HyperlinkListener() {
      private String stripHTMLExtension(final String s) {
        final int extensionIndex = s.indexOf(".html");
        if (extensionIndex >= 0) {
          return s.substring(0, extensionIndex);
        }
        return s;
      }
      @Override
      public void hyperlinkUpdate(HyperlinkEvent he) {
        if (he.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          if (he.getURL() != null) {
            // we only get here if we have a true URL with a valid protocol, e.g. http or ftp
            if (he.getURL().getProtocol().equals(SHtoHTML.FAKE_PROTOCOL)) {
              if (he.getURL().getHost() != null) {
                final SHPage newPage = pageProvider.showSHPage(he.getURL().getHost());
                if (newPage != null) {
                  updateContent(newPage);
                }
              }
            } else {
              try {
                Desktop.getDesktop().browse(he.getURL().toURI());
              } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
          } else {
            // no valid URL, internal link - so we look into description
            final String pageName = he.getDescription();
            if (!StringUtils.isEmptyOrNull(pageName)) {
              // might be an internal link if it starts with a hash
              if (pageName.startsWith("#")) {
                scrollToReference(pageName.substring(1));
              } else {
                final SHPage newPage = pageProvider.showSHPage(stripHTMLExtension(pageName));
                if (newPage != null) {
                  updateContent(newPage);
                }
              }
            }
          }
        }
      }
    };
    this.strongHelpView = new JEditorPane();
    this.strongHelpView.setEditable(false);
    this.strongHelpView.setContentType("text/html");
    this.strongHelpView.addHyperlinkListener(hyper);
    updateContent(sourcePage);
    this.sp = new JScrollPane(this.strongHelpView);
    add(this.sp, BorderLayout.CENTER);
    validate();
  }

  private void updateContent(final SHPage newPage) {
    try {
      this.strongHelpView.setText(newPage.getBodyAsHTML());
      this.valid = true;
      // scroll to start
      this.strongHelpView.setCaretPosition(0);
    } catch (SHContentParseException e1) {
      e1.printStackTrace();
      this.strongHelpView.setText(parseErrorSubstitutionText);
      this.valid = false;
    }
  }

  /**
   * Scrolls the current content view to the given reference.
   *
   * @param reference reference to scroll to.
   */
  private void scrollToReference(final String reference) {
    if (reference != null && !reference.equals("")) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          PagePanel.this.strongHelpView.scrollToReference(reference);
        }
      });
    }
  }

  public JComponent getStrongHelpView() {
    return this.sp;
  }

  @Override
  public boolean isValid() {
    return this.valid;
  }
}
