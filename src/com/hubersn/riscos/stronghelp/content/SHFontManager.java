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

import java.util.HashMap;
import java.util.Map;

/**
 * Manages multiple layers of font and style configs, three layers are standard:
 * <ul>
 * <li>!Configure in main StrongHelp</li>
 * <li>!Configure in manual itself</li>
 * <li>additional page config</li>
 * </ul>
 */
public class SHFontManager {

  private SHFontConfig strongHelpConfig;

  private SHFontConfig manualConfig;

  /**
   * Creates a new instance of SHFontManager.
   */
  public SHFontManager() {
    super();
    // default is empty to avoid nullchecks later.
    this.strongHelpConfig = new SHFontConfig();
    this.manualConfig = new SHFontConfig();
  }

  /**
   * Sets the given FontConfig as the global font/style layer.
   * 
   * @param strongHelpConfig font config for global font/style layer.
   */
  public void setStrongHelpConfig(final SHFontConfig strongHelpConfig) {
    this.strongHelpConfig = strongHelpConfig;
  }

  /**
   * Sets the given FontConfig as the manual-specific font/style layer.
   * 
   * @param manualConfig font config for manual-specific font/style layer.
   */
  public void setManualConfig(final SHFontConfig manualConfig) {
    this.manualConfig = manualConfig;
  }

  /**
   * Returns an HTML CSS style representation of the given page-specific font
   * config, with fallback to the other layers..
   * 
   * @param pageConfig page-specific font/style config
   * @return HTML CSS representation of all active styles.
   */
  public String getActiveStyles(final SHFontConfig pageConfig) {
    // merge together all maps to a new map, the create the style representation string
    Map<String, String> activeStylesMap = new HashMap<>();
    activeStylesMap.putAll(this.strongHelpConfig.getStylesMap());
    activeStylesMap.putAll(this.manualConfig.getStylesMap());
    activeStylesMap.putAll(pageConfig.getStylesMap());
    return SHFontConfig.getStyles(activeStylesMap);
  }

}
