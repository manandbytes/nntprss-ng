/*
 * Created on Sep 6, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.methodize.nntprss.feed.db;

import java.io.DataInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jason
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TestHsqlDb {


	public static void main(String args[]) {
		TestHsqlDb test = new TestHsqlDb();
		test.runTest();
	}
	
	public void runTest() {
		boolean hsqlFound = false;

		//		Check for nntp//rss v0.3 hsqldb database - if found, migrate...
		Connection hsqlConn = null;
		Statement stmt = null;
		Statement stmt2 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		PreparedStatement ps = null;
		PreparedStatement ps2 = null;

		try {
			Class.forName("org.hsqldb.jdbcDriver");
			hsqlConn =
				DriverManager.getConnection("jdbc:hsqldb:nntprssdb", "sa", "");

			stmt = hsqlConn.createStatement();
			System.out.println("fs: " + stmt.getFetchSize());
			stmt.setFetchSize(100);
			System.out.println("fs: " + stmt.getFetchSize());
			System.out.println("fd: " + stmt.getFetchDirection());
			stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
			System.out.println("fd: " + stmt.getFetchDirection());
			rs = stmt.executeQuery("SELECT COUNT(*) FROM items");
			if(rs.next()) {
				System.out.println("Count:" + rs.getInt(1));
			}
			stmt.close();
			rs.close();

			rs = stmt.executeQuery("SELECT id from channels");
			List ids = new ArrayList();
			while(rs.next()) {
				ids.add(new Integer(rs.getInt(1)));
			}
			
			stmt.close();
			rs.close();
			
			System.out.println("Found " + ids.size() + " channels");
			new DataInputStream(System.in).readLine();
						
			int totalCount = 0;			
			for(int i =0; i < ids.size(); i++) {
				int id = ((Integer)ids.get(i)).intValue();
				int count = 0;
				boolean moreResults = true;
				while(moreResults) {
					rs =
						stmt.executeQuery(
							"SELECT LIMIT " + count + " 1000 * FROM items WHERE channel = " + id);
	
						int recCount = 0;
						while (rs.next()) {
							System.out.println("(" + totalCount + ") " + (count + recCount) + ": " + rs.getInt("articleNumber") + " ");
							recCount++;
						}
						
						if(recCount < 1000) {
							moreResults = false;
						}

						totalCount += recCount;

						stmt.close();
						rs.close();
	
					count+= 1000;
				}
			}
					
			// Shutdown hsqldb
			stmt.execute("SHUTDOWN");

			hsqlFound = true;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (Exception e) {
			}
			try {
				if (rs != null)
					rs.close();
			} catch (Exception e) {
			}
			try {
				if (ps != null)
					ps.close();
			} catch (Exception e) {
			}
			try {
				if (ps2 != null)
					ps2.close();
			} catch (Exception e) {
			}
			try {
				if (stmt2 != null)
					stmt2.close();
			} catch (Exception e) {
			}
			try {
				if (rs2 != null)
					rs2.close();
			} catch (Exception e) {
			}
			try {
				if (hsqlConn != null)
					hsqlConn.close();
			} catch (Exception e) {
			}
		}

	}
}
