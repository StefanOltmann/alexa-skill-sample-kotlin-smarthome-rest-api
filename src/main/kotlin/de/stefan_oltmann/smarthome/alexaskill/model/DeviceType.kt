/*
 * Stefans Smart Home Server
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

enum class DeviceType {

    /**
     * A simple light that can be turned off and on.
     */
    LIGHT_SWITCH,

    /**
     * A dimmer is a light that can be set to a percentage value.
     * In addition it can be turned off and on.
     */
    DIMMER,

    /**
     * A exterior blind that can be set to a percentage value.
     * Also "on" and "off" is supported for going all the way up or down.
     */
    ROLLER_SHUTTER;

    val deviceCategory: DeviceCategory
        get() = when (this) {
            LIGHT_SWITCH -> DeviceCategory.LIGHT
            DIMMER -> DeviceCategory.LIGHT
            ROLLER_SHUTTER -> DeviceCategory.EXTERIOR_BLIND
        }

    val deviceCapabilities: List<DeviceCapability>
        get() = when (this) {
            LIGHT_SWITCH -> listOf(DeviceCapability.POWER_STATE)
            DIMMER -> listOf(DeviceCapability.POWER_STATE, DeviceCapability.PERCENTAGE)
            ROLLER_SHUTTER -> listOf(DeviceCapability.POWER_STATE, DeviceCapability.PERCENTAGE)
        }
}