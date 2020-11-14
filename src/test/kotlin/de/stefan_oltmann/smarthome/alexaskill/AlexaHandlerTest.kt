/*
 * Stefans Smart Home Alexa Skill
 * Copyright (C) 2020 Stefan Oltmann
 * https://github.com/StefanOltmann
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

package de.stefan_oltmann.smarthome.alexaskill

import com.google.gson.GsonBuilder
import de.stefan_oltmann.smarthome.alexaskill.alexamodel.AlexaRequest
import de.stefan_oltmann.smarthome.alexaskill.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import retrofit2.Call
import retrofit2.Response

internal class AlexaHandlerTest {

    private val gson by lazy {

        val builder = GsonBuilder()
        builder.setPrettyPrinting()
        builder.create()
    }

    /**
     * The handler in unit testing mode returns static messageIds and timestamps.
     *
     * This way we can easily just compare JSON input and output.
     */
    private val handler = AlexaHandler().apply { unitTesting = true }

    /**
     * A namespace that is not handled by this skill should result in an proper error reponse.
     */
    @Test
    fun `Handle invalid Request`() {

        val requestJson = readJson("invalid_directive.json")

        val restApiMock = mock(RestApi::class.java)

        val actualResponseJson = handler.handleRequestJson(requestJson, restApiMock)

        val expectedResponseJson = readJson("invalid_response.json")

        assertEquals(expectedResponseJson, actualResponseJson)

        verifyNoInteractions(restApiMock)
    }

    /**
     * We always accept authorization requests.
     *
     * See https://developer.amazon.com/en-US/docs/alexa/device-apis/alexa-authorization.html
     */
    @Test
    fun `Handle Authorization Request`() {

        val requestJson = readJson("authorization_directive.json")

        val restApiMock = mock(RestApi::class.java)

        val actualResponseJson = handler.handleRequestJson(requestJson, restApiMock)

        val expectedResponseJson = readJson("authorization_response.json")

        assertEquals(expectedResponseJson, actualResponseJson)

        verifyNoInteractions(restApiMock)
    }

    /**
     * This checks the device discovery routine.
     */
    @Test
    fun `Handle Discovery Request`() {

        val requestJson = readJson("discovery_directive.json")

        /*
         * Return mock data if the restApi is called.
         */

        val deviceList = DeviceList(listOf(
                Device("power_plug",
                        "Switchable device",
                        "Device that only has a power state.",
                        DeviceCategory.SWITCH,
                        listOf(DeviceCapability.POWER_STATE)),
                Device(
                        "dimmer",
                        "Dimmer",
                        "Device that handles power states as well as percentages.",
                        DeviceCategory.LIGHT,
                        listOf(DeviceCapability.POWER_STATE, DeviceCapability.PERCENTAGE)
                ),
                Device(
                        "rollershutter",
                        "Rollershutter",
                        "Device that only takes percentage.",
                        DeviceCategory.EXTERIOR_BLIND,
                        listOf(DeviceCapability.PERCENTAGE)
                )
        ))

        val restApiMock = mock(RestApi::class.java)

        val callMock = mock(Call::class.java) as Call<DeviceList>

        `when`(restApiMock.findAllDevices()).thenReturn(callMock)
        `when`(callMock.execute()).thenReturn(Response.success(deviceList))

        /*
         * Check response
         */

        val actualResponseJson = handler.handleRequestJson(requestJson, restApiMock)

        val expectedResponseJson = readJson("discovery_response.json")

        assertEquals(expectedResponseJson, actualResponseJson)

        /*
         * Verify mocks
         */

        verify(restApiMock, times(1)).findAllDevices()
        verifyNoMoreInteractions(restApiMock)
    }

    /**
     * This turns a light on.
     */
    @Test
    fun `Handle Power Controller Request`() {

        val requestJson = readJson("power_controller_directive.json")

        /*
         * Return mock data if the restApi is called.
         */

        val restApiMock = mock(RestApi::class.java)

        val callMock = mock(Call::class.java) as Call<Void>

        `when`(restApiMock.setPowerState("my_light_switch", DevicePowerState.ON)).thenReturn(callMock)
        `when`(callMock.execute()).thenReturn(Response.success(null))

        /*
         * Check response
         */

        val actualResponseJson = handler.handleRequestJson(requestJson, restApiMock)

        val expectedResponseJson = readJson("power_controller_response.json")

        assertEquals(expectedResponseJson, actualResponseJson)

        /*
         * Verify mocks
         */

        verify(restApiMock, times(1)).setPowerState("my_light_switch", DevicePowerState.ON)
        verifyNoMoreInteractions(restApiMock)
    }

    /**
     * Dimms a light.
     */
    @Test
    fun `Handle Percentage Controller Request`() {

        val requestJson = readJson("percentage_controller_directive.json")

        /*
         * Return mock data if the restApi is called.
         */

        val restApiMock = mock(RestApi::class.java)

        val callMock = mock(Call::class.java) as Call<Void>

        `when`(restApiMock.setPercentage("my_dimmer", 66)).thenReturn(callMock)
        `when`(callMock.execute()).thenReturn(Response.success(null))

        /*
         * Check response
         */

        val actualResponseJson = handler.handleRequestJson(requestJson, restApiMock)

        val expectedResponseJson = readJson("percentage_controller_response.json")

        assertEquals(expectedResponseJson, actualResponseJson)

        /*
         * Verify mocks
         */

        verify(restApiMock, times(1)).setPercentage("my_dimmer", 66)
        verifyNoMoreInteractions(restApiMock)
    }

    /*
     * Example test to verify GSON is working
     */
    @Test
    fun `Parse AlexaRequest object from Discovery request`() {

        val requestJson = readJson("discovery_directive.json")

        val alexaRequest = gson.fromJson(requestJson, AlexaRequest::class.java)

        assertNotNull(alexaRequest, "Parsing into object failed.")

        val directive = alexaRequest.directive

        assertNotNull(directive)

        // check header
        assertNotNull(directive.header)
        assertEquals("Alexa.Discovery", directive.header.namespace)
        assertEquals("Discover", directive.header.name)
        assertEquals("3", directive.header.payloadVersion)
        assertEquals("<message id>", directive.header.messageId)

        // check endpoint - should not exist
        assertNull(directive.endpoint)

        // check payload
        assertNotNull(directive.payload)
        assertNotNull(directive.payload.scope)
        assertEquals("BearerToken", directive.payload.scope!!.type)
        assertEquals("access-token-from-skill", directive.payload.scope!!.token)
    }

    /*
     * Helper methods
     */

    private fun readJson(fileName: String) : String {

        val jsonResource = AlexaHandlerTest::class.java.getResource(fileName)

        assertNotNull(jsonResource, "Resource $fileName not found.")

        val json = jsonResource.readText()

        assertNotNull(json, "readText() of $fileName failed.")

        return json
    }
}