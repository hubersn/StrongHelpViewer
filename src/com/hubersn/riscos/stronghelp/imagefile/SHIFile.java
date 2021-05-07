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
 * Represents a (data) file inside a StrongHelp image file.
 */
public class SHIFile extends SHIEntry {

  private int length;

  /**
   * Creates a new instance of SHIFile, representing a file with given id,
   * name, offset, LoadExec and length.
   * 
   * @param id id of entry - "DATA" for files.
   * @param name name of file.
   * @param offset byte offset into source image file binary.
   * @param loadExec RISC OS load/exec pair.
   * @param length length of file in bytes.
   */
  public SHIFile(final String id, final String name, final int offset, final LoadExec loadExec, final int length) {
    super(id, name, offset, loadExec);
    this.length = length;
  }

  /**
   * Returns the binary data content of this file.
   * 
   * @param sourceData source data of whole image file.
   * @return binary data content of this file.
   */
  public byte[] getData(final Memory sourceData) {
    return sourceData.getDataSlice(getOffset() + 8, this.length);
  }

  @Override
  public String toString() {
    return super.toString() + "|length="+this.length;
  }

}
