/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.ui.swing.gadgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import com.hubersn.ui.swing.components.HScrollPane;

/**
 * Implements a simple text view panel (with scroll pane) which shows monospaced text, e.g. for simple ReadMe visualisation.
 */
public class HTextView extends JPanel {

  private static final long serialVersionUID = 1L;

  private static final Color textColour = Color.black;

  private HScrollPane sp;

  private JTextPane tp;

  /**
   * Creates a new instance of HTextView.
   */
  public HTextView() {
    super(new BorderLayout());
    this.tp = new JTextPane();
    this.tp.setEditable(false);
    this.sp = new HScrollPane(this.tp);
    add(this.sp, BorderLayout.CENTER);
  }

  public void setMonospacedText(final String text) {
    final SimpleAttributeSet textAttributes = new SimpleAttributeSet();
    textAttributes.addAttribute(StyleConstants.FontFamily, "monospaced");
    textAttributes.addAttribute(StyleConstants.FontSize, Integer.valueOf(12));
    textAttributes.addAttribute(StyleConstants.Foreground, textColour);
    final MutableAttributeSet aset = this.tp.getInputAttributes();
    aset.addAttributes(textAttributes);
    final int offset = this.tp.getDocument().getLength();
    try {
      this.tp.getDocument().insertString(offset, text, aset);
    } catch (final BadLocationException ex) {
      ex.printStackTrace();
    }
    this.tp.setCaretPosition(0);
  }

  public void setMonospacedTextAsHTML(final String text) {
    this.tp.setContentType("text/html");
    this.tp.setText("<html><body><pre>" + text + "</pre></body></html>");
    this.tp.setCaretPosition(0);
  }

  /**
   * Load the text from the given resource via classloader in UTF-8.
   * 
   * @param resourceName
   */
  public void loadMonospacedText(final String resourceName) {
    InputStream is = HTextView.class.getResourceAsStream(resourceName);
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      final StringBuilder out = new StringBuilder();
      char[] buffer = new char[1024];
      for (;;) {
        int rsz = br.read(buffer, 0, buffer.length);
        if (rsz < 0) {
          break;
        }
        out.append(buffer, 0, rsz);
      }
      setMonospacedText(out.toString());
      br.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }
}