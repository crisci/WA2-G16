package it.polito.wa2.ticketing

import it.polito.wa2.ticketing.attachment.AttachmentRepository
import it.polito.wa2.ticketing.customer.Customer
import it.polito.wa2.ticketing.customer.CustomerNotFoundException
import it.polito.wa2.ticketing.customer.CustomerRepository
import it.polito.wa2.ticketing.employee.Employee
import it.polito.wa2.ticketing.employee.EmployeeRepository
import it.polito.wa2.ticketing.employee.toEmployeeDTO
import it.polito.wa2.ticketing.history.History
import it.polito.wa2.ticketing.history.HistoryNotFoundException
import it.polito.wa2.ticketing.history.HistoryRepository
import it.polito.wa2.ticketing.history.OperationNotPermittedException
import it.polito.wa2.ticketing.message.Message
import it.polito.wa2.ticketing.message.MessageRepository
import it.polito.wa2.ticketing.message.toDTO
import it.polito.wa2.ticketing.product.Product
import it.polito.wa2.ticketing.product.ProductNotFoundException
import it.polito.wa2.ticketing.product.ProductRepository
import it.polito.wa2.ticketing.ticket.*
import it.polito.wa2.ticketing.utils.EmployeeRole
import it.polito.wa2.ticketing.utils.PriorityLevel
import it.polito.wa2.ticketing.utils.SenderType
import it.polito.wa2.ticketing.utils.TicketStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime

@Testcontainers
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
class TicketingApplicationTests {
	companion object {
		@Container
		val postgres = PostgreSQLContainer("postgres:latest")
		@JvmStatic
		@DynamicPropertySource
		fun properties(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url", postgres::getJdbcUrl)
			registry.add("spring.datasource.username", postgres::getUsername)
			registry.add("spring.datasource.password", postgres::getPassword)
			registry.add("spring.jpa.hibernate.ddl-auto") {"create-drop"}
		}
	}
	@LocalServerPort
	protected var port: Int = 8081
	@Autowired
	lateinit var restTemplate: TestRestTemplate
	@Autowired
	lateinit var customerRepository: CustomerRepository
	@Autowired
	lateinit var productRepository: ProductRepository
	@Autowired
	lateinit var ticketRepository: TicketRepository
	@Autowired
	lateinit var historyRepository: HistoryRepository
	@Autowired
	lateinit var employeeRepository: EmployeeRepository
	@Autowired
	lateinit var messageRepository: MessageRepository
	@Autowired
	lateinit var attachmentRepository: AttachmentRepository


	var customer: Customer = Customer()
	var product: Product = Product()
	var ticket: Ticket = Ticket()
	var history1: History = History()
	var history2: History = History()
	var expert: Employee = Employee()
	var admin: Employee = Employee()
	var message1: Message = Message()
	var message2: Message = Message()
	//var attachment: Attachment = Attachment()

	//Add before and post to initialize and flush data

	@Nested
	@DisplayName("GET /API/tickets/{idCustomer}")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	inner class GetTicketByCustomerId {

		@Test
		fun customerNotFound() {
			val customerId = -1
			val response = restTemplate.getForEntity("/API/tickets/$customerId", String::class.java)
			assert(response.statusCode.is4xxClientError)
		}

		@Test
		fun emptyListOfTicket() {
			val customer: Customer = Customer().apply {
				first_name = "Luigi"
				last_name = "Crisci"
				dob = LocalDate.of(1998, 9, 13)
				email = "xyz@gmail.com"
				address = "Via Torino"
			}
			customerRepository.save(customer)


			val product = Product().apply {
				ean = "4935531465706"
				name = "JMT X-ring 530x2 Gold 104 Open Chain With Rivet Link for Kawasaki KH 400 a 1976"
				brand = "JMT"
			}
			productRepository.save(product)

			productRepository.save(product)
			customerRepository.save(customer)

			val response = restTemplate.getForEntity("/API/tickets/${customer.getId()}", String::class.java)
			assert(response.body!!.contains("[]"))

			customerRepository.flush()
			productRepository.flush()
			ticketRepository.flush()
			historyRepository.flush()
			employeeRepository.flush()
			messageRepository.flush()
		}

		@Test
		fun listOfTicket() {
			val customer: Customer = Customer().apply {
				first_name = "Luigi"
				last_name = "Crisci"
				dob = LocalDate.of(1998, 9, 13)
				email = "xyzw@gmail.com"
				address = "Via Torino"
			}
			customerRepository.save(customer)


			val product = Product().apply {
				ean = "4935531465706"
				name = "JMT X-ring 530x2 Gold 104 Open Chain With Rivet Link for Kawasaki KH 400 a 1976"
				brand = "JMT"
			}
			productRepository.save(product)

			val ticket = Ticket().apply {
				title = "Problema con la catena"
				description = "La catena si è rotta"
				priority = PriorityLevel.HIGH
			}
			ticketRepository.save(ticket)

			customer.addTicket(ticket)
			product.addTicket(ticket)

			ticketRepository.save(ticket)
			productRepository.save(product)
			customerRepository.save(customer)

			val response = restTemplate.getForEntity("/API/tickets/${customer.getId()}", String::class.java)
			assert(response.body!!.length > 0)

			customerRepository.flush()
			productRepository.flush()
			ticketRepository.flush()
			historyRepository.flush()
			employeeRepository.flush()
			messageRepository.flush()
		}
	}

	@Nested
	@DisplayName("POST /API/tickets/{idCustomer}")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	inner class AddTicket {

		val ticketWrongProductId = Ticket().apply {
			title = "Problema con la catena"
			description = "La catena si è rotta"
			priority = PriorityLevel.HIGH
			this.product = Product().apply {
				ean = "-1"
				name = "JMT X-ring 530x2 Gold 104 Open Chain With Rivet Link for Kawasaki KH 400 a 1976"
				brand = "JMT"
			}
		}

		@Test
		fun customerNotFound() {
			val customerId = -1
			val response = restTemplate.postForEntity("/API/tickets/$customerId", ticketWrongProductId.toTicketDTO(), CustomerNotFoundException::class.java)
			assert(response.body is CustomerNotFoundException)
		}

		@Test
		fun productNotFound() {
			val customer: Customer = Customer().apply {
				first_name = "Luigi"
				last_name = "Crisci"
				dob = LocalDate.of(1998, 9, 13)
				email = "AT1@test.com"
				address = "Via Torino"
			}
			customerRepository.save(customer)

			val response = restTemplate.postForEntity("/API/tickets/${customer.getId()}", ticketWrongProductId.toTicketDTO(), ProductNotFoundException::class.java)
			assert(response.body is ProductNotFoundException)

			customerRepository.deleteAll()
			productRepository.deleteAll()
			ticketRepository.deleteAll()
		}

		@Test
		fun ticketAddedCorrectly() {
			val customer: Customer = Customer().apply {
				first_name = "Luigi"
				last_name = "Crisci"
				dob = LocalDate.of(1998, 9, 13)
				email = "AT2@test.com"
				address = "Via Torino"
			}
			customerRepository.save(customer)

			val product: Product = Product().apply {
				ean = "AT2"
				name = "JMT X-ring 530x2 Gold 104 Open Chain With Rivet Link for Kawasaki KH 400 a 1976"
				brand = "JMT"
			}
			productRepository.save(product)


			val ticket: Ticket = Ticket().apply {
				title = "Title"
				description = "Description"
				priority = PriorityLevel.NOT_ASSIGNED
				this.product = product
			}

			val response = restTemplate.postForEntity("/API/tickets/${customer.getId()}", ticket.toTicketDTO(), String::class.java)
			println(response.statusCode)
			assert(response.statusCode == HttpStatus.ACCEPTED)

			customerRepository.deleteAll()
			productRepository.deleteAll()
			ticketRepository.deleteAll()
		}
	}

	@Nested
	@DisplayName("PUT /API/tickets/{idTicket}/resolved")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	inner class ResolveTicket {

		@Test
		fun ticketNotFound() {
			val idTicket = -1
			val response = restTemplate.exchange("/API/tickets/$idTicket/resolved", HttpMethod.PUT, null, TicketNotFoundException::class.java)
			assert(response.body is TicketNotFoundException)
		}

		@Test
		fun historyNotFound() {
			val ticket = Ticket().apply {
				title = "Title"
				description = "Description"
				priority = PriorityLevel.NOT_ASSIGNED
			}
			ticketRepository.save(ticket)

			val response = restTemplate.exchange("/API/tickets/${ticket.getId()}/resolved", HttpMethod.PUT, null, HistoryNotFoundException::class.java)
			assert(response.body is HistoryNotFoundException)

			ticketRepository.deleteAll()
		}

		@Test
		fun operationNotPermitted() {
			val ticket = Ticket().apply {
				title = "Title"
				description = "Description"
				priority = PriorityLevel.NOT_ASSIGNED
			}
			ticketRepository.save(ticket)

			val history = History().apply {
				this.ticket = ticket
				employee = null
				date = LocalDateTime.now()
				state = TicketStatus.CLOSED
			}

			ticket.addHistory(history)

			ticketRepository.save(ticket)
			historyRepository.save(history)

			val response1 = restTemplate.exchange("/API/tickets/${ticket.getId()}/resolved", HttpMethod.PUT, null, OperationNotPermittedException::class.java)
			assert(response1.body is OperationNotPermittedException)

			val history2 = History().apply {
				this.ticket = ticket
				employee = null
				date = LocalDateTime.now()
				state = TicketStatus.CLOSED
			}

			ticket.addHistory(history2)

			ticketRepository.save(ticket)
			historyRepository.save(history2)

			val response2 = restTemplate.exchange("/API/tickets/${ticket.getId()}/resolved", HttpMethod.PUT, null, OperationNotPermittedException::class.java)
			assert(response2.body is OperationNotPermittedException)

			ticketRepository.deleteAll()
			historyRepository.deleteAll()

		}

		@Test
		fun ticketResolved() {
			val ticket = Ticket().apply {
				title = "Title"
				description = "Description"
				priority = PriorityLevel.NOT_ASSIGNED
			}
			ticketRepository.save(ticket)

			val history = History().apply {
				this.ticket = ticket
				employee = null
				date = LocalDateTime.now()
				state = TicketStatus.IN_PROGRESS
			}

			ticket.addHistory(history)

			ticketRepository.save(ticket)
			historyRepository.save(history)

			val response = restTemplate.exchange("/API/tickets/${ticket.getId()}/resolved", HttpMethod.PUT, null, HistoryNotFoundException::class.java)
			assert(response.statusCode == HttpStatus.ACCEPTED)

			ticketRepository.deleteAll()
			historyRepository.deleteAll()
		}


	}

	@Nested
	@DisplayName("PUT /API/tickets/{idTicket}/reopen")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	inner class ReopenTicket {

		@Test
		fun ticketNotFound() {
			val idTicket = -1
			val response = restTemplate.exchange("/API/tickets/$idTicket/resolved", HttpMethod.PUT, null, TicketNotFoundException::class.java)
			assert(response.body is TicketNotFoundException)
		}

		@Test
		fun historyNotFound() {
			val ticket = Ticket().apply {
				title = "Title"
				description = "Description"
				priority = PriorityLevel.NOT_ASSIGNED
			}
			ticketRepository.save(ticket)

			val response = restTemplate.exchange("/API/tickets/${ticket.getId()}/resolved", HttpMethod.PUT, null, HistoryNotFoundException::class.java)
			assert(response.body is HistoryNotFoundException)

			ticketRepository.deleteAll()
		}

		@Test
		fun operationNotPermitted() {
			val ticket = Ticket().apply {
				title = "Title"
				description = "Description"
				priority = PriorityLevel.NOT_ASSIGNED
			}
			ticketRepository.save(ticket)

			val history = History().apply {
				this.ticket = ticket
				employee = null
				date = LocalDateTime.now()
				state = TicketStatus.IN_PROGRESS
			}

			ticket.addHistory(history)

			ticketRepository.save(ticket)
			historyRepository.save(history)

			val response1 = restTemplate.exchange("/API/tickets/${ticket.getId()}/reopen", HttpMethod.PUT, null, OperationNotPermittedException::class.java)
			assert(response1.body is OperationNotPermittedException)
			historyRepository.deleteAll()

			val history2 = History().apply {
				this.ticket = ticket
				employee = null
				date = LocalDateTime.now()
				state = TicketStatus.OPEN
			}

			ticket.addHistory(history2)

			ticketRepository.save(ticket)
			historyRepository.save(history2)

			val response2 = restTemplate.exchange("/API/tickets/${ticket.getId()}/reopen", HttpMethod.PUT, null, OperationNotPermittedException::class.java)
			assert(response2.body is OperationNotPermittedException)
			historyRepository.deleteAll()

			val history3 = History().apply {
				this.ticket = ticket
				employee = null
				date = LocalDateTime.now()
				state = TicketStatus.REOPENED
			}

			ticket.addHistory(history3)

			ticketRepository.save(ticket)
			historyRepository.save(history3)

			val response3 = restTemplate.exchange("/API/tickets/${ticket.getId()}/reopen", HttpMethod.PUT, null, OperationNotPermittedException::class.java)
			assert(response3.body is OperationNotPermittedException)


			ticketRepository.deleteAll()
			historyRepository.deleteAll()

		}

		@Test
		fun ticketReopened() {
			val ticket = Ticket().apply {
				title = "Title"
				description = "Description"
				priority = PriorityLevel.NOT_ASSIGNED
			}
			ticketRepository.save(ticket)

			val history = History().apply {
				this.ticket = ticket
				employee = null
				date = LocalDateTime.now()
				state = TicketStatus.CLOSED
			}

			ticket.addHistory(history)

			ticketRepository.save(ticket)
			historyRepository.save(history)

			val response = restTemplate.exchange("/API/tickets/${ticket.getId()}/reopen", HttpMethod.PUT, null, HistoryNotFoundException::class.java)
			assert(response.statusCode == HttpStatus.ACCEPTED)

			ticketRepository.deleteAll()
			historyRepository.deleteAll()
		}


	}

	@Nested
	@DisplayName("PUT /API/tickets/{idTicket}/messages")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	inner class AddMessagesTicket {

		@Test
		fun ticketNotFound() {
			val idTicket = -1
			val response = restTemplate.exchange("/API/tickets/$idTicket/resolved", HttpMethod.PUT, null, TicketNotFoundException::class.java)
			assert(response.body is TicketNotFoundException)
		}

		@Test
		fun historyNotFound() {
			val ticket = Ticket().apply {
				title = "Title"
				description = "Description"
				priority = PriorityLevel.NOT_ASSIGNED
			}
			ticketRepository.save(ticket)

			val response = restTemplate.exchange("/API/tickets/${ticket.getId()}/resolved", HttpMethod.PUT, null, HistoryNotFoundException::class.java)
			assert(response.body is HistoryNotFoundException)

			ticketRepository.deleteAll()
		}

		@Test
		fun operationNotPermitted() {
			val ticket = Ticket().apply {
				title = "Title"
				description = "Description"
				priority = PriorityLevel.NOT_ASSIGNED
			}
			ticketRepository.save(ticket)

			val history = History().apply {
				this.ticket = ticket
				employee = null
				date = LocalDateTime.now()
				state = TicketStatus.IN_PROGRESS
			}

			ticket.addHistory(history)

			ticketRepository.save(ticket)
			historyRepository.save(history)

			val response1 = restTemplate.exchange("/API/tickets/${ticket.getId()}/reopen", HttpMethod.PUT, null, OperationNotPermittedException::class.java)
			assert(response1.body is OperationNotPermittedException)
			historyRepository.deleteAll()

			val history2 = History().apply {
				this.ticket = ticket
				employee = null
				date = LocalDateTime.now()
				state = TicketStatus.OPEN
			}

			ticket.addHistory(history2)

			ticketRepository.save(ticket)
			historyRepository.save(history2)

			val response2 = restTemplate.exchange("/API/tickets/${ticket.getId()}/reopen", HttpMethod.PUT, null, OperationNotPermittedException::class.java)
			assert(response2.body is OperationNotPermittedException)
			historyRepository.deleteAll()

			val history3 = History().apply {
				this.ticket = ticket
				employee = null
				date = LocalDateTime.now()
				state = TicketStatus.REOPENED
			}

			ticket.addHistory(history3)

			ticketRepository.save(ticket)
			historyRepository.save(history3)

			val response3 = restTemplate.exchange("/API/tickets/${ticket.getId()}/reopen", HttpMethod.PUT, null, OperationNotPermittedException::class.java)
			assert(response3.body is OperationNotPermittedException)


			ticketRepository.deleteAll()
			historyRepository.deleteAll()

		}

		@Test
		fun ticketReopened() {
			val ticket = Ticket().apply {
				title = "Title"
				description = "Description"
				priority = PriorityLevel.NOT_ASSIGNED
			}
			ticketRepository.save(ticket)

			val history = History().apply {
				this.ticket = ticket
				employee = null
				date = LocalDateTime.now()
				state = TicketStatus.CLOSED
			}

			ticket.addHistory(history)

			ticketRepository.save(ticket)
			historyRepository.save(history)

			val response = restTemplate.exchange("/API/tickets/${ticket.getId()}/reopen", HttpMethod.PUT, null, HistoryNotFoundException::class.java)
			assert(response.statusCode == HttpStatus.ACCEPTED)

			ticketRepository.deleteAll()
			historyRepository.deleteAll()
		}
	}

	fun initialization() {
		customer.first_name = "Pietro"
		customer.last_name = "Bertorelle"
		customer.email = "email@gmail.com"
		customer.dob = LocalDate.of(1998,9,13)
		customer.password = "password"
		customer.address = "Via Rivalta"
		customer.phone_number = "3466088800"
		customerRepository.save(customer)

		product.ean = "4935531465706"
		product.name = "JMT X-ring 530x2 Gold 104 Open Chain With Rivet Link for Kawasaki KH 400 a 1976"
		product.brand = "JMT"
		productRepository.save(product)

		expert.first_name = "Francesca"
		expert.last_name = "Ferritti"
		expert.email = "myemail@gmail.com"
		expert.password = "password"
		employeeRepository.save(expert)

		admin.first_name = "Giulio"
		admin.last_name = "Rossetti"
		admin.email = "myemail2@gmail.com"
		admin.password = "password"
		admin.type = EmployeeRole.ADMIN
		employeeRepository.save(admin)

		ticket.title = "Can't use the product"
		ticket.description = "How should i assemble the product?"
		ticket.priority = PriorityLevel.MEDIUM
		ticketRepository.save(ticket)

		message1.type = SenderType.EXPERT
		message1.body = "Try sending a picture"
		messageRepository.save(message1)

		message2.type = SenderType.CUSTOMER
		message2.body = "The picture is in the attachment!"
		messageRepository.save(message2)

		history2.state = TicketStatus.IN_PROGRESS
		historyRepository.save(history1)
		historyRepository.save(history2)

		//attachment.attachment = null
		//attachmentRepository.save(attachment)

		//the customer open the ticket
		customer.addTicket(ticket)
		//on a product
		product.addTicket(ticket)
		//the admin get the ticket as open
		admin.addHistory(history1)
		ticket.addHistory(history1)
		//the admin assign the ticket to the expert
		expert.addHistory(history2)
		ticket.addHistory(history2)
		//the expert send a message
		expert.addMessage(message1)
		ticket.addMessage(message1)
		//the customer prepare the message
		//message2.addAttachment(attachment)
		//the customer send the message
		expert.addMessage(message2)
		ticket.addMessage(message2)

		//save all
		customerRepository.save(customer)
		productRepository.save(product)
		ticketRepository.save(ticket)
		historyRepository.save(history1)
		historyRepository.save(history2)
		employeeRepository.save(expert)
		employeeRepository.save(admin)
		messageRepository.save(message1)
		messageRepository.save(message2)
		//attachmentRepository.save(attachment)
		//flush all
		customerRepository.flush()
		productRepository.flush()
		ticketRepository.flush()
		historyRepository.flush()
		employeeRepository.flush()
		messageRepository.flush()
		//attachmentRepository.flush()
	}
	@Test
	fun integrationTest(){
		initialization()
	}



}
