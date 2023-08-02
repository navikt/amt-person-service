package no.nav.amt.person.service.synchronization

import java.util.*

data class SynchronizationUpsert (
	val dataProvider: DataProvider,
	val tableName: String,
	val rowId: UUID,
)
