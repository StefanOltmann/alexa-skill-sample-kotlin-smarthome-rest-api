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

/**
 * This represents what can be done with the device.
 *
 * This is Alexa specific and determined from the {@link DeviceType}.
 */
enum class DeviceCapability {

    /** Can be turned on and off */
    POWER_STATE,

    /** Can take a percent value (e.g. Dimmers or Roller shutters) */
    PERCENTAGE;

}