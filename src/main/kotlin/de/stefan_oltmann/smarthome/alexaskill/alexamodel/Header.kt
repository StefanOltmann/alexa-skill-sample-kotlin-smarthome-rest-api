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

data class Header (
        val namespace : String = NAMESPACE_ALEXA,
        val name : String = NAME_RESPONSE,
        val payloadVersion : String = PAYLOAD_VERSION,
        val messageId : String,
        val correlationToken : String? = null) {

    companion object {

        const val PAYLOAD_VERSION = "3"

        const val NAME_RESPONSE = "Response"

        const val NAMESPACE_ALEXA = "Alexa"

        const val NAMESPACE_AUTHORIZATION = "Alexa.Authorization"
        const val NAMESPACE_DISCOVERY = "Alexa.Discovery"
        const val NAMESPACE_POWER_CONTROLLER = "Alexa.PowerController"
        const val NAMESPACE_PERCENTAGE_CONTROLLER = "Alexa.PercentageController"
    }
}