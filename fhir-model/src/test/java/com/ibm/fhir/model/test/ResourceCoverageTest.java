/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.model.test;

import static com.ibm.fhir.model.type.String.string;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.generator.FHIRGenerator;
import com.ibm.fhir.model.parser.FHIRParser;
import com.ibm.fhir.model.resource.Basic;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.Base64Binary;
import com.ibm.fhir.model.type.CodeableConcept;
import com.ibm.fhir.model.type.ContactDetail;
import com.ibm.fhir.model.type.ContactPoint;
import com.ibm.fhir.model.type.Count;
import com.ibm.fhir.model.type.Decimal;
import com.ibm.fhir.model.type.Distance;
import com.ibm.fhir.model.type.Extension;
import com.ibm.fhir.model.type.MoneyQuantity;
import com.ibm.fhir.model.type.code.ContactPointUse;
import com.ibm.fhir.model.type.code.QuantityComparator;
import com.ibm.fhir.model.type.code.ResourceType;
import com.ibm.fhir.model.type.code.ResourceType.ValueSet;

/**
 * This class exercises the getters in the resource package.
 */
public class ResourceCoverageTest {

    public static byte[] PAYLOAD = "THIS IS A FAKE PAYLOAD".getBytes();

    public static Extension buildBooleanTrue() {
        return Extension.builder().url("https://extension-Boolean")
                .value(com.ibm.fhir.model.type.Boolean.builder().value("TRUE").build()).build();
    }

    public static Extension buildBase64Binary() {
        return Extension.builder().url("https://extension-base64")
                .value(Base64Binary.builder().value(PAYLOAD).build()).build();
    }

    public static Extension buildId() {
        return Extension.builder().url("https://extension-base64")
                .value(Base64Binary.builder().value(PAYLOAD).build()).build();
    }

    public static Extension buildCanonical() {
        return Extension.builder().url("https://extension-Canonical")
                .value(com.ibm.fhir.model.type.Canonical.builder().value("https://mycanonical").build())
                .build();
    }

    public static Extension buildContactDetail() {
        return Extension.builder().url("https://extension-ContactDetail")
                .value(ContactDetail.builder().name(string("Patient P"))
                        .telecom(ContactPoint.builder().use(ContactPointUse.HOME)
                                .value(string("1-111-111-1111")).build())
                        .build())
                .build();
    }

    private static Extension buildCount() {
        Count count =
                Count.builder().comparator(QuantityComparator.GREATER_OR_EQUALS).value(Decimal.of("10.0"))
                        .unit(string("kg")).build();
        return Extension.builder().url("https://extension-Count").value(count).build();
    }

    private static Extension buildDistance() {
        Distance distance = Distance.builder().unit(string("km")).value(Decimal.of("10.0")).build();
        return Extension.builder().url("https://extension-Distance")
                .value(distance).build();
    }

    private static Extension buildMoneyQuantity() {
        MoneyQuantity moneyQuantity = MoneyQuantity.builder().unit(string("$")).value(Decimal.of("10.0")).build();
        return Extension.builder().url("https://extension-MoneyQuantity")
                .value(moneyQuantity).build();
    }

    @Test
    public void testResources() throws Exception {
        List<String> SKIP = Arrays.asList("DomainResource", "Resource", "Builder");
        ValueSet[] values = ResourceType.ValueSet.values();
        for (ValueSet valueSet : values) {
            if (!SKIP.contains(valueSet.value())) {
                Resource resource =
                        TestUtil.readExampleResource("json/ibm/complete-mock/" + valueSet.value() + "-1.json");
                Method[] methods = resource.getClass().getMethods();
                runMethods(resource, methods);
                Class<?>[] clzs = resource.getClass().getClasses();
                for (Class<?> clz : clzs) {

                    if (!SKIP.contains(clz.getSimpleName())) {
                        Method m = resource.getClass().getMethod("get" + clz.getSimpleName());
                        Object o = m.invoke(resource);
                        if (o.getClass().getSimpleName().contains("List")) {
                            @SuppressWarnings("unchecked")
                            List<Object> os = ((List<Object>) o);
                            if (!os.isEmpty()) {
                                runMethods(os.get(0), clz.getMethods());
                            }
                        } else if (!o.getClass().getSimpleName().contains("Class")) {
                            runMethods(o, clz.getMethods());
                        }
                    }
                }
            }
        }
    }
    
    @Test
    public void testResourcesWithXml() throws Exception {
        List<String> SKIP = Arrays.asList("DomainResource", "Resource", "Builder");
        ValueSet[] values = ResourceType.ValueSet.values();
        for (ValueSet valueSet : values) {
            if (!SKIP.contains(valueSet.value())) {
                Resource resource =
                        TestUtil.readExampleResource("xml/ibm/complete-mock/" + valueSet.value() + "-1.xml");

                Resource.Builder builder = resource.toBuilder();
                Method[] methods = builder.getClass().getMethods();
                for(Method method : methods) {
                    if (method.getName().equals("extension") && method.toString().contains("java.util.Collection")) {
                        List<Extension> extensions = Arrays.asList(buildBooleanTrue());
                        builder = (Resource.Builder) method.invoke(builder, extensions);
                    }
                    
                    if (method.getName().equals("modifierExtension") && method.toString().contains("java.util.Collection")) {
                        List<Extension> extensions = Arrays.asList(buildBooleanTrue());
                        builder = (Resource.Builder) method.invoke(builder, extensions);
                    }
                }
                resource = builder.build();
                
                try (StringWriter writer = new StringWriter()) {
                    FHIRGenerator.generator(Format.JSON).generate(resource, writer);
                    String outJson = writer.toString();
                    assertNotNull(outJson);
                    assertFalse(outJson.isEmpty());
                    
                    try (ByteArrayInputStream in = new ByteArrayInputStream(outJson.getBytes())) {
                        Resource resource2 = FHIRParser.parser(Format.JSON).parse(in).as(Resource.class);
                        assertNotNull(resource2);
                        assertTrue(true);
                    }
                }
                
                try (StringWriter writer = new StringWriter()) {
                    FHIRGenerator.generator(Format.XML).generate(resource, writer);
                    String outXML = writer.toString();
                    assertNotNull(outXML);
                    assertFalse(outXML.isEmpty());
                    
                    try (ByteArrayInputStream in = new ByteArrayInputStream(outXML.getBytes())) {
                        Resource resource2 = FHIRParser.parser(Format.XML).parse(in).as(Resource.class);
                        assertNotNull(resource2);
                        assertTrue(true);
                    }
                }
            }
        }
    }

    public void runMethods(Object resource, Method[] methods) throws Exception {
        for (Method method : methods) {
            if (method.getName().startsWith("get")) {
                method.invoke(resource);
                assertTrue(true);
            }

            if (method.getName().equals("hashCode")) {
                method.invoke(resource);
                assertTrue(true);
            }

            if (method.getName().equals("equals")) {
                method.invoke(resource, resource);
                assertTrue(true);

                String x = "BAD";
                method.invoke(resource, x);
                assertTrue(true);
                
                if (resource instanceof Resource) {
                    Resource r = (Resource) resource; 
                    Resource r2 = r.toBuilder().build(); 
                    assertTrue(r.equals(r2));
                }
            }

            if (method.getName().equals("hasChildren")) {
                method.invoke(resource);
                assertTrue(true);
            }
        }
    }

    public void buildTestForUncoveredTypes() throws Exception {
        List<Extension> extensions = new ArrayList<>();
        extensions.add(buildBase64Binary());
        extensions.add(buildBooleanTrue());
        extensions.add(buildCanonical());
        extensions.add(buildId());
        extensions.add(buildContactDetail());
        extensions.add(buildCount());
        extensions.add(buildDistance());
        extensions.add(buildMoneyQuantity());

        CodeableConcept code = CodeableConcept.builder().id("1-2-3-4").text(string("Demo")).build();
        Basic basic = Basic.builder().code(code).extension(extensions).build();

        try (StringWriter writer = new StringWriter()) {
            FHIRGenerator.generator(Format.XML).generate(basic, writer);

            String out = writer.toString();
            assertNotNull(out);
            assertFalse(out.isEmpty());
            System.out.println(out);

            try (ByteArrayInputStream in = new ByteArrayInputStream(out.getBytes())) {
                Basic basicParsed = FHIRParser.parser(Format.XML).parse(in).as(Basic.class);
                assertNotNull(basicParsed);
                assertTrue(true);
            }
        }

        try (StringWriter writer = new StringWriter()) {
            FHIRGenerator.generator(Format.JSON).generate(basic, writer);

            String out = writer.toString();
            assertNotNull(out);
            assertFalse(out.isEmpty());
            System.out.println(out);

            try (ByteArrayInputStream in = new ByteArrayInputStream(out.getBytes())) {
                Basic basicParsed = FHIRParser.parser(Format.JSON).parse(in).as(Basic.class);
                assertNotNull(basicParsed);
                assertTrue(true);
            }
        }
    }
}