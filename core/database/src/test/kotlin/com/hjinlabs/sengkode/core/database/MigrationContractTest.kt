package com.hjinlabs.sengkode.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migration scaffold (exportSchema = true): validates that the
 * committed schema JSON matches what the database ACTUALLY creates.
 * When v2 arrives, this is where the 1->2 migration test lives;
 * v1 establishes the baseline the future migration must satisfy.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationContractTest {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun `schema v1 creates and validates against the committed json`() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, 1, true).close()
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
