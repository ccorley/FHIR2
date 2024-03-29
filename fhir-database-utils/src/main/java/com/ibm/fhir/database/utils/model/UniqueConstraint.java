/*
 * (C) Copyright IBM Corp. 2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.database.utils.model;

/**
 * Constraint on unique
 */
public class UniqueConstraint extends Constraint {

    /**
     * @param constraintName
     */
    protected UniqueConstraint(String constraintName) {
        super(constraintName);
    }

}
