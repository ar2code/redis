/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.demo.impl

import kotlinx.coroutines.CoroutineScope
import ru.ar2code.defaults.DefaultActorService

abstract class AbstractDemoService(scope: CoroutineScope) : DefaultActorService<String>(scope)