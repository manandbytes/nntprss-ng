package org.methodize.nntprss.util;

import java.util.HashMap;
import java.util.Map;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2005 Jason Brome.  All Rights Reserved.
 *
 * email: nntprss@methodize.org
 * mail:  Methodize Solutions
 *        PO Box 3865
 *        Grand Central Station
 *        New York NY 10163
 * 
 * This file is part of nntp//rss
 *
 * Entities list from:
 * http://www.w3.org/TR/html401/sgml/entities.html
 * 
 * Portions © International Organization for Standardization 1986:
 * Permission to copy in any form is granted for use with
 * conforming SGML systems and applications as defined in
 * ISO 8879, provided this notice is included in all copies.
 *
 * nntp//rss is free software; you can redistribute it 
 * and/or modify it under the terms of the GNU General 
 * Public License as published by the Free Software Foundation; 
 * either version 2 of the License, or (at your option) any 
 * later version.
 *
 * This program is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE.  See the GNU General Public License for more 
 * details.
 *
 * You should have received a copy of the GNU General Public 
 * License along with this program; if not, write to the 
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330, 
 * Boston, MA  02111-1307  USA
 * ----------------------------------------------------- */

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: HTMLHelper.java,v 1.5 2005/02/13 22:10:11 jasonbrome Exp $
 */

public class HTMLHelper {

    private static Map escapeMap = new HashMap();

    static {

        //		escapeMap.put("nbsp", new Character((char) 160));
        escapeMap.put("nbsp", new Character(' '));

        escapeMap.put("iexcl", new Character((char) 161));
        escapeMap.put("cent", new Character((char) 162));
        escapeMap.put("pound", new Character((char) 163));
        escapeMap.put("curren", new Character((char) 164));
        escapeMap.put("yen", new Character((char) 165));
        escapeMap.put("brvbar", new Character((char) 166));
        escapeMap.put("sect", new Character((char) 167));
        escapeMap.put("uml", new Character((char) 168));
        escapeMap.put("copy", new Character((char) 169));
        escapeMap.put("ordf", new Character((char) 170));
        escapeMap.put("laquo", new Character((char) 171));
        escapeMap.put("not", new Character((char) 172));
        escapeMap.put("shy", new Character((char) 173));
        escapeMap.put("reg", new Character((char) 174));
        escapeMap.put("macr", new Character((char) 175));
        escapeMap.put("deg", new Character((char) 176));
        escapeMap.put("plusmn", new Character((char) 177));
        escapeMap.put("sup2", new Character((char) 178));
        escapeMap.put("sup3", new Character((char) 179));
        escapeMap.put("acute", new Character((char) 180));
        escapeMap.put("micro", new Character((char) 181));
        escapeMap.put("para", new Character((char) 182));
        escapeMap.put("middot", new Character((char) 183));
        escapeMap.put("cedil", new Character((char) 184));
        escapeMap.put("sup1", new Character((char) 185));
        escapeMap.put("ordm", new Character((char) 186));
        escapeMap.put("raquo", new Character((char) 187));
        escapeMap.put("frac14", new Character((char) 188));
        escapeMap.put("frac12", new Character((char) 189));
        escapeMap.put("frac34", new Character((char) 190));
        escapeMap.put("iquest", new Character((char) 191));
        escapeMap.put("Agrave", new Character((char) 192));
        escapeMap.put("Aacute", new Character((char) 193));
        escapeMap.put("Acirc", new Character((char) 194));
        escapeMap.put("Atilde", new Character((char) 195));
        escapeMap.put("Auml", new Character((char) 196));
        escapeMap.put("Aring", new Character((char) 197));
        escapeMap.put("AElig", new Character((char) 198));
        escapeMap.put("Ccedil", new Character((char) 199));
        escapeMap.put("Egrave", new Character((char) 200));
        escapeMap.put("Eacute", new Character((char) 201));
        escapeMap.put("Ecirc", new Character((char) 202));
        escapeMap.put("Euml", new Character((char) 203));
        escapeMap.put("Igrave", new Character((char) 204));
        escapeMap.put("Iacute", new Character((char) 205));
        escapeMap.put("Icirc", new Character((char) 206));
        escapeMap.put("Iuml", new Character((char) 207));
        escapeMap.put("ETH ", new Character((char) 208));
        escapeMap.put("Ntilde", new Character((char) 209));
        escapeMap.put("Ograve", new Character((char) 210));
        escapeMap.put("Oacute", new Character((char) 211));
        escapeMap.put("Ocirc", new Character((char) 212));
        escapeMap.put("Otilde", new Character((char) 213));
        escapeMap.put("Ouml", new Character((char) 214));
        escapeMap.put("times", new Character((char) 215));
        escapeMap.put("Oslash", new Character((char) 216));
        escapeMap.put("Ugrave", new Character((char) 217));
        escapeMap.put("Uacute", new Character((char) 218));
        escapeMap.put("Ucirc", new Character((char) 219));
        escapeMap.put("Uuml", new Character((char) 220));
        escapeMap.put("Yacute", new Character((char) 221));
        escapeMap.put("THORN", new Character((char) 222));
        escapeMap.put("szlig", new Character((char) 223));
        escapeMap.put("agrave", new Character((char) 224));
        escapeMap.put("aacute", new Character((char) 225));
        escapeMap.put("acirc", new Character((char) 226));
        escapeMap.put("atilde", new Character((char) 227));
        escapeMap.put("auml", new Character((char) 228));
        escapeMap.put("aring", new Character((char) 229));
        escapeMap.put("aelig", new Character((char) 230));
        escapeMap.put("ccedil", new Character((char) 231));
        escapeMap.put("egrave", new Character((char) 232));
        escapeMap.put("eacute", new Character((char) 233));
        escapeMap.put("ecirc", new Character((char) 234));
        escapeMap.put("euml", new Character((char) 235));
        escapeMap.put("igrave", new Character((char) 236));
        escapeMap.put("iacute", new Character((char) 237));
        escapeMap.put("icirc", new Character((char) 238));
        escapeMap.put("iuml", new Character((char) 239));
        escapeMap.put("eth ", new Character((char) 240));
        escapeMap.put("ntilde", new Character((char) 241));
        escapeMap.put("ograve", new Character((char) 242));
        escapeMap.put("oacute", new Character((char) 243));
        escapeMap.put("ocirc", new Character((char) 244));
        escapeMap.put("otilde", new Character((char) 245));
        escapeMap.put("ouml", new Character((char) 246));
        escapeMap.put("divide", new Character((char) 247));
        escapeMap.put("oslash", new Character((char) 248));
        escapeMap.put("ugrave", new Character((char) 249));
        escapeMap.put("uacute", new Character((char) 250));
        escapeMap.put("ucirc", new Character((char) 251));
        escapeMap.put("uuml", new Character((char) 252));
        escapeMap.put("yacute", new Character((char) 253));
        escapeMap.put("thorn", new Character((char) 254));
        escapeMap.put("yuml", new Character((char) 255));

        // Mathematical, Greek and Symbolic characters for HTML
        // Latin Extended-B
        escapeMap.put("fnof", new Character((char) 402));

        // Greek
        escapeMap.put("Alpha", new Character((char) 913));
        escapeMap.put("Beta", new Character((char) 914));
        escapeMap.put("Gamma", new Character((char) 915));
        escapeMap.put("Delta", new Character((char) 916));
        escapeMap.put("Epsilon", new Character((char) 917));
        escapeMap.put("Zeta", new Character((char) 918));
        escapeMap.put("Eta", new Character((char) 919));
        escapeMap.put("Theta", new Character((char) 920));
        escapeMap.put("Iota", new Character((char) 921));
        escapeMap.put("Kappa", new Character((char) 922));
        escapeMap.put("Lambda", new Character((char) 923));
        escapeMap.put("Mu", new Character((char) 924));
        escapeMap.put("Nu", new Character((char) 925));
        escapeMap.put("Xi", new Character((char) 926));
        escapeMap.put("Omicron", new Character((char) 927));
        escapeMap.put("Pi", new Character((char) 928));
        escapeMap.put("Rho", new Character((char) 929));
        escapeMap.put("Sigma", new Character((char) 931));
        escapeMap.put("Tau", new Character((char) 932));
        escapeMap.put("Upsilon", new Character((char) 933));
        escapeMap.put("Phi", new Character((char) 934));
        escapeMap.put("Chi", new Character((char) 935));
        escapeMap.put("Psi", new Character((char) 936));
        escapeMap.put("Omega", new Character((char) 937));
        escapeMap.put("alpha", new Character((char) 945));
        escapeMap.put("beta", new Character((char) 946));
        escapeMap.put("gamma", new Character((char) 947));
        escapeMap.put("delta", new Character((char) 948));
        escapeMap.put("epsilon", new Character((char) 949));
        escapeMap.put("zeta", new Character((char) 950));
        escapeMap.put("eta", new Character((char) 951));
        escapeMap.put("theta", new Character((char) 952));
        escapeMap.put("iota", new Character((char) 953));
        escapeMap.put("kappa", new Character((char) 954));
        escapeMap.put("lambda", new Character((char) 955));
        escapeMap.put("mu", new Character((char) 956));
        escapeMap.put("nu", new Character((char) 957));
        escapeMap.put("xi", new Character((char) 958));
        escapeMap.put("omicron", new Character((char) 959));
        escapeMap.put("pi", new Character((char) 960));
        escapeMap.put("rho", new Character((char) 961));
        escapeMap.put("sigmaf", new Character((char) 962));
        escapeMap.put("sigma", new Character((char) 963));
        escapeMap.put("tau", new Character((char) 964));
        escapeMap.put("upsilon", new Character((char) 965));
        escapeMap.put("phi", new Character((char) 966));
        escapeMap.put("chi", new Character((char) 967));
        escapeMap.put("psi", new Character((char) 968));
        escapeMap.put("omega", new Character((char) 969));
        escapeMap.put("thetasym", new Character((char) 977));
        escapeMap.put("upsih", new Character((char) 978));
        escapeMap.put("piv", new Character((char) 982));

        // General Punctuation
        escapeMap.put("bull", new Character((char) 8226));
        escapeMap.put("hellip", new Character((char) 8230));
        escapeMap.put("prime", new Character((char) 8242));
        escapeMap.put("Prime", new Character((char) 8243));
        escapeMap.put("oline", new Character((char) 8254));
        escapeMap.put("frasl", new Character((char) 8260));

        // Letterlike Symbols
        escapeMap.put("weierp", new Character((char) 8472));
        escapeMap.put("image", new Character((char) 8465));
        escapeMap.put("real", new Character((char) 8476));
        escapeMap.put("trade", new Character((char) 8482));
        escapeMap.put("alefsym", new Character((char) 8501));

        // Arrows
        escapeMap.put("larr", new Character((char) 8592));
        escapeMap.put("uarr", new Character((char) 8593));
        escapeMap.put("rarr", new Character((char) 8594));
        escapeMap.put("darr", new Character((char) 8595));
        escapeMap.put("harr", new Character((char) 8596));
        escapeMap.put("crarr", new Character((char) 8629));
        escapeMap.put("lArr", new Character((char) 8656));
        escapeMap.put("uArr", new Character((char) 8657));
        escapeMap.put("rArr", new Character((char) 8658));
        escapeMap.put("dArr", new Character((char) 8659));
        escapeMap.put("hArr", new Character((char) 8660));

        // Mathematical Operators
        escapeMap.put("forall", new Character((char) 8704));
        escapeMap.put("part", new Character((char) 8706));
        escapeMap.put("exist", new Character((char) 8707));
        escapeMap.put("empty", new Character((char) 8709));
        escapeMap.put("nabla", new Character((char) 8711));
        escapeMap.put("isin", new Character((char) 8712));
        escapeMap.put("notin", new Character((char) 8713));
        escapeMap.put("ni", new Character((char) 8715));
        escapeMap.put("prod", new Character((char) 8719));
        escapeMap.put("sum", new Character((char) 8721));
        escapeMap.put("minus", new Character((char) 8722));
        escapeMap.put("lowast", new Character((char) 8727));
        escapeMap.put("radic", new Character((char) 8730));
        escapeMap.put("prop", new Character((char) 8733));
        escapeMap.put("infin", new Character((char) 8734));
        escapeMap.put("ang", new Character((char) 8736));
        escapeMap.put("and", new Character((char) 8743));
        escapeMap.put("or", new Character((char) 8744));
        escapeMap.put("cap", new Character((char) 8745));
        escapeMap.put("cup", new Character((char) 8746));
        escapeMap.put("int", new Character((char) 8747));
        escapeMap.put("there4", new Character((char) 8756));
        escapeMap.put("sim", new Character((char) 8764));
        escapeMap.put("cong", new Character((char) 8773));
        escapeMap.put("asymp", new Character((char) 8776));
        escapeMap.put("ne", new Character((char) 8800));
        escapeMap.put("equiv", new Character((char) 8801));
        escapeMap.put("le", new Character((char) 8804));
        escapeMap.put("ge", new Character((char) 8805));
        escapeMap.put("sub", new Character((char) 8834));
        escapeMap.put("sup", new Character((char) 8835));
        escapeMap.put("nsub", new Character((char) 8836));
        escapeMap.put("sube", new Character((char) 8838));
        escapeMap.put("supe", new Character((char) 8839));
        escapeMap.put("oplus", new Character((char) 8853));
        escapeMap.put("otimes", new Character((char) 8855));
        escapeMap.put("perp", new Character((char) 8869));
        escapeMap.put("sdot", new Character((char) 8901));

        // Miscellaneous Technical
        escapeMap.put("lceil", new Character((char) 8968));
        escapeMap.put("rceil", new Character((char) 8969));
        escapeMap.put("lfloor", new Character((char) 8970));
        escapeMap.put("rfloor", new Character((char) 8971));
        escapeMap.put("lang", new Character((char) 9001));
        escapeMap.put("rang", new Character((char) 9002));

        // Geometric Shapes 
        escapeMap.put("loz", new Character((char) 9674));

        // Miscellaneous Symbols 
        escapeMap.put("spades", new Character((char) 9824));
        escapeMap.put("clubs", new Character((char) 9827));
        escapeMap.put("hearts", new Character((char) 9829));
        escapeMap.put("diams", new Character((char) 9830));

        // Special characters for HTML
        // C0 Controls and Basic Latin 
        escapeMap.put("quot", new Character((char) 34));
        escapeMap.put("amp", new Character((char) 38));
        escapeMap.put("lt", new Character((char) 60));
        escapeMap.put("gt", new Character((char) 62));
        escapeMap.put("apos", new Character('\''));

        // Latin Extended-A
        escapeMap.put("OElig", new Character((char) 338));
        escapeMap.put("oelig", new Character((char) 339));
        escapeMap.put("Scaron", new Character((char) 352));
        escapeMap.put("scaron", new Character((char) 353));
        escapeMap.put("Yuml", new Character((char) 376));

        // Spacing Modifier Letters 
        escapeMap.put("circ", new Character((char) 710));
        escapeMap.put("tilde", new Character((char) 732));

        // General Punctuation 
        escapeMap.put("ensp", new Character((char) 8194));
        escapeMap.put("emsp", new Character((char) 8195));
        escapeMap.put("thinsp", new Character((char) 8201));
        escapeMap.put("zwnj", new Character((char) 8204));
        escapeMap.put("zwj", new Character((char) 8205));
        escapeMap.put("lrm", new Character((char) 8206));
        escapeMap.put("rlm", new Character((char) 8207));
        escapeMap.put("ndash", new Character((char) 8211));
        escapeMap.put("mdash", new Character((char) 8212));
        escapeMap.put("lsquo", new Character((char) 8216));
        escapeMap.put("rsquo", new Character((char) 8217));
        escapeMap.put("sbquo", new Character((char) 8218));
        escapeMap.put("ldquo", new Character((char) 8220));
        escapeMap.put("rdquo", new Character((char) 8221));
        escapeMap.put("bdquo", new Character((char) 8222));
        escapeMap.put("dagger", new Character((char) 8224));
        escapeMap.put("Dagger", new Character((char) 8225));
        escapeMap.put("permil", new Character((char) 8240));
        escapeMap.put("lsaquo", new Character((char) 8249));
        escapeMap.put("rsaquo", new Character((char) 8250));
        escapeMap.put("euro", new Character((char) 8364));

    }

    public static String unescapeString(String value) {
        StringBuffer unescapedString = new StringBuffer();
        StringBuffer charBuf = null;

        for (int pos = 0; pos < value.length(); pos++) {
            char c = value.charAt(pos);

            if (c == '&'
                && (pos < value.length() - 3)) { // Process reference...
                // if less than three characters left, discard
                c = value.charAt(++pos);

                boolean numeric = false;
                if (c == '#') {
                    numeric = true;
                    c = value.charAt(++pos);
                }

                if (charBuf == null) {
                    charBuf = new StringBuffer(32);
                } else {
                    charBuf.setLength(0);
                }

                while (c != ';' && pos < value.length() - 1) {
                    charBuf.append(c);
                    c = value.charAt(++pos);
                }

                if (numeric) {
                    try {
                        c = (char) Integer.parseInt(charBuf.toString(), 16);
                    } catch (NumberFormatException nfe) {
                        // If we can't process it, just write out the text...
                        unescapedString.append("&#").append(
                            charBuf.toString()).append(
                            ';');
                    }
                } else {
                    Character unescapedVer =
                        (Character) escapeMap.get(charBuf.toString());
                    if (unescapedVer != null) {
                        unescapedString.append(unescapedVer);
                    } else {
                        unescapedString.append("&").append(
                            charBuf.toString()).append(
                            ';');
                    }
                }
            } else if (c == '&' && (pos >= value.length() - 3)) {
                // If we're at the end of the string, and there's 3 or less characters,
                // then assume that this string has been truncated
                break;
            } else {
                unescapedString.append(c);
            }

        }

        return unescapedString.toString();
    }

    public static String escapeString(String value) {
        StringBuffer escapedString = new StringBuffer();
        for (int charCount = 0; charCount < value.length(); charCount++) {
            char c = value.charAt(charCount);
            switch (c) {
                case '&' :
                    escapedString.append("&amp;");
                    break;
                case '<' :
                    escapedString.append("&lt;");
                    break;
                case '>' :
                    escapedString.append("&gt;");
                    break;
                case '\"' :
                    escapedString.append("&quot;");
                    break;
                case '\'' :
                    escapedString.append("&#39;");
                    break;
                default :
                    escapedString.append(c);
            }
        }
        return escapedString.toString();
    }

    public static String stripCRLF(String value) {
        StringBuffer strippedString = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c == '\n' || c == '\r')) {
                strippedString.append(c);
            }
        }
        return strippedString.toString();
    }
}
