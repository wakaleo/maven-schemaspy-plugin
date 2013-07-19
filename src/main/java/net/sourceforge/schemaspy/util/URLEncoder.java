/*
 * Copyright 2013 Wakaleo Consulting.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.schemaspy.util;

import java.io.UnsupportedEncodingException;
import net.sourceforge.schemaspy.Config;

/**
 * Percent-encode Strings for use in URLs.
 *
 * @author sturton
 */
public class URLEncoder {

  /*
   *  Encoding characterset 
   */
  private  String  charset = Config.getInstance().getCharset();

  public URLEncoder(String charset) {
    this.charset = charset ;
  }

   /**
     * Return an URL-encoded version of the specified string in the specified encoding.
     *
     * <p>Any encoding failure results in the return of the original string.
     * </p>
     * @param str 
     * @return 
     */
    public String encode(String str) {
	try {
    	   String url = java.net.URLEncoder.encode(str, charset);
	   int len = url.length();
    	   StringBuilder buf = new StringBuilder(len * 2); // x2 should limit # of reallocs
    	   for (int i = 0; i < len; i++) {
			buf.append( ( '+' == url.charAt(i) )
				   ? "%20"
				   : url.charAt(i)
				  );
		}
    	   return buf.toString();
	}
	catch(UnsupportedEncodingException uee)
	{
	  return str;
	}
    }
  
}
