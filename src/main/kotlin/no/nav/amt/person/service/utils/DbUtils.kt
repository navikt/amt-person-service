package no.nav.amt.person.service.utils

import org.postgresql.util.PGobject
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.util.UUID

fun <V> sqlParameters(vararg pairs: Pair<String, V>): MapSqlParameterSource = MapSqlParameterSource().addValues(pairs.toMap())

fun ResultSet.getUUID(columnLabel: String): UUID = UUID.fromString(this.getString(columnLabel))

fun ResultSet.getNullableUUID(columnLabel: String): UUID? =
	this
		.getString(columnLabel)
		?.let { UUID.fromString(it) }

fun toPGObject(
	value: Any?,
	objectMapper: ObjectMapper,
) = PGobject().also {
	it.type = "json"
	it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
}
