/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.ui.swing.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseListener;
import java.lang.reflect.Method;
import java.net.URI;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

/**
 * Collection of Swing utility methods.
 */
public class SwingUtils {

  /**
   * Returns the current screen size.
   * 
   * @return screen size in pixels.
   */
  public static int getScreenWidth() {
    return Toolkit.getDefaultToolkit().getScreenSize().width;
  }

  /**
   * Returns the current screen height.
   * 
   * @return screen height in pixels.
   */
  public static int getScreenHeight() {
    return Toolkit.getDefaultToolkit().getScreenSize().height;
  }

  /**
   * Sizes the given window to the given width and height, but to maximum screen width and screen height.
   * 
   * @param window window.
   * @param width target width.
   * @param height target height.
   */
  public static void sizeWindow(Window window, int width, int height) {
    window.setSize(Math.min(getScreenWidth(), width), Math.min(getScreenHeight(), height));
  }

  /**
   * Centers window on screen.
   *
   * @param win window to center.r
   */
  public static void centerWindow(final Window win) {
    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    final double screenwidth = screenSize.getWidth();
    final double screenheight = screenSize.getHeight();
    final Point target = new Point();
    final double width = win.getWidth();
    final double height = win.getHeight();
    target.setLocation((screenwidth - width) / 2, (screenheight - height) / 2);
    win.setLocation(target);
    return;
  }

  /**
   * Adds the given action a triggered by keystroke k to component c.
   *
   * @param c component
   * @param a action
   * @param k keystroke
   */
  public static void addKeytriggeredActionToComponent(final JComponent c, final Action a, final KeyStroke k) {
    c.getActionMap().put(a.getValue(Action.NAME), a);
    c.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(k, a.getValue(Action.NAME));
  }

  /**
   * Set given string as clipboard content.
   *
   * @param str string
   */
  public static void setClipboard(final String str) {
    final Toolkit tk = Toolkit.getDefaultToolkit();
    final Clipboard c = tk.getSystemClipboard();
    final StringSelection ss = new StringSelection(str);
    c.setContents(ss, ss);
    return;
  }

  public static void centerComponent(final Component c) {
    centerComponent(c, true);
  }

  public static void centerComponent(final Component c, final boolean keepOnScreen) {
    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    final double screenwidth = screenSize.getWidth();
    final double screenheight = screenSize.getHeight();
    final Point target = new Point();
    final double width = c.getWidth();
    final double height = c.getHeight();
    double location_x = (screenwidth - width) / 2;
    double location_y = (screenheight - height) / 2;
    if (keepOnScreen) {
      location_x = Math.max(0.0d, location_x);
      location_y = Math.max(0.0d, location_y);
    }
    target.setLocation(location_x, location_y);
    c.setLocation(target);
    return;
  }

  public static void centerComponentInComponent(final Component c, final Component pc) {
    // parent check - available and visible? if not, early exit
    if (pc == null || !pc.isShowing() || !pc.isVisible()) {
      centerComponent(c, true);
      return;
    }
    final Rectangle rect = c.getBounds();

    rect.x = (pc.getWidth() - rect.width) / 2;
    rect.y = (pc.getHeight() - rect.height) / 2;

    if (c instanceof Window) {
      rect.x += pc.getLocationOnScreen().getX();
      rect.y += pc.getLocationOnScreen().getY();
    } // if

    keepOnScreen(rect);
    c.setLocation(rect.x, rect.y);
  }

  public static void addMouseListenerToAllLabels(Container c, MouseListener mlst) {
    Component[] comps = c.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof JLabel) {
        ((JLabel) comps[i]).addMouseListener(mlst);
      } else if (comps[i] instanceof Container) {
        addMouseListenerToAllLabels((Container) comps[i], mlst);
      }
    }
  }

  /**
   * Launches the default system browser with the given URL. Uses Desktop (Java 1.6ff) or magic (Java 1.5). Slightly adapted from free code
   * BareBonesBrowserLaunch.
   * 
   * @param url URL to launch
   */
  public static void launchURL(String url) {
    try {
      URI uri = java.net.URI.create(url);
      // the following is equivalent to "java.awt.Desktop.getDesktop().browse(new URI(url))"
      Method getDesktop = null;
      Method browse = null;
      Class<?> c = Class.forName("java.awt.Desktop");
      Method ms[] = c.getMethods();
      for (int i = 0; i < ms.length; i++) {
        if (ms[i].getName().compareTo("getDesktop") == 0) {
          getDesktop = ms[i];
        } else if (ms[i].getName().compareTo("browse") == 0) {
          browse = ms[i];
        }
      }
      // if methods are not found, an exception is thrown and 1.5 fallback takes over
      Object[] desktopParams = new Object[0];
      Object desktop = getDesktop.invoke(null, desktopParams);
      Object[] browseParams = new Object[1];
      browseParams[0] = uri;
      browse.invoke(desktop, browseParams);
    } catch (Throwable t) {
      // invoke magic...
      final String[] browsers = { "google-chrome",
          "firefox",
          "opera",
          "konqueror",
          "epiphany",
          "seamonkey",
          "galeon",
          "kazehakase",
          "mozilla" };
      String osName = System.getProperty("os.name");
      try {
        if (osName.startsWith("Mac OS")) {
          Class.forName("com.apple.eio.FileManager").getDeclaredMethod("openURL", new Class[] { String.class })
              .invoke(null, new Object[] { url });
        } else if (osName.startsWith("Windows")) {
          Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
        } else { // assume Unix or Linux
          boolean found = false;
          for (String browser : browsers) {
            if (!found) {
              found = Runtime.getRuntime().exec(new String[] { "which", browser }).waitFor() == 0;
              if (found) {
                Runtime.getRuntime().exec(new String[] { browser, url });
              }
            }
          }
        }
      } catch (Throwable t2) {
        // silence is golden...
      }
    }
  }

  /**
   * Makes the bounds reside within the screen bounds. Also resolves intersection with other monitors, i.e. the bounds will not cross
   * multiple screens. It also takes operating system dependant objects like taskbars into account.
   *
   * @param bounds e.g. from {@link JFrame#getBounds()}
   */
  private static Rectangle keepOnScreen(final Rectangle bounds) {
    return keepOnScreen(bounds, bounds);
  }

  /**
   * Makes the bounds reside within the screen bounds. Also resolves intersection with other monitors, i.e. the bounds will not cross
   * multiple screens. It also takes operating system dependent objects like task bars into account.
   *
   * @param bounds e.g. from {@link JFrame#getBounds()}
   */
  private static Rectangle keepOnScreen(final Rectangle bounds, final Rectangle parentBounds) {

    final int deviceIndex = getNearestDeviceIndex(parentBounds);

    final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    final GraphicsDevice[] gs = ge.getScreenDevices();
    final GraphicsConfiguration gc = gs[deviceIndex].getDefaultConfiguration();
    final Rectangle deviceBounds = gc.getBounds();
    // get screen insets (due to taskbars etc.) and adjust the screen bounds
    // accordingly
    /*
     * Bug under windows: if you have device 2 on the left, and device 1 on
     * the right, there is a bug in the Toolkit.getScreenInsets-method: the
     * taskbar is seen as being on every screen if you have it on device 1,
     * and on no screen if you have it on device 2. There is no way to fix
     * it through Java code. See also:
     * http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6899304
     */
    final Insets deviceInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

    subtractInsets(deviceBounds, deviceInsets);
    // bound the given bounds into the adjusted device bounds
    if (bounds.y < deviceBounds.y) {
      bounds.y = deviceBounds.y;
    }
    if (bounds.x < deviceBounds.x) {
      bounds.x = deviceBounds.x;
    }
    if (deviceBounds.width >= bounds.width) {
      if (bounds.x + bounds.width > deviceBounds.x + deviceBounds.width) {
        bounds.x = deviceBounds.x + deviceBounds.width - bounds.width;
      }
    }
    if (deviceBounds.height >= bounds.height) {
      if (bounds.y + bounds.height > deviceBounds.y + deviceBounds.height) {
        bounds.y = deviceBounds.y + deviceBounds.height - bounds.height;
      }
    }

    return bounds;
  }

  /**
   * This method returns the index of the screen device the bounds are closest to.
   *
   * @param bounds
   * @return the screen device index the bounds are closest to
   * @see #setFrameMaximizedAtDeviceIndex(JFrame, int)
   */
  private static int getNearestDeviceIndex(final Rectangle bounds) {
    final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    final GraphicsDevice[] gs = ge.getScreenDevices();
    double minDistance = Double.MAX_VALUE;
    int nearestIndex = -1;
    final Point center = new Point((int) bounds.getCenterX(), (int) bounds.getCenterY());
    for (int i = 0; i < gs.length; i++) {
      final GraphicsDevice gd = gs[i];
      final GraphicsConfiguration gc = gd.getDefaultConfiguration();
      final Rectangle deviceBounds = gc.getBounds();
      deviceBounds.grow(-1, -1);
      final double distance = getPointRectangleDistanceSquared(center, deviceBounds);
      if (distance < minDistance) {
        minDistance = distance;
        nearestIndex = i;
      }
    }
    return nearestIndex;
  }

  /**
   * @param point a point
   * @param rect a rectangle
   * @return the squared distance from point to rectangle
   */
  private static double getPointRectangleDistanceSquared(final Point point, final Rectangle rect) {
    double dx = Math.abs(point.x - rect.x - rect.width / 2);
    double dy = Math.abs(point.y - rect.y - rect.height / 2);
    final double cx = rect.width / 2;
    final double cy = rect.height / 2;
    dx = dx > cx ? (dx - cx) * (dx - cx) : 0;
    dy = dy > cy ? (dy - cy) * (dy - cy) : 0;
    return dx + dy;
  }

  /**
   * Subtracts the given insets from the bounds.
   *
   * @param bounds the bounds e.g. a frame.getBounds()
   * @param insets the insets e.g. a Toolkit.getDefaultToolkit().getScreenInsets(GraphicsConfiguration)
   */
  private static void subtractInsets(final Rectangle bounds, final Insets insets) {
    bounds.x += insets.left;
    bounds.y += insets.top;
    bounds.width -= insets.right + insets.left;
    bounds.height -= insets.bottom + insets.top;
  }

  /**
   * Adds the insets of the given component to the given base.
   * 
   * @param base base dimension to add insets to.
   * @param c component to add insets from.
   * @return new Dimension representing base dimension with added insets of component.
   */
  public static Dimension withInsets(final Dimension base, final JComponent c) {
    final Insets cInsets = c.getInsets();
    return new Dimension(base.width + cInsets.left + cInsets.right,
                         base.height + cInsets.top + cInsets.bottom);
  }

  private SwingUtils() {
    // never instantiate.
  }
}