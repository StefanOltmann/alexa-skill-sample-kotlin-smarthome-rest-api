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

package de.stefan_oltmann.smarthome.alexaskill.model

/*
 * A device like a light switch, a dimmer or a roller shutter.
 *
 * Alexa calls these "endpoints".
 */
data class Device(val id: String,
                  val name: String,
                  val type: DeviceType) {

    val category: DeviceCategory
        get() = type.deviceCategory

    val capabilities: List<DeviceCapability>
        get() = type.deviceCapabilities
}