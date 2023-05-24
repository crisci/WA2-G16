package it.polito.wa2.ticketing.employee

import it.polito.wa2.ticketing.utils.EmployeeRole
import java.util.UUID

data class EmployeeDTO(
    val id: UUID?,
    val first_name: String,
    val last_name: String,
    val email: String,
    val type: EmployeeRole,
)

fun Employee.toEmployeeDTO(): EmployeeDTO {
    return EmployeeDTO(getId(),first_name,last_name,email,type)
}