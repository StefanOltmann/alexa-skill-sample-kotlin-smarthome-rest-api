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

import de.stefan_oltmann.smarthome.alexaskill.model.Device
import de.stefan_oltmann.smarthome.alexaskill.model.DevicePowerState
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface RestApi {

    /**
     * Returns all devices for device discovery.
     */
    @GET("/alexa/devices")
    fun findAllDevices(): Call<List<Device>>

    /**
     * Turns a device (for e.g. a light) on and off.
     */
    @GET("/alexa/switch/{endpointId}/to/{powerState}")
    fun setPowerState(@Path("endpointId") endpointId: String,
                      @Path("powerState") powerState: DevicePowerState): Call<Void>

    /**
     * Sets a percentage value to a device. For example a dimmer or a roller shutter.
     */
    @GET("/alexa/set/{endpointId}/to/{percentage}")
    fun setPercentage(@Path("endpointId") endpointId: String,
                      @Path("percentage") percentage: Int): Call<Void>
}