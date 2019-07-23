/**
 * (C) Copyright IBM Corp. 2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.watsonhealth.fhir.model.path;

import static com.ibm.watsonhealth.fhir.model.path.util.FHIRPathUtil.getTemporalAmount;
import static com.ibm.watsonhealth.fhir.model.path.util.FHIRPathUtil.getTemporalAccessor;
import static com.ibm.watsonhealth.fhir.model.path.util.FHIRPathUtil.getTemporal;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Objects;

public class FHIRPathDateTimeValue extends FHIRPathAbstractNode implements FHIRPathPrimitiveValue {
    private static final DateTimeFormatter DATE_TIME_PARSER_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy")
            .optionalStart()
                .appendPattern("-MM")
                .optionalStart()
                    .appendPattern("-dd")
                    .optionalStart()
                        .appendPattern("'T'HH:mm:ss")
                        .optionalStart()
                            .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                        .optionalEnd()
                        .appendPattern("XXX")
                    .optionalEnd()
                .optionalEnd()
            .optionalEnd()
            .toFormatter();

    private final TemporalAccessor dateTime;
    
    protected FHIRPathDateTimeValue(Builder builder) {
        super(builder);
        dateTime = builder.dateTime;
    }
    
    @Override
    public boolean isDateTimeValue() {
        return true;
    }
    
    public boolean isPartial() {
        return !(dateTime instanceof ZonedDateTime);
    }
    
    public TemporalAccessor dateTime() {
        return dateTime;
    }
    
    public static FHIRPathDateTimeValue dateTimeValue(String dateTime) {
        return FHIRPathDateTimeValue.builder(DATE_TIME_PARSER_FORMATTER.parseBest(dateTime, ZonedDateTime::from, LocalDate::from, YearMonth::from, Year::from)).build();
    }
    
    public static FHIRPathDateTimeValue dateTimeValue(TemporalAccessor dateTime) {
        return FHIRPathDateTimeValue.builder(dateTime).build();
    }
    
    public static FHIRPathDateTimeValue dateTimeValue(String name, TemporalAccessor dateTime) {
        return FHIRPathDateTimeValue.builder(dateTime).name(name).build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(type, dateTime);
    }
    
    public static Builder builder(TemporalAccessor dateTime) {
        return new Builder(FHIRPathType.SYSTEM_DATE_TIME, dateTime);
    }
    
    public static class Builder extends FHIRPathAbstractNode.Builder {
        private final TemporalAccessor dateTime;
        
        private Builder(FHIRPathType type, TemporalAccessor dateTime) {
            super(type);
            this.dateTime = dateTime;
        }
        
        @Override
        public Builder name(String name) {
            return (Builder) super.name(name);
        }
        
        @Override
        public Builder value(FHIRPathPrimitiveValue value) {
            return this;
        }
        
        @Override
        public Builder children(FHIRPathNode... children) {
            return this;
        }
        
        @Override
        public Builder children(Collection<FHIRPathNode> children) {
            return this;
        }

        @Override
        public FHIRPathDateTimeValue build() {
            return new FHIRPathDateTimeValue(this);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FHIRPathDateTimeValue other = (FHIRPathDateTimeValue) obj;
        return Objects.equals(dateTime, other.dateTime);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(dateTime);
    }
    
    @Override
    public boolean isComparableTo(FHIRPathNode other) {
        return other instanceof FHIRPathDateTimeValue;
    }

    @Override
    public int compareTo(FHIRPathNode other) {
        if (!isComparableTo(other)) {
            throw new IllegalArgumentException();
        }
        FHIRPathDateTimeValue value = (FHIRPathDateTimeValue) other;
        if (dateTime instanceof Year || value.dateTime instanceof Year) {
            return Year.from(dateTime).compareTo(Year.from(value.dateTime));
        }
        if (dateTime instanceof YearMonth || value.dateTime instanceof YearMonth) {
            return YearMonth.from(dateTime).compareTo(YearMonth.from(value.dateTime));
        }
        if (dateTime instanceof LocalDate || value.dateTime instanceof LocalDate) {
            return LocalDate.from(dateTime).compareTo(LocalDate.from(value.dateTime));
        }
        return ZonedDateTime.from(dateTime).compareTo(ZonedDateTime.from(value.dateTime));
    }
    
    public FHIRPathDateTimeValue add(FHIRPathQuantityNode quantityNode) {
        TemporalAmount temporalAmount = getTemporalAmount(quantityNode);
        Temporal temporal = getTemporal(dateTime);
        return dateTimeValue(getTemporalAccessor(temporal.plus(temporalAmount), dateTime.getClass()));
    }
    
    public FHIRPathDateTimeValue subtract(FHIRPathQuantityNode quantityNode) {
        TemporalAmount temporalAmount = getTemporalAmount(quantityNode);
        Temporal temporal = getTemporal(dateTime);
        return dateTimeValue(getTemporalAccessor(temporal.minus(temporalAmount), dateTime.getClass()));
    }
    
    @Override
    public String toString() {
        return DATE_TIME_PARSER_FORMATTER.format(dateTime);
    }
    
    public static void main(String[] args) {
        System.out.println(LocalDate.from(Year.now()));
    }
}
