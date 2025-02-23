/*
 * Copyright © 2020-2025 ForgeRock AS (obst@forgerock.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.forgerock.sapi.gateway.ob.uk.rs.server.service.frequency;

import com.forgerock.sapi.gateway.uk.common.shared.api.meta.forgerock.FRFrequencyType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Unit test for {@link FrequencyService}.
 */
public class FrequencyServiceTest {
    // (Saturday 6th Feb 2021)
    private static final DateTime PREVIOUS_DATE_TIME = new DateTime(2021, 2, 6, 11, 0);

    // EVERYDAY("EvryDay")
    @Test
    public void shouldMatchPattern_everyDay() {
        // Given
        String frequency = FRFrequencyType.EVERYDAY.getFrequencyStr();

        // When
        DateTime dateTime = FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency);

        // Then
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(PREVIOUS_DATE_TIME.getYear());
        assertThat(dateTime.getMonthOfYear()).isEqualTo(PREVIOUS_DATE_TIME.getMonthOfYear());
        assertThat(dateTime.getDayOfMonth()).isEqualTo(PREVIOUS_DATE_TIME.getDayOfMonth() + 1);
    }

    @Test
    public void shouldRaiseException_everyDay() {
        // Given
        String frequency = FRFrequencyType.EVERYDAY.getFrequencyStr() + "x";

        // When
        IllegalArgumentException e = catchThrowableOfType(() ->
                FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency), IllegalArgumentException.class);

        // Then
        assertThat(e.getMessage()).isEqualTo("Frequency type value not found: " + frequency);
    }

    // EVERYWORKINGDAY("EvryWorkgDay"),
    @Test
    public void shouldMatchPattern_everyWorkingDay() {
        // Given
        String frequency = FRFrequencyType.EVERYWORKINGDAY.getFrequencyStr();

        // When
        DateTime dateTime = FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency);

        // Then
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(PREVIOUS_DATE_TIME.getYear());
        assertThat(dateTime.getMonthOfYear()).isEqualTo(PREVIOUS_DATE_TIME.getMonthOfYear());
        assertThat(dateTime.getDayOfMonth()).isEqualTo(PREVIOUS_DATE_TIME.getDayOfMonth() + 2);
    }

    @Test
    public void shouldRaiseException_everyWorkingDay() {
        // Given
        String frequency = FRFrequencyType.EVERYWORKINGDAY.getFrequencyStr() + "x";

        // When
        IllegalArgumentException e = catchThrowableOfType(() ->
                FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency), IllegalArgumentException.class);

        // Then
        assertThat(e.getMessage()).isEqualTo("Frequency type value not found: " + frequency);
    }

    // INTERVALWEEKDAY("IntrvlWkDay", "0?([1-9]):0?([1-7])$")
    @Test
    public void shouldMatchPattern_weekday() {
        // Given
        String frequency = FRFrequencyType.INTERVALWEEKDAY.getFrequencyStr() + ":01:07";

        // When
        DateTime dateTime = FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency);

        // Then
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(PREVIOUS_DATE_TIME.getYear());
        assertThat(dateTime.getMonthOfYear()).isEqualTo(PREVIOUS_DATE_TIME.getMonthOfYear());
        assertThat(dateTime.getDayOfMonth()).isEqualTo(PREVIOUS_DATE_TIME.getDayOfMonth() + 8);
    }

    @Test
    public void shouldRaiseException_weekday() {
        // Given
        FRFrequencyType frequencyType = FRFrequencyType.INTERVALWEEKDAY;
        String frequency = frequencyType.getFrequencyStr() + ":00:07";

        // When
        IllegalArgumentException e = catchThrowableOfType(() ->
                FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency), IllegalArgumentException.class);

        // Then
        assertThat(e.getMessage()).isEqualTo("Frequency '"+frequency+"' doesn't match regex '"+frequencyType.getPattern()+"'");
    }

    // WEEKINMONTHDAY("WkInMnthDay", "0?([1-5]):0?([1-7])$")
    @Test
    public void shouldMatchPattern_weekInMonthDay() {
        // Given
        String frequency = FRFrequencyType.WEEKINMONTHDAY.getFrequencyStr() + ":02:03";

        // When
        DateTime dateTime = FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency);

        // Then
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(PREVIOUS_DATE_TIME.getYear());
        assertThat(dateTime.getMonthOfYear()).isEqualTo(PREVIOUS_DATE_TIME.getMonthOfYear() + 1);
        assertThat(dateTime.getDayOfMonth()).isEqualTo(2 * 7 + 3);
    }

    @Test
    public void shouldRaiseException_weekInMonthDay() {
        // Given
        FRFrequencyType frequencyType = FRFrequencyType.WEEKINMONTHDAY;
        String frequency = frequencyType.getFrequencyStr() + ":00:03";

        // When
        IllegalArgumentException e = catchThrowableOfType(() ->
                FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency), IllegalArgumentException.class);

        // Then
        assertThat(e.getMessage()).isEqualTo("Frequency '"+frequency+"' doesn't match regex '"+frequencyType.getPattern()+"'");
    }

    // INTERVALMONTHDAY("IntrvlMnthDay", "(0?[1-6]|12|24):(-0?[1-5]|0?[1-9]|[12][0-9]|3[01])$")
    @Test
    public void shouldMatchPattern_intervalMonthDay() {
        // Given
        String frequency = FRFrequencyType.INTERVALMONTHDAY.getFrequencyStr() + ":01:30";

        // When
        DateTime dateTime = FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency);

        // Then
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(PREVIOUS_DATE_TIME.getYear());
        assertThat(dateTime.getMonthOfYear()).isEqualTo(PREVIOUS_DATE_TIME.getMonthOfYear() + 1);
        assertThat(dateTime.getDayOfMonth()).isEqualTo(30);
    }

    @Test
    public void shouldRaiseException_intervalMonthDay() {
        // Given
        FRFrequencyType frequencyType = FRFrequencyType.INTERVALMONTHDAY;
        String frequency = frequencyType.getFrequencyStr() + ":01:-8";

        // When
        IllegalArgumentException e = catchThrowableOfType(() ->
                FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency), IllegalArgumentException.class);

        // Then
        assertThat(e.getMessage()).isEqualTo("Frequency '"+frequency+"' doesn't match regex '"+frequencyType.getPattern()+"'");
    }

    // QUARTERDAY("QtrDay", "(ENGLISH|SCOTTISH|RECEIVED)$")
    @Test
    public void shouldMatchPattern_quarterDay() {
        // Given
        String frequency = FRFrequencyType.QUARTERDAY.getFrequencyStr() + ":ENGLISH";

        // When
        DateTime dateTime = FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency);

        // Then
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(PREVIOUS_DATE_TIME.getYear());
        assertThat(dateTime.getMonthOfYear()).isEqualTo(PREVIOUS_DATE_TIME.getMonthOfYear() + 1);
        assertThat(dateTime.getDayOfMonth()).isEqualTo(25);
    }

    @Test
    public void shouldRaiseException_quarterDay() {
        // Given
        FRFrequencyType frequencyType = FRFrequencyType.QUARTERDAY;
        String frequency = frequencyType.getFrequencyStr() + ":SENT";

        // When
        IllegalArgumentException e = catchThrowableOfType(() ->
                FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency), IllegalArgumentException.class);

        // Then
        assertThat(e.getMessage()).isEqualTo("Frequency '"+frequency+"' doesn't match regex '"+frequencyType.getPattern()+"'");
    }

    // INTERVALDAY("IntrvlDay", "(0?[2-9]|[1-2][0-9]|3[0-1])$");
    @Test
    public void shouldMatchPattern_intervalDay() {
        // Given
        String frequency = FRFrequencyType.INTERVALDAY.getFrequencyStr() + ":02";

        // When
        DateTime dateTime = FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency);

        // Then
        assertThat(dateTime).isNotNull();
        assertThat(dateTime.getYear()).isEqualTo(PREVIOUS_DATE_TIME.getYear());
        assertThat(dateTime.getMonthOfYear()).isEqualTo(PREVIOUS_DATE_TIME.getMonthOfYear());
        assertThat(dateTime.getDayOfMonth()).isEqualTo(PREVIOUS_DATE_TIME.getDayOfMonth() + 2);
    }

    @Test
    public void shouldRaiseException_intervalDay() {
        // Given
        FRFrequencyType frequencyType = FRFrequencyType.INTERVALDAY;
        String frequency = frequencyType.getFrequencyStr() + ":1";

        // When
        IllegalArgumentException e = catchThrowableOfType(() ->
                FrequencyService.getNextDateTime(PREVIOUS_DATE_TIME, frequency), IllegalArgumentException.class);

        // Then
        assertThat(e.getMessage()).isEqualTo("Frequency '"+frequency+"' doesn't match regex '"+frequencyType.getPattern()+"'");
    }

}
