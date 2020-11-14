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

package de.stefan_oltmann.smarthome.alexaskill.alexamodel

import com.google.gson.annotations.SerializedName

/**
 * A capability is something a device can do like be turned off and on (power control)
 * as well as set to a specific value (percentage control) which applies to dimmers and rollershutters.
 */
data class Capability (
        val type : String = "AlexaInterface",
        @SerializedName("interface")
        val interfaceName : String,
        val version : String = Header.PAYLOAD_VERSION,
        val properties: CapabilityProperties? = null) {

    companion object {

        fun create(
                interfaceName: String,
                supportedName : String) : Capability {

            return Capability(
                    interfaceName = interfaceName,
                    properties = CapabilityProperties(
                            supported = listOf(Supported(name = supportedName))
                    )
            )
        }
    }
}