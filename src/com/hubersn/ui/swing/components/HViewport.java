/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.ui.swing.components;

import java.awt.Point;

import javax.swing.JViewport;

/**
 * Viewport supporting explicit-only scroll mode to inhibit automatic Swing scrolling behaviour.
 */
public class HViewport extends JViewport {

  private static final long serialVersionUID = 1L;

  private boolean explicitOnlyScrollMode;

  /** Creates new instance of HViewport. */
  public HViewport() {
    super();
    this.explicitOnlyScrollMode = false;
  }

  public void setExplicitOnlyScrollMode(boolean explicitOnlyScrollMode) {
    this.explicitOnlyScrollMode = explicitOnlyScrollMode;
  }

  public boolean isExplicitOnlyScrollMode() {
    return this.explicitOnlyScrollMode;
  }

  @Override
  public void setViewPosition(Point p) {
    if (!isExplicitOnlyScrollMode()) {
      super.setViewPosition(p);
    }
  }

  public void setExplicitViewPosition(Point p) {
    super.setViewPosition(p);
  }
}