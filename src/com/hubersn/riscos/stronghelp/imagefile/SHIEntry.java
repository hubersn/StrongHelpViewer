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

import com.hubersn.riscos.util.fs.LoadExec;

/**
 * Represents an entry inside a StrongHelp image file - this is an abstraction
 * to generalize directories, files and links.
 */
public abstract class SHIEntry {

  private static final char DIR_SEP = '/';

  private String id; // first four bytes as a String

  private String name;

  private int offset;

  private LoadExec loadExec;

  private SHIEntry parent;

  /**
   * Creates a new instance of SHIEntry, representing an entry with given id, name, offset and LoadExec.
   * 
   * @param id id of entry - "DIR$" for directories, "LINK" for link files, "DATA" for files.
   * @param name name of entry.
   * @param offset byte offset into source image file binary.
   * @param loadExec RISC OS load/exec pair.
   */
  public SHIEntry(final String id, final String name, final int offset, final LoadExec loadExec) {
    this.id = id;
    this.name = name;
    this.offset = offset;
    this.loadExec = loadExec;
  }

  /**
   * Returns the id of this entry - "DIR$" for directories, "LINK" for link files, "DATA" for files.
   * 
   * @return id for this entry.
   */
  public String getId() {
    return this.id;
  }

  /**
   * Returns this entry's name.
   * 
   * @return name of this entry.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Returns the byte offset into the source image file binary.
   * 
   * @return byte offset into source image file binary.
   */
  public int getOffset() {
    return this.offset;
  }

  public LoadExec getLoadExec() {
    return this.loadExec;
  }

  public void setParent(final SHIEntry parent) {
    this.parent = parent;
  }

  /**
   * Returns the parent entry for this entry, null if there is none (e.g. we are root).
   * 
   * @return parent entry for this entry, or null.
   */
  public SHIEntry getParent() {
    return this.parent;
  }

  /**
   * Returns true if this entry is currently the root of an SHI structure.
   * 
   * @return true if entry is root of SHI structure.
   */
  public boolean isRoot() {
    return this.parent == null;
  }

  /**
   * Returns the full pathname of this entry in the form "$/parent/entry".
   * 
   * @return full pathname of this entry.
   */
  public String getPathname() {
    if (!isRoot()) {
      return getParent().getPathname() + DIR_SEP + getName();
    }
    return getName();
  }

  /**
   * Returns the lookup name of this entry, i.e. the name that is used
   * inside manuals to link to each other.
   * 
   * @return lookup name of this entry.
   */
  public String getLookupName() {
    String result = "";
    if (!isRoot()) {
      result = getParent().getLookupName() + getName();
    }
    return result;
  }

  /**
   * Returns the root for this entry in this SHI structure.
   * 
   * @return root for this entry in this SHI structure.
   */
  public SHIEntry getRoot() {
    SHIEntry root = this;
    while (!root.isRoot()) {
      root = getParent();
    }
    return root;
  }

  /**
   * Convenience method that searches for the entry with the given name, starting from
   * the root directory of the SHI structure of which this entry is part of.
   * 
   * @param entryName name of entry to search for
   * @return entry matching name, or null if no matching entry is found.
   */
  public SHIEntry getGlobalEntry(final String entryName) {
    SHIDir root = (SHIDir)getRoot();
    return getGlobalDirEntry(root, entryName);
  }

  private SHIEntry getGlobalDirEntry(final SHIDir dirToCheck, final String entryName) {
    SHIEntry entry = dirToCheck.getEntry(entryName);
    if (entry == null) {
      for (SHIEntry possibleDirToCheck : dirToCheck.getEntries()) {
        if (possibleDirToCheck instanceof SHIDir) {
          SHIEntry possibleResult = getGlobalDirEntry((SHIDir)possibleDirToCheck, entryName);
          if (possibleResult != null) {
            return possibleResult;
          }
        }
      }
    }
    return entry;
  }

  @Override
  public String toString() {
    return getPathname() + " [" + this.loadExec + "]";
  }
}
