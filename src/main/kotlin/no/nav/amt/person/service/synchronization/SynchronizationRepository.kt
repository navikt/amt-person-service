package no.nav.amt.person.service.synchronization
import no.nav.amt.person.service.utils.getUUID
import no.nav.amt.person.service.utils.sqlParameters
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SynchronizationRepository(
	private val template: NamedParameterJdbcTemplate
) {

	private val rowMapper = RowMapper { rs, _ ->
		SynchronizationDbo(
			id = rs.getUUID("id"),
			dataProvider = DataProvider.valueOf(rs.getString("data_provider")),
			tableName = rs.getString("table_name"),
			rowId = rs.getUUID("row_id"),
			lastSync = rs.getTimestamp("last_sync").toLocalDateTime(),
			createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
		)
	}

	fun	get(externalRowId: UUID): SynchronizationDbo? {
		val sql = """
			select *
			from synchronization
			where row_id = :external_row_id
		""".trimIndent()
		val parameters = sqlParameters("external_row_id" to externalRowId)

		return template.query(sql, parameters, rowMapper).firstOrNull()
	}

	fun upsert(synchronizationUpsert: SynchronizationUpsert) {
		val sql = """
			insert into synchronization(
				id,
				data_provider,
				table_name,
				row_id
			) values (
				:id,
				:data_provider,
				:table_name,
				:row_id
			) on conflict(row_id) do update set last_sync = current_timestamp
		""".trimIndent()

		val parameters = sqlParameters(
			"id" to UUID.randomUUID(),
			"data_provider" to synchronizationUpsert.dataProvider.name,
			"table_name" to synchronizationUpsert.tableName,
			"row_id" to synchronizationUpsert.rowId,
		)

		template.update(sql, parameters)
	}

	fun delete(rowId: UUID) {
		val sql = """
			delete from synchronization
			where row_id = :row_id
		""".trimIndent()

		val parameters = sqlParameters("row_id" to rowId)

		template.update(sql, parameters)
	}

}

