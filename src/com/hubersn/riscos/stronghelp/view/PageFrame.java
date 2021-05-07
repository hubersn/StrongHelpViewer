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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import com.hubersn.riscos.stronghelp.content.SHPage;
import com.hubersn.riscos.stronghelp.content.SHPageProviderIF;
import com.hubersn.ui.swing.gadgets.HTextView;

/**
 * A frame visualizing a StrongHelp page.
 */
public class PageFrame {

  // show all three representations in a tabbed view
  private static final boolean DEBUG = true;

  private JFrame frame;

  private JPanel cp;

  private JTabbedPane debugView;

  private SHPageProviderIF pageProvider;

  private PagePanel strongHelpView;

  public PageFrame(final SHPage page, final SHPageProviderIF pageProvider) {
    this.pageProvider = pageProvider;
    this.frame = new JFrame();
    this.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.cp = new JPanel(new BorderLayout());
    this.frame.setContentPane(this.cp);
    this.frame.setTitle(page.getTitle());
    create(page);
    this.frame.pack();
    debugPrefSizes("after pack");
    // limit width to something sensible
    final Dimension prefSizeOfHelpViewScrollPane = this.strongHelpView.getStrongHelpView().getPreferredSize();
    this.frame.setSize(prefSizeOfHelpViewScrollPane.width + 50, prefSizeOfHelpViewScrollPane.height + 100);
    this.frame.setSize(Math.min(600, this.frame.getWidth()), this.frame.getHeight());
    this.frame.setLocation(0,0);
    debugPrefSizes("after resizing");
    //SwingUtils.centerComponent(this.frame, true);
  }

  private void debugPrefSizes(final String desc) {
    if (DEBUG) {
      System.out.println(desc);
      printPrefSize("Frame prefSize", this.frame);
      printPrefSize("ContentPane prefSize", this.cp);
      printPrefSize("HelpView prefSize", this.strongHelpView.getStrongHelpView());
    }
  }

  private void printPrefSize(final String desc, final Component c) {
    System.out.println(desc + " " + c.getPreferredSize());
  }

  public void show() {
    this.frame.setVisible(true);
  }

  public JFrame getFrame() {
    return this.frame;
  }

  private void create(final SHPage page) {
    this.strongHelpView = new PagePanel(page, this.pageProvider);
    if (DEBUG) {
      HTextView plainView = new HTextView();
      plainView.setMonospacedText(page.getBody());
      String htmlSourceText = "<html><body>Error!</body></html>";
      try {
        htmlSourceText = page.getBodyAsHTML();
      } catch (final Exception ex) {
        ex.printStackTrace();
      }
      HTextView htmlSourceView = new HTextView();
      htmlSourceView.setMonospacedText(htmlSourceText);
      this.debugView = new JTabbedPane(SwingConstants.BOTTOM);
      this.debugView.addTab("StrongHelp", this.strongHelpView.getStrongHelpView());
      if (!this.strongHelpView.isValid()) {
        this.debugView.setBackgroundAt(0, Color.RED);
      }
      this.debugView.addTab("HTML", htmlSourceView);
      this.debugView.addTab("Source", plainView);
      this.cp.add(this.debugView, BorderLayout.CENTER);
    } else {
      this.cp.add(this.strongHelpView.getStrongHelpView(), BorderLayout.CENTER);
    }
  }
}
