package service

import db.Payments
import db.PaySupportRequests
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object PayService {
    data class PaymentInfo(
        val providerChargeId: String,
        val telegramChargeId: String,
        val amount: Int = 0,
        val currency: String = "XTR"
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

    data class SupportRequest(val id: Long, val userId: Long)

    fun findSupportRequest(id: Long): SupportRequest? = transaction {
        PaySupportRequests.select { PaySupportRequests.id eq id }
            .map { SupportRequest(it[PaySupportRequests.id].value, it[PaySupportRequests.userId].value) }
            .singleOrNull()
    }
}

