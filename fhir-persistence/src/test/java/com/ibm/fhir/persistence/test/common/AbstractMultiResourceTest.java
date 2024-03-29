/*
 * (C) Copyright IBM Corp. 2018,2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.test.common;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;
import java.util.UUID;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ibm.fhir.model.resource.Encounter;
import com.ibm.fhir.model.resource.Observation;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.test.TestUtil;
import com.ibm.fhir.persistence.SingleResourceResult;

/**
 * This class contains a collection of tests that will be run against
 * each of the various persistence layer implementations.
 * There will be a subclass in each persistence project.
 */
public abstract class AbstractMultiResourceTest extends AbstractPersistenceTest {
    String commonId = UUID.randomUUID().toString();
    
    /**
     * Create two different resources with the same id 
     */
    @BeforeClass
    public void createResources() throws Exception {
        
        Encounter encounter = TestUtil.readExampleResource("json/ibm/minimal/Encounter-1.json");
        
        // Update the id on the resource
        encounter = encounter.toBuilder().id(commonId).build();
        
        encounter = persistence.update(getDefaultPersistenceContext(), commonId, encounter).getResource();
        assertNotNull(encounter);
        assertNotNull(encounter.getId());
        assertNotNull(encounter.getMeta());
        assertNotNull(encounter.getMeta().getVersionId().getValue());
        assertEquals("1", encounter.getMeta().getVersionId().getValue());
        
        Observation observation = TestUtil.readExampleResource("json/ibm/minimal/Observation-1.json");

        // update the id on the resource
        observation = observation.toBuilder().id(commonId).build();
        observation = persistence.update(getDefaultPersistenceContext(), commonId, observation).getResource();
        assertNotNull(observation);
        assertNotNull(observation.getId());
        assertNotNull(observation.getMeta());
        assertNotNull(observation.getMeta().getVersionId().getValue());
        assertEquals("1", observation.getMeta().getVersionId().getValue());
    } 
    
    /**
     * Tests a normal read when a different resource type has the same id
     */
    @Test
    public void testRead() throws Exception {
        SingleResourceResult<? extends Resource> result;
        
        result = persistence.read(getDefaultPersistenceContext(), Encounter.class, commonId);
        assertTrue(result.isSuccess());
        assertNotNull(result.getResource());
        assertTrue(result.getResource() instanceof Encounter);
        
        result = persistence.read(getDefaultPersistenceContext(), Observation.class, commonId);
        assertTrue(result.isSuccess());
        assertNotNull(result.getResource());
        assertTrue(result.getResource() instanceof Observation);
    }
    
    /**
     * Tests searching by id when a different resource type has the same id
     */
    @Test
    public void testSearchById() throws Exception {
        List<Resource> resources;
        
        resources = runQueryTest(Encounter.class, "_id", commonId);
        assertNotNull(resources);
        assertTrue(resources.size() == 1);
        assertTrue(resources.get(0) instanceof Encounter);
        
        resources = runQueryTest(Observation.class, "_id", commonId);
        assertNotNull(resources);
        assertTrue(resources.size() == 1);
        assertTrue(resources.get(0) instanceof Observation);
        
        resources = runQueryTest(Resource.class, "_id", commonId);
        assertNotNull(resources);
        assertTrue(resources.size() == 2);
    }
}
