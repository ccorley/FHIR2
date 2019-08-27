/**
 * (C) Copyright IBM Corp. 2017,2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.watsonhealth.fhir.server.test;

import static com.ibm.watsonhealth.fhir.model.type.String.string;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.testng.annotations.Test;

import com.ibm.watsonhealth.fhir.core.FHIRMediaType;
import com.ibm.watsonhealth.fhir.model.format.Format;
import com.ibm.watsonhealth.fhir.model.resource.Bundle;
import com.ibm.watsonhealth.fhir.model.resource.CapabilityStatement;
import com.ibm.watsonhealth.fhir.model.resource.Observation;
import com.ibm.watsonhealth.fhir.model.resource.OperationOutcome;
import com.ibm.watsonhealth.fhir.model.resource.Patient;
import com.ibm.watsonhealth.fhir.model.type.ContactPoint;
import com.ibm.watsonhealth.fhir.model.type.ContactPointSystem;
import com.ibm.watsonhealth.fhir.model.type.ContactPointUse;
import com.ibm.watsonhealth.fhir.model.util.FHIRUtil;

/**
 * Basic sniff test of the FHIR Server.
 */
public class BasicServerTest extends FHIRServerTestBase {
    private Patient savedCreatedPatient;
    private Observation savedCreatedObservation;

    /**
     * Verify the 'metadata' API.
     */
    @Test(groups = { "server-basic" })
    public void testMetadataAPI() {
        WebTarget target = getWebTarget();
        Response response = target.path("metadata").request().get();
        assertResponse(response, Response.Status.OK.getStatusCode());

        CapabilityStatement conf = response.readEntity(CapabilityStatement.class);
        assertNotNull(conf);
        assertNotNull(conf.getFormat());
        assertEquals(6, conf.getFormat().size());
        assertNotNull(conf.getVersion());
        assertNotNull(conf.getName());
    }

    /**
     * Verify the 'metadata' API with Content-Type XML.
     */
    @Test(groups = { "server-basic" })
    public void testMetadataAPI_XML() {
        WebTarget target = getWebTarget();
        Response response = target.path("metadata").request(FHIRMediaType.APPLICATION_FHIR_XML).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        assertEquals(FHIRMediaType.APPLICATION_FHIR_XML_TYPE, response.getMediaType());

        CapabilityStatement conf = response.readEntity(CapabilityStatement.class);
        assertNotNull(conf);
        assertNotNull(conf.getFormat());
        assertEquals(6, conf.getFormat().size());
        assertNotNull(conf.getVersion());
        assertNotNull(conf.getName());
    }

    /**
     * Create a Patient, then make sure we can retrieve it.
     */
    @Test(groups = { "server-basic" })
    public void testCreatePatient() throws Exception {
        WebTarget target = getWebTarget();

        // Build a new Patient and then call the 'create' API.
        Patient patient = readResource(Patient.class, "Patient_JohnDoe.json");
        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Patient").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the patient's logical id value.
        String patientId = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new patient and verify it.
        response = target.path("Patient/" + patientId).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient responsePatient = response.readEntity(Patient.class);
        savedCreatedPatient = responsePatient;

        assertResourceEquals(patient, responsePatient);
    }

    /**
     * Create a minimal Patient, then make sure we can retrieve it.
     */
    @Test(groups = { "server-basic" })
    public void testCreatePatient_minimal() throws Exception {
        WebTarget target = getWebTarget();

        // Build a new Patient and then call the 'create' API.
        Patient patient = readResource(Patient.class, "Patient_DavidOrtiz.json");

        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Patient").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the patient's logical id value.
        String patientId = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new patient and verify it.
        response = target.path("Patient/" + patientId).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient responsePatient = response.readEntity(Patient.class);

        assertResourceEquals(patient, responsePatient);
    }

    /**
     * Create a minimal Patient, then make sure we can retrieve it.
     */
    @Test(groups = { "server-basic" })
    public void testCreatePatient_minimal_extraElement() throws Exception {
        WebTarget target = getWebTarget();

        // Build a new Patient and then call the 'create' API.
        Patient patient = readResource(Patient.class, "Patient_DavidOrtiz.json");

        StringWriter writer = new StringWriter();
        FHIRUtil.write(patient, Format.JSON, writer);
        String patientResourceString = writer.toString();
        String patientResourceStringWithFakeElement = patientResourceString.substring(0, 1) + "\"fake\":\"value\","
                + patientResourceString.substring(1);

        Entity<String> entity = Entity.entity(patientResourceStringWithFakeElement, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Patient").request().post(entity, Response.class);

        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        OperationOutcome responseResource = response.readEntity(OperationOutcome.class);
        assertNotNull(responseResource);
        assertTrue(responseResource.getIssue().size() > 0);
    }

    /**
     * Create an Observation and make sure we can retrieve it.
     */
    @Test(groups = { "server-basic" }, dependsOnMethods = { "testCreatePatient" })
    public void testCreateObservation() throws Exception {
        WebTarget target = getWebTarget();

        // Next, create an Observation belonging to the new patient.
        String patientId = savedCreatedPatient.getId().getValue();
        Observation observation = buildObservation(patientId, "Observation1.json");
        Entity<Observation> obs = Entity.entity(observation, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Observation").request().post(obs, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        String observationId = getLocationLogicalId(response);

        // Next, retrieve the new Observation with a read operation and verify it.
        response = target.path("Observation/" + observationId).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Observation responseObs = response.readEntity(Observation.class);
        savedCreatedObservation = responseObs;

        assertResourceEquals(observation, responseObs);
    }

    /**
     * Tests the update of the original patient that was previously created.
     */
    @Test(groups = { "server-basic" }, dependsOnMethods = { "testCreatePatient" })
    public void testUpdatePatient() throws Exception {
        WebTarget target = getWebTarget();

        // Create a fresh copy of the mock Patient.
        Patient patient = readResource(Patient.class, "Patient_JohnDoe.json");

        // Be sure to set the saved patient's id as well.
        // And add an additional contact phone number
        patient = patient.toBuilder().id(savedCreatedPatient.getId()).telecom(ContactPoint.builder()
                .system(ContactPointSystem.PHONE).use(ContactPointUse.WORK).value(string("999-111-1111")).build())
                .build();

        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);

        // Now call the 'update' API.
        String targetPath = "Patient/" + patient.getId().getValue();
        Response response = target.path(targetPath).request().put(entity, Response.class);
        assertResponse(response, Response.Status.OK.getStatusCode());

        // Get the updated patient's logical id value.
        String patientId = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the updated patient and verify it.
        response = target.path("Patient/" + patientId).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient responsePatient = response.readEntity(Patient.class);

        assertResourceEquals(patient, responsePatient);
    }

    /**
     * Tests the update of the original observation that was previously created.
     */
    @Test(groups = { "server-basic" }, dependsOnMethods = { "testCreateObservation" })
    public void testUpdateObservation() throws Exception {
        WebTarget target = getWebTarget();

        // Create an updated Observation based on the original saved observation
        String patientId = savedCreatedPatient.getId().getValue();
        Observation observation = buildObservation(patientId, "Observation2.json");

        observation = observation.toBuilder().id(savedCreatedObservation.getId()).build();

        Entity<Observation> obs = Entity.entity(observation, FHIRMediaType.APPLICATION_FHIR_JSON);

        // Call the 'update' API.
        String targetPath = "Observation/" + observation.getId().getValue();
        Response response = target.path(targetPath).request().put(obs, Response.class);
        assertResponse(response, Response.Status.OK.getStatusCode());
        String observationId = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the updated observation and verify it.
        response = target.path("Observation/" + observationId).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Observation responseObservation = response.readEntity(Observation.class);
        assertNotNull(responseObservation);
        assertResourceEquals(observation, responseObservation);
    }

    /**
     * Tests the retrieval of the history for a previously saved and updated
     * patient.
     */
    @Test(groups = { "server-basic" }, dependsOnMethods = { "testCreatePatient", "testUpdatePatient" })
    public void testHistoryPatient() {
        WebTarget target = getWebTarget();

        // Call the 'history' API to retrieve both the original and updated versions of
        // the patient.
        String targetPath = "Patient/" + savedCreatedPatient.getId().getValue() + "/_history";
        Response response = target.path(targetPath).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());

        Bundle resources = response.readEntity(Bundle.class);
        assertNotNull(resources);
        assertEquals(2, resources.getEntry().size());
        Patient updatedPatient = (Patient) resources.getEntry().get(0).getResource();
        Patient originalPatient = (Patient) resources.getEntry().get(1).getResource();
        // Make sure patient ids are equal, and versionIds are NOT equal.
        assertEquals(originalPatient.getId().getValue(), updatedPatient.getId().getValue());
        assertTrue(!updatedPatient.getMeta().getVersionId().getValue()
                .contentEquals(originalPatient.getMeta().getVersionId().getValue()));
        // Patient create time should be earlier than Patient update time.       
        assertTrue(originalPatient.getMeta().getLastUpdated().getValue()
                .compareTo(updatedPatient.getMeta().getLastUpdated().getValue()) < 0);

    }

    /**
     * Tests the retrieval of the history for a previously saved and updated
     * observation.
     */
    @Test(groups = { "server-basic" }, dependsOnMethods = { "testCreateObservation", "testUpdateObservation" })
    public void testHistoryObservation() {
        WebTarget target = getWebTarget();

        // Call the 'history' API to retrieve both the original and updated versions of
        // the observation.
        String targetPath = "Observation/" + savedCreatedObservation.getId().getValue() + "/_history";
        Response response = target.path(targetPath).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());

        Bundle resources = response.readEntity(Bundle.class);
        assertNotNull(resources);
        assertEquals(2, resources.getEntry().size());
        Observation updatedObservation = (Observation) resources.getEntry().get(0).getResource();
        Observation originalObservation = (Observation) resources.getEntry().get(1).getResource();
        // Make sure observation ids are equal, and versionIds are NOT equal.
        assertEquals(originalObservation.getId().getValue(), updatedObservation.getId().getValue());
        assertTrue(!updatedObservation.getMeta().getVersionId().getValue()
                .contentEquals(originalObservation.getMeta().getVersionId().getValue()));
        // Observation create time should be earlier than Observation update time.
        assertTrue(originalObservation.getMeta().getLastUpdated().getValue()
                .compareTo(updatedObservation.getMeta().getLastUpdated().getValue()) < 0);
    }

    /**
     * Tests the retrieval of a particular version of a patient.
     */
    @Test(groups = { "server-basic" }, dependsOnMethods = { "testCreatePatient", "testUpdatePatient" })
    public void testVreadPatient() {
        WebTarget target = getWebTarget();

        // Call the 'version read' API to retrieve the original created version of the
        // patient.
        String targetPath = "Patient/" + savedCreatedPatient.getId().getValue() + "/_history/"
                + savedCreatedPatient.getMeta().getVersionId().getValue();
        Response response = target.path(targetPath).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Patient originalPatient = response.readEntity(Patient.class);
        assertNotNull(originalPatient);
        assertResourceEquals(savedCreatedPatient, originalPatient);

        // Now try reading a Patient, passing a bogus version.
        targetPath = "Patient/" + savedCreatedPatient.getId().getValue() + "/_history/" + "-44";
        response = target.path(targetPath).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Tests the retrieval of a particular version of an observation.
     */
    @Test(groups = { "server-basic" }, dependsOnMethods = { "testCreateObservation", "testUpdateObservation" })
    public void testVreadObservation() {
        WebTarget target = getWebTarget();

        // Call the 'version read' API to retrieve the original created version of the
        // observation.
        String targetPath = "Observation/" + savedCreatedObservation.getId().getValue() + "/_history/"
                + savedCreatedObservation.getMeta().getVersionId().getValue();
        Response response = target.path(targetPath).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Observation originalObservation = response.readEntity(Observation.class);
        assertNotNull(originalObservation);
        assertResourceEquals(savedCreatedObservation, originalObservation);

        // Now try reading an Observation, passing a bogus version.
        targetPath = "Observation/" + savedCreatedObservation.getId().getValue() + "/_history/" + "-44";
        response = target.path(targetPath).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Tests a search for a patient.
     */
    @Test(groups = { "server-basic" }, dependsOnMethods = { "testCreatePatient", "testUpdatePatient" })
    public void testSearchPatient() {
        WebTarget target = getWebTarget();

        String familyName = savedCreatedPatient.getName().get(0).getFamily().getValue();
        Response response = target.path("Patient").queryParam("family", familyName)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);
        assertNotNull(bundle);
        assertTrue(bundle.getEntry().size() >= 1);

        assertNotNull(bundle.getTotal());
        assertNotNull(bundle.getTotal().getValue());
        // assertEquals(new Double(bundle.getEntry().size()), new
        // Double(bundle.getTotal().getValue().doubleValue()));

        String homePhone = savedCreatedPatient.getTelecom().get(0).getValue().getValue();
        response = target.path("Patient").queryParam("telecom", homePhone).request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        bundle = response.readEntity(Bundle.class);
        assertNotNull(bundle);
        assertTrue(bundle.getEntry().size() >= 1);
    }

    /**
     * Searches for all Patients, and ensures that no duplicates for the same
     * Patient are returned. (This ensures that multiple versions of a Patient with
     * the same logical id are not returned.)
     */
    @Test(groups = { "server-basic" }, dependsOnMethods = { "testCreatePatient", "testUpdatePatient" })
    public void testSearchAllPatients() {
        WebTarget target = getWebTarget();

        Response response = target.path("Patient").request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);
        assertNotNull(bundle);
        assertTrue(bundle.getEntry().size() >= 1);
        assertNotNull(bundle.getTotal());
        assertNotNull(bundle.getTotal().getValue());

        List<String> patientLogicalIds = new ArrayList<>();
        Patient patient;
        for (Bundle.Entry entry : bundle.getEntry()) {
            patient = (Patient) entry.getResource();
            if (patientLogicalIds.contains(patient.getId().getValue())) {
                fail("Duplicate logicalId found: " + patient.getId().getValue());
            } else {
                patientLogicalIds.add(patient.getId().getValue());
            }
        }
    }

    /**
     * Tests a search for an observation based on its association with a patient.
     */
    @Test(groups = { "server-basic" }, dependsOnMethods = { "testCreatePatient", "testUpdatePatient",
            "testCreateObservation" })
    public void testSearchObservation() {
        WebTarget target = getWebTarget();

        // Next, retrieve the Observation via a search.
        String patientId = savedCreatedPatient.getId().getValue();
        Response response = target.path("Observation").queryParam("subject", "Patient/" + patientId)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);
        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
    }
}
