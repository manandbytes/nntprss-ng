package org.methodize.nntprss.feed.db;

/* -----------------------------------------------------------
 * nntp//rss - a bridge between the RSS world and NNTP clients
 * Copyright (c) 2002, 2003 Jason Brome.  All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import jdbm.helper.Serializer;

/**
 * @author Jason Brome <jason@methodize.org>
 * @version $Id: InstanceJdbmSerializer.java,v 1.1 2003/09/28 20:14:18 jasonbrome Exp $
 */

public class InstanceJdbmSerializer implements Serializer {

	private Externalizable clazz = null;

	public InstanceJdbmSerializer(Externalizable clazz) {
		this.clazz = clazz;
	}

	/* (non-Javadoc)
	 * @see jdbm.helper.Serializer#deserialize(byte[])
	 */
	public Object deserialize(byte[] buf) throws IOException {
		ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(buf));
		try {
			clazz.readExternal(is);
		} catch(Exception e) {
			throw new IOException(e.getMessage());
		}
		return clazz;
	}

	/* (non-Javadoc)
	 * @see jdbm.helper.Serializer#serialize(java.lang.Object)
	 */
	public byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		((Externalizable)obj).writeExternal(oos);
		oos.flush();
		oos.close();
		return baos.toByteArray();
	}

}