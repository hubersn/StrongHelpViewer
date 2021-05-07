/*
 * (c) hubersn Software
 * www.hubersn.com
 */
package com.hubersn.ui.swing.lookandfeel;

import java.awt.Component;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.plaf.metal.OceanTheme;

/**
 * Collection of static Swing Look&Feel related utility methods.
 */
public class LookAndFeelUtils {

  /** Metal (aka Cross-Platform) Look and Feel - usually the Swing default. */
  public static final String METAL_LOOK_AND_FEEL = "javax.swing.plaf.metal.MetalLookAndFeel";
  /** Cross-Platform (aka Metal) Look and Feel - usually the Swing default. */
  public static final String CROSS_PLATFORM_LOOK_AND_FEEL = METAL_LOOK_AND_FEEL;
  /** Nimbus Look and Feel. */
  public static final String NIMBUS_LOOK_AND_FEEL = "Nimbus";
  /** Windows Look and Feel - only available on Windows platform! */
  public static final String WINDOWS_LOOK_AND_FEEL = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
  /** GTK Look and Feel - only available on unixy platforms. */
  public static final String GTK_LOOK_AND_FEEL = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
  /** Motif Look and Feel. */
  public static final String MOTIF_LOOK_AND_FEEL = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";

  public static final String STEEL_METAL_THEME = "steel";
  public static final String OCEAN_METAL_THEME = "ocean";
  public static final String DEFAULT_METAL_THEME = "defaultMetal";

  /**
   * Sets the Look&Feel associated with the given String - see constants of this class. All newly created Swing components will use that
   * Look&Feel.
   * 
   * @param lookAndFeel look&feel to switch to.
   */
  public static void setLookAndFeel(final String lookAndFeel) {
    try {
      if (NIMBUS_LOOK_AND_FEEL.equals(lookAndFeel)) {
        setNimbusOrCrossPlatformLookAndFeel();
      } else {
        UIManager.setLookAndFeel(lookAndFeel);
      }
    } catch (final Exception ex) {
      // silently ignore
    }
  }

  /**
   * Sets the given metal theme defined in this class' constants and updates to MetalLookAndFeel.
   * 
   * @param metalTheme metal theme to set.
   */
  public static void setMetalTheme(final String metalTheme) {
    if (DEFAULT_METAL_THEME.equals(metalTheme)) {
      setMetalTheme(new DefaultMetalTheme());
    } else if (STEEL_METAL_THEME.equals(metalTheme)) {
      setMetalTheme(new DefaultMetalTheme());
    } else if (OCEAN_METAL_THEME.equals(metalTheme)) {
      setMetalTheme(new OceanTheme());
    } else {
      throw new RuntimeException("Unknown metal theme >" + metalTheme + "< specified.");
    }
  }

  /**
   * Sets the given metal theme and updates to MetalLookAndFeel.
   * 
   * @param metalTheme metal theme to set.
   */
  public static void setMetalTheme(final MetalTheme metalTheme) {
    MetalLookAndFeel.setCurrentTheme(metalTheme);
    setLookAndFeel(METAL_LOOK_AND_FEEL);
  }

  /**
   * Updates all UI resources in the component tree starting with given rootComponent.
   * 
   * @param rootComponent root component to start updating the UI resources.
   */
  public static void updateToCurrentLookAndFeel(final Component rootComponent) {
    SwingUtilities.updateComponentTreeUI(rootComponent);
  }

  /**
   * Sets the system look and feel, but not if it would be "motif" - then try Nimbus if available, else generic cross platform L&F; respect
   * command line system property "swing.defaultlaf".
   */
  public static void setSystemLookAndFeel() {
    try {
      // first check if command line option is present to force L&F
      if (System.getProperty("swing.defaultlaf") != null) {
        return;
      }

      final String systemLnfName = UIManager.getSystemLookAndFeelClassName();
      if (systemLnfName == null || systemLnfName.indexOf("Motif") >= 0) {
        // either change to Nimbus, or leave it alone (usually Metal)
        setNimbusOrCrossPlatformLookAndFeel();
        return;
      }
      UIManager.setLookAndFeel(systemLnfName);
    } catch (final Exception ex) {
      // silently ignore
    }
  }

  private static void setNimbusOrCrossPlatformLookAndFeel() throws UnsupportedLookAndFeelException {
    try {
      boolean lookAndFeelSet = false;
      for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if (NIMBUS_LOOK_AND_FEEL.equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          lookAndFeelSet = true;
          break;
        }
      }
      if (!lookAndFeelSet) {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      }
    } catch (final Exception e) {
      throw new UnsupportedLookAndFeelException("Nimbus Look&Feel not supported - probably running on JRE older than 1.6.0_10");
    }
  }

  private LookAndFeelUtils() {
    // no instances allowed
  }

}