/*
 * Copyright (c) 2013, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software;Designed and Developed mainly by many Chinese 
 * opensource volunteers. you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Any questions about this component can be directed to it's project Web address 
 * https://code.google.com/p/opencloudb/.
 *
 */
package org.opencloudb.backend;

import org.apache.log4j.Logger;
import org.opencloudb.mysql.nio.handler.ResponseHandler;
import org.opencloudb.route.RouteResultsetNode;

public class PhysicalDBNode {
	protected static final Logger LOGGER = Logger
			.getLogger(PhysicalDBNode.class);

	protected final String name;
	protected final String database;
	protected final PhysicalDBPool dbPool;
	protected volatile long executeCount;

	public PhysicalDBNode(String hostName, String database,
			PhysicalDBPool dbPool) {
		this.name = hostName;
		this.database = database;
		this.dbPool = dbPool;
	}

	public String getName() {
		return name;
	}

	public long getExecuteCount() {
		return executeCount;
	}

	public PhysicalDBPool getDbPool() {
		return dbPool;
	}

	public String getDatabase() {
		return database;
	}

	/**
	 * get connection from the same datasource
	 * 
	 * @param exitsCon
	 * @throws Exception
	 */
	public void getConnectionFromSameSource(ConnectionMeta conMeta,
			BackendConnection exitsCon, ResponseHandler handler,
			Object attachment) throws Exception {
		
		PhysicalDatasource ds = this.dbPool.findDatasouce(exitsCon);
		if (ds == null) {
			throw new RuntimeException(
					"can't find exits connection,maybe fininshed " + exitsCon);
		} else {
			ds.getConnection(conMeta, handler, attachment);
		}

	}

	private void checkRequest(ConnectionMeta conMeta) {
		if (conMeta.getSchema() != null
				&& !conMeta.getSchema().equals(this.database)) {
			throw new RuntimeException(
					"invalid param ,connection request db is :"
							+ conMeta.getSchema() + " and datanode db is "
							+ this.database);
		}
		if (!dbPool.isInitSuccess()) {
			dbPool.init(dbPool.activedIndex);
		}
	}

	public void getConnection(ConnectionMeta conMeta, RouteResultsetNode rrs,
			ResponseHandler handler, Object attachment) throws Exception {
		checkRequest(conMeta);
		if (dbPool.isInitSuccess()) {
			if (rrs.canRunnINReadDB(conMeta.isAutocommit())) {
				dbPool.getRWBanlanceCon(conMeta, handler, attachment,
						this.database);
			} else {
				dbPool.getSource().getConnection(conMeta, handler, attachment);
			}

			executeCount++;
		} else {
			throw new IllegalArgumentException("Invalid DataSource:"
					+ dbPool.getActivedIndex());
		}
	}
}