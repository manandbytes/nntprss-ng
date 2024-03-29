package org.methodize.nntprss.util;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2007 Jason Brome.  All Rights Reserved.
 *
 * email: nntprss@methodize.org
 * mail:  Jason Brome
 *        PO Box 222-WOB
 *        West Orange
 *        NJ 07052-0222
 * 
 * This file is part of nntp//rss
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
 * 
 * Base64 encoding class - decoding not required for nntp//rss
 * 
 * Based upon Public Domain Base64 class from Robert Harder
 * See: http://iharder.sourceforge.net/base64/
 * 
 * @author Robert Harder
 * @author rob@iharder.net
 * @version 1.4
 */
public class Base64 {

    /** Specify encoding (value is <tt>true</tt>). */
    public final static boolean ENCODE = true;

    /** Maximum line length (76) of Base64 output. */
    private final static int MAX_LINE_LENGTH = 76;

    /** The equals sign (=) as a byte. */
    private final static byte EQUALS_SIGN = (byte) '=';

    /** The new line character (\n) as a byte. */
    private final static byte NEW_LINE = (byte) '\n';

    /** The 64 valid Base64 values. */
    private final static byte[] ALPHABET =
        {
            (byte) 'A',
            (byte) 'B',
            (byte) 'C',
            (byte) 'D',
            (byte) 'E',
            (byte) 'F',
            (byte) 'G',
            (byte) 'H',
            (byte) 'I',
            (byte) 'J',
            (byte) 'K',
            (byte) 'L',
            (byte) 'M',
            (byte) 'N',
            (byte) 'O',
            (byte) 'P',
            (byte) 'Q',
            (byte) 'R',
            (byte) 'S',
            (byte) 'T',
            (byte) 'U',
            (byte) 'V',
            (byte) 'W',
            (byte) 'X',
            (byte) 'Y',
            (byte) 'Z',
            (byte) 'a',
            (byte) 'b',
            (byte) 'c',
            (byte) 'd',
            (byte) 'e',
            (byte) 'f',
            (byte) 'g',
            (byte) 'h',
            (byte) 'i',
            (byte) 'j',
            (byte) 'k',
            (byte) 'l',
            (byte) 'm',
            (byte) 'n',
            (byte) 'o',
            (byte) 'p',
            (byte) 'q',
            (byte) 'r',
            (byte) 's',
            (byte) 't',
            (byte) 'u',
            (byte) 'v',
            (byte) 'w',
            (byte) 'x',
            (byte) 'y',
            (byte) 'z',
            (byte) '0',
            (byte) '1',
            (byte) '2',
            (byte) '3',
            (byte) '4',
            (byte) '5',
            (byte) '6',
            (byte) '7',
            (byte) '8',
            (byte) '9',
            (byte) '+',
            (byte) '/' };

    /** 
     * Translates a Base64 value to either its 6-bit reconstruction value
     * or a negative number indicating some other meaning.
     **/
    private final static byte[] DECODABET =
        { -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal  0 -  8
        -5, -5, // Whitespace: Tab and Linefeed
        -9, -9, // Decimal 11 - 12
        -5, // Whitespace: Carriage Return
        -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 - 26
        -9, -9, -9, -9, -9, // Decimal 27 - 31
        -5, // Whitespace: Space
        -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42
        62, // Plus sign at decimal 43
        -9, -9, -9, // Decimal 44 - 46
        63, // Slash at decimal 47
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero through nine
        -9, -9, -9, // Decimal 58 - 60
        -1, // Equals sign at decimal 61
        -9, -9, -9, // Decimal 62 - 64
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
        // Letters 'A' through 'N'
        14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
        // Letters 'O' through 'Z'
        -9, -9, -9, -9, -9, -9, // Decimal 91 - 96
        26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38,
        // Letters 'a' through 'm'
        39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51,
        // Letters 'n' through 'z'
        -9, -9, -9, -9 // Decimal 123 - 126
        /*,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 127 - 139
        -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 140 - 152
        -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 153 - 165
        -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 166 - 178
        -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 179 - 191
        -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 192 - 204
        -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 205 - 217
        -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 218 - 230
        -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,     // Decimal 231 - 243
        -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9         // Decimal 244 - 255 */
    };

    private final static byte BAD_ENCODING = -9; // Indicates error in encoding
    private final static byte WHITE_SPACE_ENC = -5;
    // Indicates white space in encoding
    private final static byte EQUALS_SIGN_ENC = -1;
    // Indicates equals sign in encoding

    /** Defeats instantiation. */
    private Base64() {
    }

    /* ********  E N C O D I N G   M E T H O D S  ******** */

    /**
     * Encodes the first three bytes of array <var>threeBytes</var>
     * and returns a four-byte array in Base64 notation.
     *
     * @param threeBytes the array to convert
     * @return four byte array in Base64 notation.
     * @since 1.3
     */
    private static byte[] encode3to4(byte[] threeBytes) {
        return encode3to4(threeBytes, 3);
    } // end encodeToBytes

    /**
     * Encodes up to the first three bytes of array <var>threeBytes</var>
     * and returns a four-byte array in Base64 notation.
     * The actual number of significant bytes in your array is
     * given by <var>numSigBytes</var>.
     * The array <var>threeBytes</var> needs only be as big as
     * <var>numSigBytes</var>.
     *
     * @param threeBytes the array to convert
     * @param numSigBytes the number of significant bytes in your array
     * @return four byte array in Base64 notation.
     * @since 1.3
     */
    private static byte[] encode3to4(byte[] threeBytes, int numSigBytes) {
        byte[] dest = new byte[4];
        encode3to4(threeBytes, 0, numSigBytes, dest, 0);
        return dest;
    }

    /**
     * Encodes up to three bytes of the array <var>source</var>
     * and writes the resulting four Base64 bytes to <var>destination</var>.
     * The source and destination arrays can be manipulated
     * anywhere along their length by specifying 
     * <var>srcOffset</var> and <var>destOffset</var>.
     * This method does not check to make sure your arrays
     * are large enough to accomodate <var>srcOffset</var> + 3 for
     * the <var>source</var> array or <var>destOffset</var> + 4 for
     * the <var>destination</var> array.
     * The actual number of significant bytes in your array is
     * given by <var>numSigBytes</var>.
     *
     * @param source the array to convert
     * @param srcOffset the index where conversion begins
     * @param numSigBytes the number of significant bytes in your array
     * @param destination the array to hold the conversion
     * @param destOffset the index where output will be put
     * @return the <var>destination</var> array
     * @since 1.3
     */
    private static byte[] encode3to4(
        byte[] source,
        int srcOffset,
        int numSigBytes,
        byte[] destination,
        int destOffset) {
        //           1         2         3  
        // 01234567890123456789012345678901 Bit position
        // --------000000001111111122222222 Array position from threeBytes
        // --------|    ||    ||    ||    | Six bit groups to index ALPHABET
        //          >>18  >>12  >> 6  >> 0  Right shift necessary
        //                0x3f  0x3f  0x3f  Additional AND

        // Create buffer with zero-padding if there are only one or two
        // significant bytes passed in the array.
        // We have to shift left 24 in order to flush out the 1's that appear
        // when Java treats a value as negative that is cast from a byte to an int.
        int inBuff =
            (numSigBytes > 0 ? ((source[srcOffset] << 24) >>> 8) : 0)
                | (numSigBytes > 1 ? ((source[srcOffset + 1] << 24) >>> 16) : 0)
                | (numSigBytes > 2 ? ((source[srcOffset + 2] << 24) >>> 24) : 0);

        switch (numSigBytes) {
            case 3 :
                destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
                destination[destOffset + 3] = ALPHABET[(inBuff) & 0x3f];
                return destination;

            case 2 :
                destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
                destination[destOffset + 3] = EQUALS_SIGN;
                return destination;

            case 1 :
                destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                destination[destOffset + 2] = EQUALS_SIGN;
                destination[destOffset + 3] = EQUALS_SIGN;
                return destination;

            default :
                return destination;
        } // end switch
    } // end encode3to4

    /**
     * Encodes a byte array into Base64 notation.
     * Equivalen to calling
     * <code>encodeBytes( source, 0, source.length )</code>
     *
     * @param source The data to convert
     * @since 1.4
     */
    public static String encodeBytes(byte[] source) {
        return encodeBytes(source, true);
    } // end encodeBytes

    /**
     * Encodes a byte array into Base64 notation.
     * Equivalen to calling
     * <code>encodeBytes( source, 0, source.length )</code>
     *
     * @param source The data to convert
     * @param breakLines Break lines at 80 characters or less.
     * @since 1.4
     */
    public static String encodeBytes(byte[] source, boolean breakLines) {
        return encodeBytes(source, 0, source.length, breakLines);
    } // end encodeBytes

    /**
     * Encodes a byte array into Base64 notation.
     *
     * @param source The data to convert
     * @param off Offset in array where conversion should begin
     * @param len Length of data to convert
     * @param breakLines Break lines at 80 characters or less.
     * @since 1.4
     */
    public static String encodeBytes(
        byte[] source,
        int off,
        int len,
        boolean breakLines) {
        int len43 = len * 4 / 3;
            byte[] outBuff = new byte[(len43) // Main 4:3
        + ((len % 3) > 0 ? 4 : 0) // Account for padding
    + (breakLines ? (len43 / MAX_LINE_LENGTH) : 0)]; // New lines      
        int d = 0;
        int e = 0;
        int len2 = len - 2;
        int lineLength = 0;
        for (; d < len2; d += 3, e += 4) {
            encode3to4(source, d + off, 3, outBuff, e);

            lineLength += 4;
            if (breakLines && lineLength == MAX_LINE_LENGTH) {
                outBuff[e + 4] = NEW_LINE;
                e++;
                lineLength = 0;
            } // end if: end of line
        } // en dfor: each piece of array

        if (d < len) {
            encode3to4(source, d + off, len - d, outBuff, e);
            e += 4;
        } // end if: some padding needed

        return new String(outBuff, 0, e);
    } // end encodeBytes

} // end class Base64
