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

package com.hubersn.riscos.stronghelp.imagefile;

import java.util.ArrayList;
import java.util.List;

import com.hubersn.riscos.util.fs.LoadExec;

/**
 * Represents a directory inside a StrongHelp image file.
 */
public class SHIDir extends SHIEntry {

  private List<SHIEntry> children = new ArrayList<>();

  /**
   * Creates a new instance of SHIDir, representing a directory with given id, name, offset and LoadExec.
   * 
   * @param id id of entry - "DIR$" for directories.
   * @param name name of directory.
   * @param offset byte offset into source image file binary.
   * @param loadExec RISC OS load/exec pair.
   */
  public SHIDir(String id, String name, int offset, final LoadExec loadExec) {
    super(id, name, offset, loadExec);
  }

  /**
   * Adds a new entry as child to this directory.
   * 
   * @param child new entry to add.
   */
  public void add(final SHIEntry child) {
    this.children.add(child);
    child.setParent(this);
  }

  /**
   * Returns the entry at the given index of this directory.
   * 
   * @param index entry index.
   * @return entry at given index.
   */
  public SHIEntry getEntryAt(final int index) {
    return this.children.get(index);
  }

  /**
   * Returns an entry matching the given name, or null if not found - direct
   * children are searched for first, then a recursive pre-order search is
   * done.
   * 
   * @param name name of entry to search for.
   * @return entry with given name, or null if not found.
   */
  public SHIEntry getEntry(final String name) {
    for (final SHIEntry entry : this.children) {
      // all internal links were converted to lower case!
      if (name.equalsIgnoreCase(entry.getName()) || name.equalsIgnoreCase(entry.getLookupName())) {
        return entry;
      }
    }
    // not yet found? check all dirs recursively for entry
    for (final SHIEntry entry : this.children) {
      if (entry instanceof SHIDir) {
        SHIEntry matchingEntry = ((SHIDir)entry).getEntry(name);
        if (matchingEntry != null) {
          return matchingEntry;
        }
      }
    }
    // not found - caller must handle null
    return null;
  }

  /**
   * Returns an array of all entries of this directory, might be an empty
   * array, but never null.
   * 
   * @return array of all entries of this directory.
   */
  public SHIEntry[] getEntries() {
    return this.children.toArray(new SHIEntry[0]);
  }
}
