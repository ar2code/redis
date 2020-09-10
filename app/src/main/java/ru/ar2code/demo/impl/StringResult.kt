/*
 * Copyright (c) 2020. Created by Alexey Rozhkov.
 */

package ru.ar2code.demo.impl

import ru.ar2code.android.architecture.core.models.ServiceResult

class StringResult(payload : String?) : ServiceResult<String>(payload) {
}