/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.ui.swing.components;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

/**
 * Simple extension to JScrollPane supporting special operation modes to give more control over scrolling,
 * default values for unit and block increments, as well as wheel scroll event delegation to a possible
 * JScrollPane/HScrollPane parent.
 * 
 * <p>The operation modes are:
 * <ul>
 * <li>standard - behave like JScrollPane</li>
 * <li>only explicit scrolling - cut off automatic scrolling behaviour from Swing components, only allow explicit calls via setExplicitViewPosition</li>
 * </ul>
 * </p>
 */
public class HScrollPane extends JScrollPane {

  private static final long serialVersionUID = 1L;

  /** Marker to signal "dynamically calculated block increment". */
  public static final int DYNAMIC_BLOCK_INCREMENT = -1;

  private static int defaultHorizontalUnitIncrement = 16;

  private static int defaultVerticalUnitIncrement = 16;

  private static int defaultHorizontalBlockIncrement = DYNAMIC_BLOCK_INCREMENT;

  private static int defaultVerticalBlockIncrement = DYNAMIC_BLOCK_INCREMENT;

  private int horizontalUnitIncrement;

  private int verticalUnitIncrement;

  private int horizontalBlockIncrement;

  private int verticalBlockIncrement;

  private boolean explicitOnlyScrollMode;

  private boolean wheelEventDelegationActive = true;

  /**
   * Creates a new instance of HScrollPane - no special scroll mode, wheel event delegation is active.
   */
  public HScrollPane() {
    super();
    setDefaults();
  }

  /**
   * Creates a new instance of HScrollPane with the given component as content - no special scroll mode, wheel event delegation is active.
   *
   * @param c content component.
   */
  public HScrollPane(Component c) {
    super(c);
    setDefaults();
  }

  /**
   * Creates a new instance of HScrollPane with the given component as content and the given scrollbar policies - no special scroll mode,
   * wheel event delegation is active.
   *
   * @param c content component.
   * @param vsbPolicy vertical scrollbar policy.
   * @param hsbPolicy horizontal scrollbar policy.
   */
  public HScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
    super(view, vsbPolicy, hsbPolicy);
    setDefaults();
  }

  /**
   * Creates a new instance of HScrollPane with the given scrollbar policies - no special scroll mode, wheel event delegation is active.
   *
   * @param vsbPolicy vertical scrollbar policy.
   * @param hsbPolicy horizontal scrollbar policy.
   */
  public HScrollPane(int vsbPolicy, int hsbPolicy) {
    super(vsbPolicy, hsbPolicy);
    setDefaults();
  }

  /**
   * Returns plain text for given AdjustmentEvent constant for debug purposes.
   *
   * @param value adjustment type.
   * @return plain text.
   */
  protected String getAdjustmentType(int value) {
    switch (value) {
      case AdjustmentEvent.BLOCK_DECREMENT:
        return "Block Decrement";
      case AdjustmentEvent.BLOCK_INCREMENT:
        return "Block Increment";
      case AdjustmentEvent.TRACK:
        return "Track";
      case AdjustmentEvent.UNIT_DECREMENT:
        return "Unit Decrement";
      case AdjustmentEvent.UNIT_INCREMENT:
        return "Unit Increment";
      default:
    }
    return "unknown: " + value;
  }

  /**
   * Sets the current defaults as current values for this instance - no special scroll mode, wheel event delegation is active.
   */
  protected void setDefaults() {
    setHorizontalBlockIncrement(defaultHorizontalBlockIncrement);
    setVerticalBlockIncrement(defaultVerticalBlockIncrement);

    setHorizontalUnitIncrement(defaultHorizontalUnitIncrement);
    setVerticalUnitIncrement(defaultVerticalUnitIncrement);

    setExplicitOnlyScrollMode(false);

    setWheelEventDelegationActive(true);
  }

  @Override
  public JScrollBar createHorizontalScrollBar() {
    final JScrollBar scrollBar = super.createHorizontalScrollBar();

    scrollBar.addAdjustmentListener(new AdjustmentListener() {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent ev) {
        final Point p = getViewPosition();
        p.x = ev.getValue();
        setExplicitViewPosition(p);
      }
    });

    scrollBar.setUnitIncrement(getHorizontalUnitIncrement());
    return scrollBar;
  }

  @Override
  public JScrollBar createVerticalScrollBar() {
    final JScrollBar scrollBar = super.createVerticalScrollBar();
    scrollBar.addAdjustmentListener(new AdjustmentListener() {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent ev) {
        final Point p = getViewPosition();
        p.y = ev.getValue();
        setExplicitViewPosition(p);
      }
    });

    scrollBar.setUnitIncrement(getVerticalUnitIncrement());

    return scrollBar;
  }

  @Override
  public JViewport createViewport() {
    final HViewport aViewport = new HViewport();
    aViewport.setExplicitOnlyScrollMode(this.explicitOnlyScrollMode);
    return aViewport;
  }

  public void setHorizontalBlockIncrement(int blockIncrement) {
    this.horizontalBlockIncrement = blockIncrement;
  }

  public int getHorizontalBlockIncrement() {
    return this.horizontalBlockIncrement;
  }

  public void setVerticalBlockIncrement(int blockIncrement) {
    this.verticalBlockIncrement = blockIncrement;
  }

  public int getVerticalBlockIncrement() {
    return this.verticalBlockIncrement;
  }

  public void setBlockIncrements(int blockIncrement) {
    setHorizontalBlockIncrement(blockIncrement);
    setVerticalBlockIncrement(blockIncrement);
  }

  public void setHorizontalUnitIncrement(int unitIncrement) {
    this.horizontalUnitIncrement = unitIncrement;
    if (getHorizontalScrollBar() != null) {
      getHorizontalScrollBar().setUnitIncrement(unitIncrement);
    }
  }

  public int getHorizontalUnitIncrement() {
    return this.horizontalUnitIncrement;
  }

  public void setVerticalUnitIncrement(int unitIncrement) {
    this.verticalUnitIncrement = unitIncrement;
    if (getVerticalScrollBar() != null) {
      getVerticalScrollBar().setUnitIncrement(unitIncrement);
    }
  }

  public int getVerticalUnitIncrement() {
    return this.verticalUnitIncrement;
  }

  public void setUnitIncrements(int unitIncrement) {
    setHorizontalUnitIncrement(unitIncrement);
    setVerticalUnitIncrement(unitIncrement);
  }

  public static void setDefaultHorizontalBlockIncrement(int blockIncrement) {
    defaultHorizontalBlockIncrement = blockIncrement;
  }

  public int getDefaultHorizontalBlockIncrement() {
    return defaultHorizontalBlockIncrement;
  }

  public static void setDefaultVerticalBlockIncrement(int blockIncrement) {
    defaultVerticalBlockIncrement = blockIncrement;
  }

  public int getDefaultVerticalBlockIncrement() {
    return defaultVerticalBlockIncrement;
  }

  public static void setDefaultHorizontalUnitIncrement(int unitIncrement) {
    defaultHorizontalUnitIncrement = unitIncrement;
  }

  public int getDefaultHorizontalUnitIncrement() {
    return defaultHorizontalUnitIncrement;
  }

  public static void setDefaultVerticalUnitIncrement(int unitIncrement) {
    defaultVerticalUnitIncrement = unitIncrement;
  }

  public int getDefaultVerticalUnitIncrement() {
    return defaultVerticalUnitIncrement;
  }

  public void setExplicitOnlyScrollMode(boolean explicitOnlyScrollMode) {
    this.explicitOnlyScrollMode = explicitOnlyScrollMode;
    if (getHViewport() != null) {
      getHViewport().setExplicitOnlyScrollMode(explicitOnlyScrollMode);
    }
  }

  public boolean isExplicitOnlyScrollMode() {
    return this.explicitOnlyScrollMode;
  }

  public void setViewPosition(Point p) {
    if (!isExplicitOnlyScrollMode()) {
      getViewport().setViewPosition(p);
    }
  }

  public Point getViewPosition() {
    return getViewport().getViewPosition();
  }

  public void setExplicitViewPosition(Point p) {
    if (getHViewport() != null) {
      getHViewport().setExplicitViewPosition(p);
    } else {
      getViewport().setViewPosition(p);
    }
  }

  public HViewport getHViewport() {
    final JViewport result = super.getViewport();
    if (result instanceof HViewport) {
      return (HViewport) result;
    }
    return null;
  }

  public void setWheelEventDelegationActive(final boolean wheelEventDelegationActive) {
    this.wheelEventDelegationActive = wheelEventDelegationActive;
  }

  public boolean isWheelEventDelegationActive() {
    return this.wheelEventDelegationActive;
  }

  @Override
  protected void processMouseWheelEvent(final MouseWheelEvent e) {
    Point oldPosition = getViewport().getViewPosition();
    super.processMouseWheelEvent(e);

    if (this.isWheelEventDelegationActive() && getViewport().getViewPosition().y == oldPosition.y) {
      delegateToParent(e);
    }
  }

  private void delegateToParent(final MouseWheelEvent e) {
    JScrollPane ancestor = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
    if (ancestor != null) {
      MouseWheelEvent converted = null;
      for (MouseWheelListener listener : ancestor.getMouseWheelListeners()) {
        listener.mouseWheelMoved(converted != null ? converted
            : (converted = (MouseWheelEvent) SwingUtilities
                .convertMouseEvent(this, e, ancestor)));
      }
    }
  }
}