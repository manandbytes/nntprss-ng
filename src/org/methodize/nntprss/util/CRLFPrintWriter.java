package org.methodize.nntprss.util;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002-2004 Jason Brome.  All Rights Reserved.
 *
 * email: nntprss@methodize.org
 * mail:  Methodize Solutions
 *        PO Box 3865
 *        Grand Central Station
 *        New York NY 10163
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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * 
 * Wrapper class - used for output writer in NNTP communications
 * All lines must end in CR/LF - if println is used in
 * standard PrintWriter implementation, platform specific
 * line termination will be used - e.g. \r\n *or* \n
 * 
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: CRLFPrintWriter.java,v 1.4 2004/01/04 21:26:46 jasonbrome Exp $
 */
public class CRLFPrintWriter extends PrintWriter {
	
	private static final String CRLF = "\r\n";
	
	public CRLFPrintWriter(OutputStream out) {
		super(out);
	}
	
	public CRLFPrintWriter(OutputStream out, boolean autoFlush) {
		super(out, autoFlush);
	}

	public CRLFPrintWriter(Writer out) {
		super(out);
	}
	
	public CRLFPrintWriter(Writer out, boolean autoFlush) {
		super(out, autoFlush);
	}
	
	

	/**
	 * @see java.io.PrintWriter#println()
	 */
	public void println() {
		super.print(CRLF);
	}

	/**
	 * @see java.io.PrintWriter#println(boolean)
	 */
	public void println(boolean arg0) {
		super.print(arg0);
		println();
	}

	/**
	 * @see java.io.PrintWriter#println(char)
	 */
	public void println(char arg0) {
		super.print(arg0);
		println();
	}

	/**
	 * @see java.io.PrintWriter#println(char[])
	 */
	public void println(char[] arg0) {
		super.print(arg0);
		println();
	}

	/**
	 * @see java.io.PrintWriter#println(double)
	 */
	public void println(double arg0) {
		super.print(arg0);
		println();
	}

	/**
	 * @see java.io.PrintWriter#println(float)
	 */
	public void println(float arg0) {
		super.print(arg0);
		println();
	}

	/**
	 * @see java.io.PrintWriter#println(int)
	 */
	public void println(int arg0) {
		super.print(arg0);
		println();
	}

	/**
	 * @see java.io.PrintWriter#println(long)
	 */
	public void println(long arg0) {
		super.print(arg0);
		println();
	}

	/**
	 * @see java.io.PrintWriter#println(Object)
	 */
	public void println(Object arg0) {
		super.print(arg0);
		println();
	}

	/**
	 * @see java.io.PrintWriter#println(String)
	 */
	public void println(String arg0) {
		super.print(arg0);
		println();
	}

}
