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
import com.hubersn.util.memory.Memory;

/**
 * A link file as a stand-in for a real file.
 */
public class SHILink extends SHIFile {

  private String linkName;

  private String myName;

  /**
   * Creates a new instance of SHILink, representing a link to a file with given id,
   * name, offset and LoadExec.
   * 
   * @param id id of entry - "LINK" for link files.
   * @param name name of link file.
   * @param offset byte offset into source image file binary.
   * @param loadExec RISC OS load/exec pair.
   */
  public SHILink(final String id, final String name, final int offset, final LoadExec loadExec) {
    super(id, name, offset, loadExec, 0);
    final int linkIndex = name.indexOf('>');
    if (linkIndex > 0) {
      this.myName = name.substring(0, linkIndex);
      this.linkName = name.substring(linkIndex + 1);
    } else {
      // is this an error?
      System.err.println("Error: not really a link file???");
      this.myName = name;
      this.linkName = name;
    }
  }

  /**
   * Returns the name of this link file.
   * 
   * @return name of link file.
   */
  @Override
  public String getName() {
    return this.myName;
  }

  /**
   * Returns the data of the linked file.
   * 
   * @param sourceData source data.
   * @return data of linked file.
   */
  @Override
  public byte[] getData(final Memory sourceData) {
    final SHIEntry entry = getGlobalEntry(this.linkName);
    if (entry instanceof SHIFile) {
      return ((SHIFile)entry).getData(sourceData);
    }
    // should an error be thrown???
    System.err.println("Strange link file data - linked file "+this.linkName+" not found???");
    return new byte[0];
  }

  @Override
  public String toString() {
    return super.toString() + "|linkTo="+getId();
  }

}
