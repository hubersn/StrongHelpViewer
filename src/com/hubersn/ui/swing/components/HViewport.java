/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.ui.swing.components;

import java.awt.Point;

import javax.swing.JViewport;

/**
 * Viewport supporting (super)power mode to inhibit automatic Swing scrolling behaviour.
 */
public class HViewport extends JViewport {

  private static final long serialVersionUID = 1L;

  private boolean powerMode;

  private boolean superPowerMode = false;

  /** Creates new instance of HViewport. */
  public HViewport() {
    super();
    this.powerMode = false;
  }

  public void setPowerMode(boolean powerMode) {
    this.powerMode = powerMode;
  }

  public void setSuperPowerMode(boolean superPowerMode) {
    this.superPowerMode = superPowerMode;
  }

  public boolean isPowerMode() {
    return this.powerMode;
  }

  @Override
  public void setViewPosition(Point p) {
    if (!isPowerMode()) {
      super.setViewPosition(p);
    } else {
      if (!this.superPowerMode) {
        super.setViewPosition(p);
      }
    }
  }

  public void setPowerViewPosition(Point p) {
    super.setViewPosition(p);
  }
}