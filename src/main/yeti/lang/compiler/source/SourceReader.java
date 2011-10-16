// ex: se sts=4 sw=4 expandtab:

/*
 * Yeti language compiler.
 *
 * Copyright (c) 2007 Madis Janson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package yeti.lang.compiler.source;

import yeti.lang.Core;
import yeti.lang.Fun;

import java.io.*;

public final class SourceReader {
    public static String[] basedirs;   //This is set in eval.yeti - custom reader function.
	public static Fun override; //This is set in eval.yeti - custom reader function.
    private static final String inCharset = "UTF-8";

    //An array is passed in here because the caller needs to access the name as mutated by this method,
    //but strings are immutable.
	public static char[] getSource(String[] name_, boolean fullPath) throws IOException {

        //If we've set the override (can this be static?) then create a Pair with the first in the array of names
        //and a Big-B-Boolean of the fullpath flag. Then apply the override function Fun to that pair. Then get
        //value 0 of the Pair and assign it to the first element of the names array. Then if what you got back from
        //applying the function is not an empty String object, return the string as a character array.
		if (override != null) {
			Struct arg = pair("name", name_[0], "fullpath", Boolean.valueOf(fullPath));
			String result = (String) override.apply(arg);
			name_[0] = (String) arg.get(0);
			if (result != Core.UNDEF_STR) {
				return result.toCharArray();
            }
		}

        // Allocate a buffer of 32768
		char[] buf = new char[0x8000];
		int l = 0;
		InputStream stream;
        
		// XXX workaround for windows - some code expects '/'
		name_[0] = name_[0].replace(File.separatorChar, '/');

        //We are taking that first name out of the array and then when we get done
        //with name at the end we are copying it back into the array. We do this because
        //the caller needs the mutated name to create the fully qualified class name (CompileCtx.java)
		String name = name_[0];

		if (fullPath) {                           //if this method was called with a full path, we return a stream from that path
			stream = new FileInputStream(name);   //name is the Filename for which we get a FileInputStream
        } else {
			try {
				stream = open(name);              //we open the FileInputStream with the "open" call below.
			} catch (IOException ex) {
				int p = name.lastIndexOf('/');    //we look for the last slash in the name, throw an exception if there is none
				if (p <= 0)
					throw ex;
				try {
                    //TODO - mutating name here
					stream = open(name = name.substring(p + 1));  //We take the string after the slash reassign it to name, and try again
				} catch (IOException e) {
					throw ex;                     //If we can't find it this time, we die.
				}
			}
        }

        try {
			Reader reader = new java.io.InputStreamReader(stream, inCharset);  //Open a reader with UTF-8
			for (int n; (n = reader.read(buf, l, buf.length - l)) >= 0;) {     //We iteratively read a whole buffer full of characters; n gives us bytes read
				if (buf.length - (l += n) < 0x1000) {                          //We add the # chars read to l, subtract from buffer length, if less than 4096
                                                                                // (i.e., if
					char[] tmp = new char[buf.length << 1];                    //create a temporary array of the buffer length << 1
					System.arraycopy(buf, 0, tmp, 0, l);                       //copy the buffer into tmp and make that into the buffer
					buf = tmp;
				}
			}
		} catch (IOException ex) {
			throw new IOException(name + ": " + ex.getMessage());
		} finally {
			stream.close();
		}

     	name_[0] = name;                          //now we reassign the actual name of the file to name_[0]
                                                  //but only if we have succeeded in reading the file
                                                  //we create a character array of length l
		char[] r = new char[l];                   //and copy the buffer into it (?)
		System.arraycopy(buf, 0, r, 0, l);        //and return it.
		return r;
	}

	private static InputStream open(String name) throws IOException {
		if (basedirs == null || basedirs.length == 0)                   //If we don't have basedirs set
			return new FileInputStream(name);                           //Just create an input stream using the name
		for (int i = 0;;)
			try {
				return new FileInputStream(new File(basedirs[i], name));  //If we do have it set, try to open a file using each one
			} catch (IOException ex) {
				if (++i >= basedirs.length)                               //if we exceed the number of base dirs, throw an exception.
					throw ex;
			}
	}

	//The TypePrettyPrinter wants to use this.
	static Struct pair(String name1, Object value1, String name2, Object value2) {
		// low-level implementation-specific struct, don't do that ;)
		return new Struct3(new String[] { name1, name2 }, new Object[] {value1, value2});
	}

}

