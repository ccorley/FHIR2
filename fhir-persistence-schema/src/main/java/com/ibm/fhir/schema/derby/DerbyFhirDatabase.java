/*
 * (C) Copyright IBM Corp. 2019, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.schema.derby;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import com.ibm.fhir.database.utils.api.IConnectionProvider;
import com.ibm.fhir.database.utils.api.IDatabaseTranslator;
import com.ibm.fhir.database.utils.api.ITransactionProvider;
import com.ibm.fhir.database.utils.common.JdbcConnectionProvider;
import com.ibm.fhir.database.utils.common.JdbcPropertyAdapter;
import com.ibm.fhir.database.utils.common.JdbcTarget;
import com.ibm.fhir.database.utils.derby.DerbyAdapter;
import com.ibm.fhir.database.utils.derby.DerbyMaster;
import com.ibm.fhir.database.utils.derby.DerbyTranslator;
import com.ibm.fhir.database.utils.model.PhysicalDataModel;
import com.ibm.fhir.database.utils.pool.PoolConnectionProvider;
import com.ibm.fhir.database.utils.transaction.SimpleTransactionProvider;
import com.ibm.fhir.database.utils.version.CreateVersionHistory;
import com.ibm.fhir.database.utils.version.VersionHistoryService;
import com.ibm.fhir.schema.control.FhirSchemaGenerator;

/**
 * An Apache Derby implementation of the IBM FHIR Server database (useful for supporting unit tests).
 */
public class DerbyFhirDatabase implements AutoCloseable, IConnectionProvider {
    private static final Logger logger = Logger.getLogger(DerbyFhirDatabase.class.getName());
    private static final String DATABASE_NAME = "derby/fhirDB";
    private static final String SCHEMA_NAME = "FHIRDATA";
    private static final String ADMIN_SCHEMA_NAME = "FHIR_ADMIN";

    // The translator to help us out with Derby syntax
    private static final IDatabaseTranslator DERBY_TRANSLATOR = new DerbyTranslator();

    // The wrapper for managing a derby in-memory instance
    final DerbyMaster derby;

    /**
     * The default constructor will initialize the database at "derby/fhirDB".
     */
    public DerbyFhirDatabase() throws SQLException {
        this(DATABASE_NAME);
    }

    /**
     * Construct a Derby database at the specified path and deploy the IBM FHIR Server schema.
     */
    public DerbyFhirDatabase(String dbPath) throws SQLException {
        logger.info("Creating Derby database for FHIR: " + dbPath);
        derby = new DerbyMaster(dbPath);

        // Lambdas are quite tasty for this sort of thing
        derby.runWithAdapter(adapter -> CreateVersionHistory.createTableIfNeeded(ADMIN_SCHEMA_NAME, adapter));

        // Database objects for the admin schema (shared across multiple tenants in the same DB)
        FhirSchemaGenerator gen = new FhirSchemaGenerator(ADMIN_SCHEMA_NAME, SCHEMA_NAME);
        PhysicalDataModel pdm = new PhysicalDataModel();
        gen.buildSchema(pdm);
        gen.buildProcedures(pdm);

        // apply the model we've defined to the new Derby database
        derby.createSchema(createVersionHistoryService(), pdm);
    }

    /**
     * Configure the TransactionProvider
     * @throws SQLException 
     */
    public VersionHistoryService createVersionHistoryService() throws SQLException {
        Connection c = derby.getConnection();
        JdbcTarget target = new JdbcTarget(c);

        JdbcPropertyAdapter jdbcAdapter = new JdbcPropertyAdapter(new Properties());
        JdbcConnectionProvider cp = new JdbcConnectionProvider(DERBY_TRANSLATOR, jdbcAdapter);
        PoolConnectionProvider connectionPool = new PoolConnectionProvider(cp, 200);
        ITransactionProvider transactionProvider = new SimpleTransactionProvider(connectionPool);

        DerbyAdapter derbyAdapter = new DerbyAdapter(target);
        CreateVersionHistory.createTableIfNeeded(ADMIN_SCHEMA_NAME, derbyAdapter);

        // Current version history for the data schema
        VersionHistoryService vhs = new VersionHistoryService(ADMIN_SCHEMA_NAME, SCHEMA_NAME);
        vhs.setTransactionProvider(transactionProvider);
        vhs.setTarget(derbyAdapter);
        vhs.init();
        return vhs;
    }

    @Override
    public void close() throws Exception {
        derby.close();
    }

    @Override
    public void commitTransaction() throws SQLException {
        // NOP
    }

    @Override
    public void describe(String arg0, StringBuilder arg1, String arg2) {
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection result = derby.getConnection();
        result.setSchema(SCHEMA_NAME);
        return result;
    }

    @Override
    public IDatabaseTranslator getTranslator() {
        return derby.getTranslator();
    }

    @Override
    public void rollbackTransaction() throws SQLException {
        // NOP
    }
}
