package com.dennymathew.frameflow.testutil

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dennymathew.frameflow.data.local.ImageDatabase

fun inMemoryDatabase(): ImageDatabase = Room.inMemoryDatabaseBuilder(
    ApplicationProvider.getApplicationContext(),
    ImageDatabase::class.java
).allowMainThreadQueries()
    // Run queries inline so tests don't race Room's background executors.
    .setQueryExecutor { it.run() }
    .setTransactionExecutor { it.run() }
    .build()
