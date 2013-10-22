/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
package org.cougaar.planning.servlet.data.xml;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * for outputting xml tags
 *
 * @since 1/24/01
 **/
public class XMLWriter extends FilterWriter{

  //Variables:
  public boolean prettyPrint = false;
  protected int level=0;

  //Constructors:
  ///////////////

  public XMLWriter(Writer out){
    super(out);
  }

  public XMLWriter(Writer out, boolean prettyPrint){
    super(out);
    this.prettyPrint=prettyPrint;
  }

  //Members:
  //////////

  //Helpers:

  protected void indent() throws IOException{
    if(prettyPrint){
    for(int i=0;i<level;i++)
      out.write(' ');
    }
  }

  protected static String noNull(String s){
    return s==null?"":s;
  }

  protected void writeAttr(String attr, String val) throws IOException{
    out.write(" ");
    out.write(attr);
    out.write("=\"");
    out.write(noNull(val));
    out.write("\"");
  }

  //For External consumption:

  public void writeHeader()throws IOException{
    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
  }

  //No attrs single tags:

  public void sitag(String tag)throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    out.write("/>");
  }

  public void sitagln(String tag)throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    out.write("/>\n");
  }

  //One attr single tags:

  public void sitag(String tag, String a1, String v1)throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    out.write("/>");
  }

  public void sitagln(String tag, String a1, String v1)throws IOException{
    sitag(tag,a1,v1);
    out.write('\n');
  }

  //Two attr single tags:

  public void sitag(String tag, String a1, String v1,
		    String a2, String v2)throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    out.write("/>");
  }

  public void sitagln(String tag, String a1, String v1,
		      String a2, String v2)throws IOException{
    sitag(tag, a1, v1, a2, v2);
    out.write('\n');
  }

  //Three attr single tags:

  public void sitagln(String tag, String a1, String v1,
		    String a2, String v2,
		    String a3, String v3)throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    out.write("/>\n");
  }
  //4 attr single tags:

  public void sitagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4
		      )throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    out.write("/>\n");
  }
  //5 attr single tags:

  public void sitagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5
		      )throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);    
    writeAttr(a5,v5);
    out.write("/>\n");
  }
  //6 attr single tags:

  public void sitagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6
		      )throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    out.write("/>\n");
  }
  //7 attr single tags:

  public void sitagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7
		      )throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    out.write("/>\n");
  }
  //8 attr single tags:

  public void sitagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7,
		      String a8, String v8
		      )throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    writeAttr(a8,v8);
    out.write("/>\n");
  }
  //9 attr single tags:

  public void sitagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7,
		      String a8, String v8,
		      String a9, String v9
		      )throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    writeAttr(a8,v8);
    writeAttr(a9,v9);
    out.write("/>\n");
  }
  //10 attr single tags:

  public void sitagln(String tag, 
		      String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7,
		      String a8, String v8,
		      String a9, String v9,
		      String a10, String v10
		      )throws IOException{
    indent();
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    writeAttr(a8,v8);
    writeAttr(a9,v9);
    writeAttr(a10,v10);
    out.write("/>\n");
  }

  //No attrs open and close tags:

  public void optag(String tag)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    out.write('>');
  }

  public void optagln(String tag)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    out.write(">\n");
  }

  public void cltag(String tag)throws IOException{
    level--;
    indent();
    out.write("</");
    out.write(tag);
    out.write('>');
  }

  public void cltagln(String tag)throws IOException{
    level--;
    indent();
    out.write("</");
    out.write(tag);
    out.write(">\n");
  }

  public void cltaglnNI(String tag)throws IOException{
    level--;
    out.write("</");
    out.write(tag);
    out.write(">\n");
  }

  //One Attr optagln:
  public void optagln(String tag, String a1, String v1)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    out.write(">\n");
  }

  //2 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    out.write(">\n");
  }

  //3 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    out.write(">\n");
  }

  //4 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    out.write(">\n");
  }

  //5 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    out.write(">\n");
  }

  //6 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    out.write(">\n");
  }

  //7 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    out.write(">\n");
  }

  //8 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7,
		      String a8, String v8)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    writeAttr(a8,v8);
    out.write(">\n");
  }

  //9 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7,
		      String a8, String v8,
		      String a9, String v9)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    writeAttr(a8,v8);
    writeAttr(a9,v9);
    out.write(">\n");
  }

  //10 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7,
		      String a8, String v8,
		      String a9, String v9,
		      String a10, String v10)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    writeAttr(a8,v8);
    writeAttr(a9,v9);
    writeAttr(a10,v10);
    out.write(">\n");
  }

  //11 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7,
		      String a8, String v8,
		      String a9, String v9,
		      String a10, String v10,
		      String a11, String v11)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    writeAttr(a8,v8);
    writeAttr(a9,v9);
    writeAttr(a10,v10);
    writeAttr(a11,v11);
    out.write(">\n");
  }


  //12 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7,
		      String a8, String v8,
		      String a9, String v9,
		      String a10, String v10,
		      String a11, String v11,
		      String a12, String v12)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    writeAttr(a8,v8);
    writeAttr(a9,v9);
    writeAttr(a10,v10);
    writeAttr(a11,v11);
    writeAttr(a12,v12);
    out.write(">\n");
  }

  //13 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7,
		      String a8, String v8,
		      String a9, String v9,
		      String a10, String v10,
		      String a11, String v11,
		      String a12, String v12,
		      String a13, String v13)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    writeAttr(a8,v8);
    writeAttr(a9,v9);
    writeAttr(a10,v10);
    writeAttr(a11,v11);
    writeAttr(a12,v12);
    writeAttr(a13,v13);
    out.write(">\n");
  }

  //14 Attr optagln:
  public void optagln(String tag, String a1, String v1,
		      String a2, String v2,
		      String a3, String v3,
		      String a4, String v4,
		      String a5, String v5,
		      String a6, String v6,
		      String a7, String v7,
		      String a8, String v8,
		      String a9, String v9,
		      String a10, String v10,
		      String a11, String v11,
		      String a12, String v12,
		      String a13, String v13,
		      String a14, String v14)throws IOException{
    indent();
    level++;
    out.write('<');
    out.write(tag);
    writeAttr(a1,v1);
    writeAttr(a2,v2);
    writeAttr(a3,v3);
    writeAttr(a4,v4);
    writeAttr(a5,v5);
    writeAttr(a6,v6);
    writeAttr(a7,v7);
    writeAttr(a8,v8);
    writeAttr(a9,v9);
    writeAttr(a10,v10);
    writeAttr(a11,v11);
    writeAttr(a12,v12);
    writeAttr(a13,v13);
    writeAttr(a14,v14);
    out.write(">\n");
  }

  //No Attrs tagln by data:

  public void tagln(String tag, String data)throws IOException{
    optag(tag);
    out.write(noNull(data));
    cltaglnNI(tag);
  }

  public void tagln(String tag, int i)throws IOException{
    tagln(tag,Integer.toString(i));
  }

  public void tagln(String tag, long l)throws IOException{
    tagln(tag,Long.toString(l));
  }

  public void tagln(String tag, float f)throws IOException{
    tagln(tag,Float.toString(f));
  }

  public void tagln(String tag, double d)throws IOException{
    tagln(tag,Double.toString(d));
  }

  //From FilterWriter:
  //------------------

  public void write(int c) throws IOException{
    out.write(c);
  }

  public void write(char[] cbuf,
		    int off,
		    int len)
    throws IOException{
    out.write(cbuf,off,len);
  }

  public void write(String str,
		    int off,
		    int len)
    throws IOException{
    out.write(str,off,len);
  }

  public void flush()throws IOException{
    out.flush();
  }

  public void close()throws IOException{
    out.close();
  }
}
