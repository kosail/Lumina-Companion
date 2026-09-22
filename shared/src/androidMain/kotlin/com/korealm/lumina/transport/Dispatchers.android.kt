package com.korealm.lumina.transport

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Android actual for [ioDispatcher]: `Dispatchers.IO`, off the main thread (FE-INV-052, CHG-FE-0027). */
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
