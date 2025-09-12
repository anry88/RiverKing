package service

import db.Payments
import db.PaySupportRequests
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object PayService {
    data class PaymentInfo(
        val providerChargeId: String? = null,
        val telegramChargeId: String,
        val amount: Int = 0,
        val currency: String = "XTR"
    )

    data class UserPayment(
        val id: Long,
        val packageId: String,
        val amount: Int,
        val currency: String,
    )

    fun recordPayment(userId: Long, packageId: String, info: PaymentInfo) = transaction {
        Payments.insert {
            it[Payments.userId] = userId
            it[Payments.packageId] = packageId
            it[Payments.providerChargeId] = info.providerChargeId
            it[Payments.telegramChargeId] = info.telegramChargeId
            it[Payments.amount] = info.amount
            it[Payments.currency] = info.currency
            it[Payments.createdAt] = Instant.now()
        }
    }

    fun listPayments(userId: Long): List<UserPayment> = transaction {
        Payments.select { (Payments.userId eq userId) and (Payments.refunded eq false) }
            .orderBy(Payments.createdAt, SortOrder.DESC)
            .map {
                UserPayment(
                    it[Payments.id].value,
                    it[Payments.packageId],
                    it[Payments.amount],
                    it[Payments.currency]
                )
            }
    }

    fun createSupportRequest(userId: Long, paymentId: Long?, reason: String): Long = transaction {
        PaySupportRequests.insertAndGetId {
            it[PaySupportRequests.userId] = userId
            it[PaySupportRequests.paymentId] = paymentId
            it[PaySupportRequests.reason] = reason
            it[PaySupportRequests.status] = "pending"
            it[PaySupportRequests.createdAt] = Instant.now()
        }.value
    }

    fun updateSupportRequest(id: Long, status: String, adminMessage: String?) = transaction {
        PaySupportRequests.update({ PaySupportRequests.id eq id }) {
            it[PaySupportRequests.status] = status
            it[PaySupportRequests.adminMessage] = adminMessage
        }
    }

    data class SupportRequest(val id: Long, val userId: Long, val paymentId: Long?)

    fun findSupportRequest(id: Long): SupportRequest? = transaction {
        PaySupportRequests.select { PaySupportRequests.id eq id }
            .map {
                SupportRequest(
                    it[PaySupportRequests.id].value,
                    it[PaySupportRequests.userId].value,
                    it[PaySupportRequests.paymentId]?.value
                )
            }
            .singleOrNull()
    }

    data class Payment(val id: Long, val telegramChargeId: String)

    fun findPayment(id: Long): Payment? = transaction {
        Payments.select { (Payments.id eq id) and (Payments.refunded eq false) }
            .map { Payment(it[Payments.id].value, it[Payments.telegramChargeId]) }
            .singleOrNull()
    }

    fun latestPayment(userId: Long): Payment? = transaction {
        Payments.select { (Payments.userId eq userId) and (Payments.refunded eq false) }
            .orderBy(Payments.createdAt, SortOrder.DESC)
            .limit(1)
            .map { Payment(it[Payments.id].value, it[Payments.telegramChargeId]) }
            .singleOrNull()
    }

    fun markPaymentRefunded(id: Long) = transaction {
        Payments.update({ Payments.id eq id }) { it[refunded] = true }
    }
}

