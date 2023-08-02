package no.nav.amt.person.service.synchronization

import java.time.LocalDateTime
import java.util.UUID

data class SynchronizationDbo(
	val id: UUID,
	val dataProvider: DataProvider,
	val tableName: String,
	val rowId: UUID,
	val lastSync: LocalDateTime,
	val createdAt: LocalDateTime,
)
