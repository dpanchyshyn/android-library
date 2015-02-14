package com.urbanairship.analytics;

import android.os.Bundle;

import com.urbanairship.RobolectricGradleTestRunner;
import com.urbanairship.TestApplication;
import com.urbanairship.push.PushManager;
import com.urbanairship.push.PushMessage;
import com.urbanairship.richpush.RichPushMessage;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class CustomEventTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();
    PushManager pushManager;
    Analytics analytics;

    @Before
    public void setup() {
        analytics = mock(Analytics.class);
        TestApplication.getApplication().setAnalytics(analytics);

        pushManager = mock(PushManager.class);
        TestApplication.getApplication().setPushManager(pushManager);
    }

    /**
     * Test creating a custom event.
     */
    @Test
    public void testCustomEvent() throws JSONException {
        String eventName = createFixedSizeString('a', 255);
        String interactionId = createFixedSizeString('b', 255);
        String interactionType = createFixedSizeString('c', 255);
        String transactionId = createFixedSizeString('d', 255);

        CustomEvent event = new CustomEvent.Builder(eventName)
                .setTransactionId(transactionId)
                .setInteraction(interactionType, interactionId)
                .setEventValue(100.123456)
                .create();

        EventTestUtils.validateEventValue(event, "event_name", eventName);
        EventTestUtils.validateEventValue(event, "event_value", 100123456L);
        EventTestUtils.validateEventValue(event, "transaction_id", transactionId);
        EventTestUtils.validateEventValue(event, "interaction_id", interactionId);
        EventTestUtils.validateEventValue(event, "interaction_type", interactionType);
    }

    /**
     * Test creating a custom event with a null event name throws an exception.
     */
    @Test
    public void testNullEventName() {
        exception.expect(IllegalArgumentException.class);
        new CustomEvent.Builder(null);
    }

    /**
     * Test creating a custom event with an empty event name throws an exception.
     */
    @Test
    public void testEmptyEventName() {
        exception.expect(IllegalArgumentException.class);
        new CustomEvent.Builder("");
    }

    /**
     * Test creating a custom event with a name longer than 255 characters throws
     * an exception.
     */
    @Test
    public void testEventNameExceedsMaxLength() {
        exception.expect(IllegalArgumentException.class);
        String eventName = createFixedSizeString('a', 256);
        new CustomEvent.Builder(eventName);
    }

    /**
     * Test setting a interaction ID that is longer than 255 characters throws an
     * exception.
     */
    @Test
    public void testInteractionIDExceedsMaxLength() {
        exception.expect(IllegalArgumentException.class);
        new CustomEvent.Builder("event name")
                .setInteraction("interaction type", createFixedSizeString('a', 256));
    }

    /**
     * Test setting a attribution type that is longer than 255 characters throws an
     * exception.
     */
    @Test
    public void testInteractionTypeExceedsMaxLength() {
        exception.expect(IllegalArgumentException.class);
        new CustomEvent.Builder("event name")
                .setInteraction(createFixedSizeString('a', 256), "interaction id");
    }

    /**
     * Test setting a transaction ID that is longer than 255 characters throws an
     * exception.
     */
    @Test
    public void testTransactionIDExceedsMaxLength() {
        exception.expect(IllegalArgumentException.class);
        new CustomEvent.Builder("event name")
                .setTransactionId(createFixedSizeString('a', 256));
    }

    /**
     * Test Builder.AddEvent creates and adds the event to analytics.
     */
    @Test
    public void testAddEvent() {
        CustomEvent event = new CustomEvent.Builder("event name").addEvent();

        ArgumentCaptor<Event> argument = ArgumentCaptor.forClass(Event.class);
        verify(analytics).addEvent(argument.capture());

        assertEquals("Add event should add the event.", event, argument.getValue());
    }

    /**
     * Test creating a custom event includes the hard conversion send id if set.
     */
    @Test
    public void testHardConversionId() throws JSONException {
        CustomEvent event = new CustomEvent.Builder("event name").create();
        when(analytics.getConversionSendId()).thenReturn("send id");
        EventTestUtils.validateEventValue(event, "conversion_send_id", "send id");
    }

    /**
     * Test creating a custom event includes the last received send id.
     */
    @Test
    public void testLastSendId() throws JSONException {
        when(pushManager.getLastReceivedSendId()).thenReturn("last send id");

        CustomEvent event = new CustomEvent.Builder("event name").create();

        EventTestUtils.validateEventValue(event, "last_received_send_id", "last send id");
    }

    /**
     * Test creating a custom event includes only the hard id if set and not the last send.
     */
    @Test
    public void testHardConversionIDAndLastSendId() throws JSONException {
        when(analytics.getConversionSendId()).thenReturn("send id");
        when(pushManager.getLastReceivedSendId()).thenReturn("last send id");

        CustomEvent event = new CustomEvent.Builder("event name").create();

        EventTestUtils.validateEventValue(event, "last_received_send_id", null);
        EventTestUtils.validateEventValue(event, "conversion_send_id", "send id");
    }

    /**
     * Test adding a custom event with interaction from a message.
     */
    @Test
    public void testInteractionFromMessage() throws JSONException {
        RichPushMessage message = mock(RichPushMessage.class);
        when(message.getMessageId()).thenReturn("message id");

        CustomEvent event = new CustomEvent.Builder("event name")
                .setInteraction(message)
                .create();

        EventTestUtils.validateEventValue(event, "interaction_id", "message id");
        EventTestUtils.validateEventValue(event, "interaction_type", "ua_mcrap");
    }

    /**
     * Test adding a custom event with a custom interaction.
     */
    @Test
    public void testCustomInteraction() throws JSONException {
        CustomEvent event = new CustomEvent.Builder("event name")
                .setInteraction("interaction type", "interaction id")
                .create();

        EventTestUtils.validateEventValue(event, "interaction_id", "interaction id");
        EventTestUtils.validateEventValue(event, "interaction_type", "interaction type");
    }

    /**
     * Test adding an interaction with a null id is allowed.
     */
    @Test
    public void testCustomInteractionNullID() throws JSONException {
        CustomEvent event = new CustomEvent.Builder("event name")
                .setInteraction("interaction type", null)
                .create();

        EventTestUtils.validateEventValue(event, "interaction_id", null);
        EventTestUtils.validateEventValue(event, "interaction_type", "interaction type");
    }

    /**
     * Test adding an interaction with a null type is allowed.
     */
    @Test
    public void testCustomInteractionNullType() throws JSONException {
        CustomEvent event = new CustomEvent.Builder("event name")
                .setInteraction(null, "interaction id")
                .create();

        EventTestUtils.validateEventValue(event, "interaction_type", null);
        EventTestUtils.validateEventValue(event, "interaction_id", "interaction id");
    }

    /**
     * Test creating a custom event without an interaction, last send id, or
     * conversion push id will be empty.
     */
    @Test
    public void testCustomInteractionEmpty() throws JSONException {
        when(pushManager.getLastReceivedSendId()).thenReturn("last send id");

        CustomEvent event = new CustomEvent.Builder("event name")
                .create();

        EventTestUtils.validateEventValue(event, "interaction_type", null);
        EventTestUtils.validateEventValue(event, "interaction_id", null);
    }

    /**
     * Test setting the event value to various valid values.
     */
    @Test
    public void testSetEventValue() throws JSONException {
        CustomEvent event;

        // Max integer
        event = new CustomEvent.Builder("event name").setEventValue(Integer.MAX_VALUE).create();
        EventTestUtils.validateEventValue(event, "event_value", 2147483647000000L);

        // Min integer
        event = new CustomEvent.Builder("event name").setEventValue(Integer.MIN_VALUE).create();
        EventTestUtils.validateEventValue(event, "event_value", -2147483648000000L);

        // 0
        event = new CustomEvent.Builder("event name").setEventValue(0).create();
        EventTestUtils.validateEventValue(event, "event_value", 0);

        // Min double (very small number) - should be 0.
        event = new CustomEvent.Builder("event name").setEventValue(Double.MIN_VALUE).create();
        EventTestUtils.validateEventValue(event, "event_value", 0);

        // Max supported double
        event = new CustomEvent.Builder("event name").setEventValue((double) Integer.MAX_VALUE).create();
        EventTestUtils.validateEventValue(event, "event_value", 2147483647000000L);

        // Min supported double
        event = new CustomEvent.Builder("event name").setEventValue((double) Integer.MIN_VALUE).create();
        EventTestUtils.validateEventValue(event, "event_value", -2147483648000000L);

        // Max supported String
        event = new CustomEvent.Builder("event name").setEventValue(String.valueOf(Integer.MAX_VALUE)).create();
        EventTestUtils.validateEventValue(event, "event_value", 2147483647000000L);

        // Min supported String
        event = new CustomEvent.Builder("event name").setEventValue(String.valueOf(Integer.MIN_VALUE)).create();
        EventTestUtils.validateEventValue(event, "event_value", -2147483648000000L);

        // "0"
        event = new CustomEvent.Builder("event name").setEventValue("0").create();
        EventTestUtils.validateEventValue(event, "event_value", 0);

        // null String
        event = new CustomEvent.Builder("event name").setEventValue((String) null).create();
        EventTestUtils.validateEventValue(event, "event_value", null);

        // Some Big Decimal
        event = new CustomEvent.Builder("event name").setEventValue(new BigDecimal(123)).create();
        EventTestUtils.validateEventValue(event, "event_value", 123000000L);

        // Max supported Big Decimal
        BigDecimal maxDecimal = new BigDecimal(Integer.MAX_VALUE);
        event = new CustomEvent.Builder("event name").setEventValue(maxDecimal).create();
        EventTestUtils.validateEventValue(event, "event_value", 2147483647000000L);

        // Min supported Big Decimal
        BigDecimal minDecimal = new BigDecimal(Integer.MIN_VALUE);
        event = new CustomEvent.Builder("event name").setEventValue(minDecimal).create();
        EventTestUtils.validateEventValue(event, "event_value", -2147483648000000L);

        // null Big Decimal
        event = new CustomEvent.Builder("event name").setEventValue((BigDecimal) null).create();
        EventTestUtils.validateEventValue(event, "event_value", null);
    }


    /**
     * Test setting event value to positive infinity throws an exception.
     */
    @Test
    public void testSetEventValuePositiveInfinity() {
        exception.expect(NumberFormatException.class);
        new CustomEvent.Builder("event name").setEventValue(Double.POSITIVE_INFINITY);
    }

    /**
     * Test setting event value to negative infinity throws an exception.
     */
    @Test
    public void testSetEventValueNegativeInfiinty() {
        exception.expect(NumberFormatException.class);
        new CustomEvent.Builder("event name").setEventValue(Double.NEGATIVE_INFINITY);
    }

    /**
     * Test setting event value to Double.NaN throws an exception.
     */
    @Test
    public void testSetEventValueDoubleNAN() {
        exception.expect(NumberFormatException.class);
        new CustomEvent.Builder("event name").setEventValue(Double.NaN);
    }

    /**
     * Test setting event value above the max allowed throws an exception.
     */
    @Test
    public void testEventValueDoubleAboveMax() {
        exception.expect(IllegalArgumentException.class);
        new CustomEvent.Builder("event name").setEventValue(Integer.MAX_VALUE + .000001);

    }

    /**
     * Test setting event value below the min allowed throws an exception.
     */
    @Test
    public void testEventValueDoubleBelowMin() {
        exception.expect(IllegalArgumentException.class);
        new CustomEvent.Builder("event name").setEventValue(Integer.MIN_VALUE - .000001);
    }

    /**
     * Test setting event value to a string that is not a number throws an exception.
     */
    @Test
    public void testEventValueStringNAN() {
        exception.expect(NumberFormatException.class);
        new CustomEvent.Builder("event name").setEventValue("not a number!");
    }

    /**
     * Test setting event value above the max allowed throws an exception.
     */
    @Test
    public void testEventValueStringAboveMax() {
        exception.expect(IllegalArgumentException.class);
        new CustomEvent.Builder("event name").setEventValue(String.valueOf(Integer.MAX_VALUE + 0.000001));
    }

    /**
     * Test setting event value below the min allowed throws an exception.
     */
    @Test
    public void testEventValueStringBelowMin() {
        exception.expect(IllegalArgumentException.class);
        new CustomEvent.Builder("event name").setEventValue(String.valueOf(Integer.MIN_VALUE - 0.000001));
    }

    /**
     * Test setting event value above the max allowed throws an exception.
     */
    @Test
    public void testEventValueBigDecimalAboveMax() {
        exception.expect(IllegalArgumentException.class);
        BigDecimal overMax = new BigDecimal(Integer.MAX_VALUE).add(BigDecimal.valueOf(0.000001));
        new CustomEvent.Builder("event name").setEventValue(overMax);
    }

    /**
     * Test setting event value below the min allowed throws an exception.
     */
    @Test
    public void testEventValueBigDecimalBelowMin() {
        exception.expect(IllegalArgumentException.class);
        BigDecimal belowMin = new BigDecimal(Integer.MIN_VALUE).subtract(BigDecimal.valueOf(0.000001));
        new CustomEvent.Builder("event name").setEventValue(belowMin);
    }

    /**
     * Test setting the attribution directly from a push message.
     */
    @Test
    public void testAttributionFromPushMessage() throws JSONException {
        Bundle pushBundle = new Bundle();
        pushBundle.putString(PushMessage.EXTRA_SEND_ID, "send id");
        PushMessage pushMessage = new PushMessage(pushBundle);

        CustomEvent event = new CustomEvent.Builder("event name")
                .setAttribution(pushMessage)
                .create();

        EventTestUtils.validateEventValue(event, "conversion_send_id", "send id");
    }

    /**
     * Helper method to create a fixed size string with a repeating character.
     *
     * @param repeat The character to repeat.
     * @param length Length of the String.
     * @return A fixed size string.
     */
    private String createFixedSizeString(char repeat, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(repeat);
        }
        return builder.toString();
    }
}