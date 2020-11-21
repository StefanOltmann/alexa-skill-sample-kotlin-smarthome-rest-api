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

import com.amazonaws.services.lambda.runtime.Context
import com.google.gson.GsonBuilder
import de.stefan_oltmann.smarthome.alexaskill.alexamodel.*
import de.stefan_oltmann.smarthome.alexaskill.model.DeviceCapability
import de.stefan_oltmann.smarthome.alexaskill.model.DevicePowerState
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * This must be a class (and cannot be an object) because the AWS Lambda
 * will create an instance of it before calling the handleRequest() method.
 */
class AlexaHandler {

    companion object {

        /*
         * Likely your name if these are your devices.
         * Must not be empty.
         */
        const val MANUFACTURER_NAME = "Smart Home"

        /*
         * A placeholder description for the devices.
         * Must not be empty.
         */
        const val DEVICE_DESCRIPTION = "-"

        const val UNIT_TEST_MESSAGE_ID = "MESSAGE_ID"
        const val UNIT_TEST_TIMESTAMP = 1577885820000
    }

    /**
     * GSON instance that creates pretty JSON.
     *
     * This is good for unit tests and log statements as well.
     */
    private val gson by lazy {

        val builder = GsonBuilder()
        builder.setPrettyPrinting()
        builder.create()
    }

    /**
     * Modifies behaviour to help us with unit tests.
     *
     * Should never be 'true' in production.
     */
    var unitTesting = false

    /**
     * This method is called by the AWS Lambda.
     *
     * This method reads and outputs JSON data as bytes. To better work on this we
     * only handle his conversion here and delegate JSON strings to handleRequestJson().
     */
    fun handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context) {

        try {

            /* For e.g. "https://myserver.com:50000/" (without quotes) */
            val apiUrl = System.getenv("API_URL")

            val retrofit = Retrofit.Builder()
                .baseUrl(apiUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val restApi = retrofit.create(RestApi::class.java)

            val requestJson = getRequestString(inputStream)

            println("Request: $requestJson")

            val responseJson = handleRequestJson(requestJson, restApi)

            println("Response: $responseJson")

            outputStream.write(responseJson.toByteArray(StandardCharsets.UTF_8))

        } catch (e: Exception) {
            println("ERROR: " + e.message)
            e.printStackTrace()
        }
    }

    /**
     * This method takes in the request as JSON string and returns the response as JSON string.
     *
     * It delegates to handleRequestObject() which then works on the object level.
     */
    fun handleRequestJson(requestJson: String, restApi: RestApi): String {

        val alexaRequest = gson.fromJson(requestJson, AlexaRequest::class.java)

        val alexaResponse = handleRequestObject(alexaRequest, restApi)

        return gson.toJson(alexaResponse)
    }

    /**
     * Finally on this level we only work with real objects and keep the JSON stuff out of the logic.
     */
    private fun handleRequestObject(alexaRequest: AlexaRequest, restApi: RestApi) : AlexaResponse {

        /* Every call to an Alexa Skill is a "Directive" like orders: "Do this, do that." */
        val directive = alexaRequest.directive

        /*
         * We can us the Namespace to decide upon the action.
         */
        when (directive.header.namespace) {

            /**
             * This is the first call to this skill when the users wants to activate it.
             *
             * In this simple case it's always accepted.
             */
            Header.NAMESPACE_AUTHORIZATION -> return createAuthorizationGrantedResponse()

            /**
             * This is the second call to this skill after successful authorization:
             * Alexa needs to find all our devices for configuration.
             *
             * This is called only on user interaction. Alexa will cache the discovery result.
             */
            Header.NAMESPACE_DISCOVERY -> return createDeviceDiscoveryResponse(restApi)

            /**
             * This call is made when you tell Alexa to turn something on or off.
             *
             * "Alexa, kitchen light on!" is considered a "Power Controller Directive".
             */
            Header.NAMESPACE_POWER_CONTROLLER ->
                return executePowerControllerDirectiveAndCreateResponse(directive, restApi)

            /**
             * This call is made when you tell Alexa to set something to a percentage value.
             *
             * "Alexa, kitchen light to 60 percent!" is considered a "Percentage Controller Directive".
             */
            Header.NAMESPACE_PERCENTAGE_CONTROLLER ->
                return executePercentageControllerDirectiveAndCreateResponse(directive, restApi)

            /**
             * In case this skill receives a directive it cannot handle we return an error.
             */
            else -> return createErrorResponse()
        }
    }

    /**
     * Creates a response that grants authorization
     */
    private fun createAuthorizationGrantedResponse() : AlexaResponse {

        return AlexaResponse(
            event = Event(
                header = Header(
                    namespace = Header.NAMESPACE_AUTHORIZATION,
                    name = "AcceptGrant.Response",
                    messageId = createMessageId()
                ),
                payload = Payload()
            )
        )
    }

    /**
     * Calls the backend API for the devices and creates a Discovery response.
     */
    private fun createDeviceDiscoveryResponse(restApi: RestApi) : AlexaResponse {

        /* Fetch all the devices from the REST API */
        val devicesResponse = restApi.findAllDevices().execute()

        /* Return ASAP if a problem occurred. */
        if (!devicesResponse.isSuccessful)
            return createErrorResponse()

        val devices = devicesResponse.body()

        /**
         * It's very helpful for debugging to see which devices are actually returned by the backend API.
         */
        println("Devices from API: $devices")

        /*
         * Creation of capabilities our devices need.
         *
         * Of course there are a lot more, but we go with the most common used: Switches & Dimmers.
         *
         * We define them here as we don't want to create more objects in memory
         * as needed and these can be reused for all devices.
         */

        val alexaCapability = Capability(
            interfaceName = Header.NAMESPACE_ALEXA)

        val powerControllerCapability = Capability.create(
            interfaceName = Header.NAMESPACE_POWER_CONTROLLER,
            supportedName = "powerState")

        val percentageControllerCapability = Capability.create(
            interfaceName = Header.NAMESPACE_PERCENTAGE_CONTROLLER,
            supportedName = "percentage")

        /*
         * An "endpoint" is a device. For example the light you want to turn off and on is a "endpoint".
         */
        val endpoints = mutableListOf<DiscoveryEndpoint>()

        if (devices != null) {

            for (device in devices) {

                /*
             * Devices can have many capabilities and you can mix them like you need it.
             */
                val capabilities = mutableListOf<Capability>()

                for (capability in device.capabilities) {

                    /* We always need this base capability. */
                    capabilities.add(alexaCapability)

                    /* Add other capabilities based on supported functions. */
                    when (capability) {
                        DeviceCapability.POWER_STATE -> capabilities.add(powerControllerCapability)
                        DeviceCapability.PERCENTAGE -> capabilities.add(percentageControllerCapability)
                    }
                }

                val discoveryEndpoint = DiscoveryEndpoint(
                        endpointId = device.id,
                        friendlyName = device.name,
                        description = DEVICE_DESCRIPTION,
                        manufacturerName = MANUFACTURER_NAME,
                        capabilities = capabilities,
                        displayCategories = listOf(device.category.name))

                endpoints.add(discoveryEndpoint)
            }
        }

        return AlexaResponse(
            event = Event(
                header = Header(
                    namespace = Header.NAMESPACE_DISCOVERY,
                    name = "Discover.Response",
                    messageId = createMessageId()
                ),
                payload = Payload(
                    endpoints = endpoints
                )
            )
        )
    }

    private fun executePowerControllerDirectiveAndCreateResponse(directive: Directive, restApi: RestApi) : AlexaResponse {

        val endpointId = directive.endpoint.endpointId

        val powerState =
            if (directive.header.name == "TurnOn")
                DevicePowerState.ON else DevicePowerState.OFF

        val callWasSuccessful = executePowerStateCall(endpointId, powerState, restApi)

        if (!callWasSuccessful)
            return createErrorResponse()

        return AlexaResponse(
            event = Event(
                header = Header(
                    correlationToken = directive.header.correlationToken,
                    messageId = createMessageId()
                ),
                endpoint = Endpoint(
                    endpointId = directive.endpoint.endpointId,
                    scope = Scope(token = directive.endpoint.scope.token)
                )
            ),
            context = Context(
                properties = listOf(
                    ContextProperties(
                        namespace = Header.NAMESPACE_POWER_CONTROLLER,
                        name = "powerState",
                        value = powerState.name,
                        timeOfSample = createCurrentTimeString())
                )
            )
        )
    }

    private fun executePercentageControllerDirectiveAndCreateResponse(directive: Directive, restApi: RestApi) : AlexaResponse {

        val endpointId = directive.endpoint.endpointId

        val percentage = directive.payload.percentage!!

        val callWasSuccessful = executePercentageCall(endpointId, percentage, restApi)

        if (!callWasSuccessful)
            return createErrorResponse()

        return AlexaResponse(
            event = Event(
                header = Header(
                    correlationToken = directive.header.correlationToken,
                    messageId = createMessageId()
                ),
                endpoint = Endpoint(
                    endpointId = directive.endpoint.endpointId,
                    scope = Scope(token = directive.endpoint.scope.token)
                )
            ),
            context = Context(
                properties = listOf(
                    ContextProperties(
                        namespace = Header.NAMESPACE_PERCENTAGE_CONTROLLER,
                        name = "percentage",
                        value = percentage.toString(),
                        timeOfSample = createCurrentTimeString())
                )
            )
        )
    }

    /*
     * Methods to communicate with backend API
     */

    /**
     * Calls the backend API to set the power state of specified endpoint.
     */
    private fun executePowerStateCall(
        endpointId: String, devicePowerState: DevicePowerState, restApi: RestApi): Boolean {

        val call = restApi.setPowerState(endpointId, devicePowerState)

        if (!unitTesting)
            println("Execute call: " + call.request().url())

        val response: Response<*> = call.execute()

        if (!unitTesting)
            println("Call result: " + response.code() + " - " + response.message())

        return response.isSuccessful
    }

    /**
     * Calls the backend API to set the percentage of specified endpoint.
     */
    private fun executePercentageCall(
        endpointId: String, percentage: Int, restApi: RestApi): Boolean {

        val call = restApi.setPercentage(endpointId, percentage)

        if (!unitTesting)
            println("Execute call: " + call.request().url())

        val response: Response<*> = call.execute()

        if (!unitTesting)
            println("Call result: " + response.code() + " - " + response.message())

        return response.isSuccessful
    }

    /*
     * Helper methods
     */

    private fun getRequestString(inputStream: InputStream): String {

        val scanner = Scanner(inputStream).useDelimiter("\\A")

        return if (scanner.hasNext()) scanner.next() else ""
    }

    private fun createErrorResponse(): AlexaResponse {

        return AlexaResponse(
            event = Event(
                header = Header(
                    name = "ErrorResponse",
                    messageId = createMessageId()
                ),
                payload = Payload(
                    type = "INVALID_DIRECTIVE",
                    message = "Request is invalid."
                )
            )
        )
    }

    /**
     * Every message needs a unique UUID as identifier.
     *
     * This method creates that. Expect we are in unitTesting mode.
     */
    private fun createMessageId() = if (unitTesting) UNIT_TEST_MESSAGE_ID else UUID.randomUUID().toString()

    private fun createCurrentTimeString() : String {

        val timestamp = if (unitTesting) UNIT_TEST_TIMESTAMP else Date().time

        val simpleDataFormat = SimpleDateFormat(ContextProperties.DATE_FORMAT_PATTERN)
        simpleDataFormat.timeZone = TimeZone.getTimeZone(ContextProperties.DATE_FORMAT_TIMEZONE)

        return simpleDataFormat.format(timestamp)
    }
}