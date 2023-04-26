package no.nav.amt.person.service.kafka.config

import java.util.*

interface KafkaProperties {

    fun consumer(): Properties

    fun producer(): Properties

}
