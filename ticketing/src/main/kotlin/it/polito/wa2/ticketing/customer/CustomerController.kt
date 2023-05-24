package it.polito.wa2.ticketing.customer

import it.polito.wa2.ticketing.message.MessageDTO
import it.polito.wa2.ticketing.security.JwtAuthConverter
import it.polito.wa2.ticketing.ticket.TicketDTO
import it.polito.wa2.ticketing.utils.EmailValidationUtil
import jakarta.transaction.Transactional
import org.springframework.http.*
import org.springframework.security.access.annotation.Secured
import org.springframework.security.access.prepost.PreFilter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.security.Principal
import java.util.UUID


@RestController
class CustomerController(val customerService: CustomerService) {

    private val emailValidator = EmailValidationUtil()

    @PostMapping("/API/login")
    @ResponseStatus(HttpStatus.OK)
    fun login(@RequestBody credentials: Map<String, String>): String {
        val restTemplate = RestTemplate()

        val url = "http://localhost:8080/realms/ticketing/protocol/openid-connect/token"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val requestBody: MultiValueMap<String, String> = LinkedMultiValueMap()
        requestBody.add("grant_type", "password")
        requestBody.add("client_id", "authN")
        requestBody.add("username", credentials["username"])
        requestBody.add("password", credentials["password"])

        val requestEntity = HttpEntity(requestBody, headers)
        val responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String::class.java)

        return responseEntity.body ?: "Error occurred during login."
    }

    @GetMapping("/API/customers/email={email}")
    @ResponseStatus(HttpStatus.OK)
    fun getCustomerByEmail(@PathVariable email: String) : CustomerDTO? {
        if (emailValidator.checkEmail(email)) {
            if(customerService.getCustomerByEmail(email.lowercase()) == null)
                throw CustomerNotFoundException("Customer not fount with the following email '${email}'")
            else
                return customerService.getCustomerByEmail(email.lowercase())
        } else {
            throw InvalidEmailFormatException("Invalid email format")
        }
    }

    @GetMapping("/API/customers")
    @ResponseStatus(HttpStatus.OK)
    fun getCustomers() : List<CustomerDTO> {
        return customerService.getCustomers()
    }

    @PostMapping("/API/customers")
    @ResponseStatus(HttpStatus.CREATED)
    fun postCustomer(@RequestBody customerDTO: CustomerDTO) {
        if(customerDTO.email.isNotBlank() && customerDTO.first_name.isNotBlank() && customerDTO.last_name.isNotBlank()) {
            if (emailValidator.checkEmail(customerDTO.email)) {
                    customerService.insertCustomer(customerDTO)
                } else {
                throw InvalidEmailFormatException("Invalid email format")
            }} else {
            throw BlankFieldsException("Fields must not be blank")
        }
    }

    @PostMapping("/API/customers/tickets/{idTicket}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    fun addMessage(@PathVariable idTicket: Long, @RequestBody message: MessageDTO) {
        customerService.addMessage(idTicket, message)
    }

    @GetMapping("/API/customers/tickets/{idTicket}/messages")
    @ResponseStatus(HttpStatus.OK)
    @Secured("ROLE_Customer")
    fun getTicketsWithMessagesByCustomerId(@PathVariable idTicket: Long): List<MessageDTO>? {
        val userDetails = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        return customerService.getTicketsWithMessagesByCustomerId(UUID.fromString(userDetails.tokenAttributes["sub"].toString()), idTicket)
    }

    @GetMapping("/API/customers/tickets")
    @ResponseStatus(HttpStatus.OK)
    @Secured("ROLE_Customer")
    fun getTicketsByCustomerId(): List<TicketDTO>? {
        val userDetails = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        return customerService.getTicketsByCustomerId(UUID.fromString(userDetails.tokenAttributes["sub"].toString()))
    }

    @PostMapping("/API/customers/tickets")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Secured("ROLE_Customer")
    fun addTicket(@RequestBody ticket: TicketDTO) {
        val userDetails = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        customerService.addTicket(ticket, UUID.fromString(userDetails.tokenAttributes["sub"].toString()))
    }





}
