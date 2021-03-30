/*
 * Copyright (c) 2020.  The Redis Open Source Project
 * Author: Alexey Rozhkov https://github.com/ar2code
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ar2code.redis.core

/**
 * Interface that describes how to store and restore state
 * When state changed service find a StateStore item for new state inside [stateStores].
 * If [StateStore] item found service invoke [StateStore.store] method to store current state.
 *
 * So you should create a list of StateStore items to provide a mechanism of storing for each service state.
 * But you can set only single StateStore that handles all service states. For it just create [StateStore] with [StateStore.expectState] is null.
 *
 * After service was created it searches a [StateRestore] inside [stateRestores] and invoke [StateRestore.restoreState] to give you a possibility restore state or dispatch an intent to service itself.
 *
 * @property stateStoreKeyName - is used for storing state name in [SavedStateStore]
 */
interface SavedStateHandler {

    val stateStores: List<StateStore>

    val stateRestores: List<StateRestore>

    val stateStoreKeyName: String
}