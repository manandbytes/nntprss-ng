package org.methodize.nntprss.util;

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
 * @version 
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
