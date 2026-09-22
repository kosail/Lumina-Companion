package com.korealm.lumina.transport

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** JVM actual for [ioDispatcher]: `Dispatchers.IO`, the elastic pool for socket/disk I/O (FE-INV-052). */
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
