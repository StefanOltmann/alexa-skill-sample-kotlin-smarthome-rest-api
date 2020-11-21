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

data class ContextProperties (
        val namespace : String,
        val name : String,
        val timeOfSample : String,
        val uncertaintyInMilliseconds : Int = UNCERTAINTY_IN_MS,
        val value : String) {

    companion object {

        const val UNCERTAINTY_IN_MS = 200

        const val DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.sss'Z'"
        const val DATE_FORMAT_TIMEZONE = "UTC"
    }
}