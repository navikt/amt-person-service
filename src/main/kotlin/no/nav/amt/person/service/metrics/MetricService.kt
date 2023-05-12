package no.nav.amt.person.service.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class MetricService(
	private val repository: MetricRepository,
	val meterRegistry: MeterRegistry,
) {
	private val antallPersoner = gauge("amt_person_antall_personer")
	private val antallNavBruker = gauge("amt_person_antall_nav_brukere")
	private val antallNavAnsatte = gauge("amt_person_antall_nav_ansatte")
	private val antallNavEnheter = gauge("amt_person_antall_nav_enheter")
	private val antallArrangorAnsatte = gauge("amt_person_antall_arrangor_ansatte")

	fun oppdaterMetrikker() {
		val counts = repository.getCounts()

		antallPersoner.set(counts.antallPersoner)
		antallNavBruker.set(counts.antallNavBrukere)
		antallNavAnsatte.set(counts.antallNavAnsatte)
		antallNavEnheter.set(counts.antallNavEnheter)
		antallArrangorAnsatte.set(counts.antallArrangorAnsatte)

	}

	private fun gauge(name: String): AtomicInteger {
		return meterRegistry.gauge(name, AtomicInteger(0))!!
	}

}

