package it.polito.wa2.ticketing.integrationTest

import it.polito.wa2.ticketing.attachment.Attachment
import it.polito.wa2.ticketing.attachment.AttachmentDTO
import it.polito.wa2.ticketing.attachment.AttachmentRepository
import it.polito.wa2.ticketing.customer.*
import it.polito.wa2.ticketing.employee.*
import it.polito.wa2.ticketing.history.History
import it.polito.wa2.ticketing.history.HistoryRepository
import it.polito.wa2.ticketing.history.OperationNotPermittedException
import it.polito.wa2.ticketing.message.*
import it.polito.wa2.ticketing.product.*
import it.polito.wa2.ticketing.product.BlankFieldsException
import it.polito.wa2.ticketing.ticket.*
import it.polito.wa2.ticketing.utils.EmployeeRole
import it.polito.wa2.ticketing.utils.PriorityLevel
import it.polito.wa2.ticketing.utils.TicketStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.jvm.JvmStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Testcontainers
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
class IntegrationTest {
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
    @Autowired
    lateinit var ticketService: TicketService
    @Autowired
    lateinit var messageService: MessageService
    @Autowired
    lateinit var expertService: ExpertService
    @Autowired
    lateinit var customerService: CustomerService
    @Autowired
    lateinit var managerService: ManagerService
    @Autowired
    lateinit var productService: ProductService


    var customer: Customer = Customer()
    var product: Product = Product()
    var ticket: Ticket = Ticket()
    var history1: History = History()
    var history2: History = History()
    var expert: Employee = Employee()
    var admin: Employee = Employee()
    var message1: Message = Message()
    var message2: Message = Message()
    var attachment: Attachment = Attachment()

    fun myInit1() {
        customerRepository.deleteAll()
        productRepository.deleteAll()
        ticketRepository.deleteAll()
        historyRepository.deleteAll()
        employeeRepository.deleteAll()
        messageRepository.deleteAll()
        attachmentRepository.deleteAll()

        customerRepository.flush()
        productRepository.flush()
        ticketRepository.flush()
        historyRepository.flush()
        employeeRepository.flush()
        messageRepository.flush()
        attachmentRepository.flush()

        customer.first_name = "Pietro"
        customer.last_name = "Bertorelle"
        customer.email = "email@gmail.com"
        customer.dob = LocalDate.of(1998,9,13)
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
        employeeRepository.save(expert)

        admin.first_name = "Giulio"
        admin.last_name = "Rossetti"
        admin.email = "myemail2@gmail.com"
        admin.type = EmployeeRole.MANAGER
        employeeRepository.save(admin)

        ticket.title = "Can't use the product"
        ticket.description = "How should i assemble the product?"
        ticket.priority = PriorityLevel.MEDIUM
        ticketRepository.save(ticket)

        message1.body = "Try sending a picture"
        messageRepository.save(message1)

        message2.body = "The picture is in the attachment!"
        messageRepository.save(message2)

        history1.date = LocalDateTime.now().minusDays(1)
        historyRepository.save(history1)

        history2.state = TicketStatus.IN_PROGRESS
        historyRepository.save(history2)

        attachment.attachment = "Attachment Test".toByteArray()
        attachmentRepository.save(attachment)

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
    @Transactional
    @Test
    fun expertOperatingOnTicketsTest(){
        myInit1()

        val ticketId =  ticket.getId()!!
        assert(ticketRepository.findByIdOrNull(ticketId) == ticket)
        val hs = historyRepository.findByTicketIdOrderByDateDesc(ticketId)
        val i = hs.iterator()
        assert(i.next() == history2)
        assert(i.next() == history1)
        assert(!i.hasNext())

        val expertId = expert.getId()!!
        val adminId = admin.getId()!!
        assert(employeeRepository.findByIdOrNull(expertId) == expert)
        assert(employeeRepository.findByIdOrNull(adminId) == admin)

        val tickets = expertService.getTickets(expertId)
        assert(tickets.size == 1)

        ticketService.getMessages(ticketId).iterator()
        assertThrows<TicketNotFoundException> {
            ticketService.getMessages(ticketId.inc())
        }

        assert(ticketService.getStatus(ticketId) == TicketStatus.IN_PROGRESS)
        assertThrows<TicketNotFoundException> {
            ticketService.getStatus(ticketId.inc())
        }

        assertThrows<TicketNotFoundException> {
            expertService.reassignTicket(ticketId.inc(),expertId)
        }
        expertService.reassignTicket(ticketId,expertId)
        assertThrows<OperationNotPermittedException> {
            expertService.reassignTicket(ticketId,expertId)
        }

        assertThrows<TicketNotFoundException> {
            ticketService.closeTicket(ticketId.inc())
        }
        ticketService.closeTicket(ticketId)
    }

    @Test
    fun operationNotPermitted() {
        //RESOLVED When Ticket is CLOSED or RESOLVED is not permitted
        customer.apply {
            first_name = "Luigi"
            last_name = "Crisci"
            email = "xyz@xyz.it"
            dob = LocalDate.of(1998,9,13)
            address = "Via Rivalta"
            phone_number = "0000000000"
        }
        customerRepository.save(customer)

        product.apply {
            ean = "4935531465706"
            name = "JMT X-ring 530x2 Gold 104 Open Chain With Rivet Link for Kawasaki KH 400 a 1976"
            brand = "JMT"
        }
        productRepository.save(product)

        ticket.apply {
            title = "Can't use the product"
            description = "How should i assemble the product?"
            priority = PriorityLevel.HIGH
        }
        ticketRepository.save(ticket)

        expert.apply {
            first_name = "Giorgio"
            last_name = "P"
            email = "giorgio.p@polito.i"
            type = EmployeeRole.EXPERT
        }
        employeeRepository.save(expert)

        history1.apply {
            state = TicketStatus.CLOSED
            date = LocalDateTime.now()
        }
        historyRepository.save(history1)

        product.addTicket(ticket)
        customer.addTicket(ticket)
        product.addTicket(ticket)
        ticket.addHistory(history1)

        customerRepository.save(customer)
        employeeRepository.save(expert)
        productRepository.save(product)
        ticketRepository.save(ticket)
        historyRepository.save(history1)

        assertThrows<OperationNotPermittedException> {
            customerService.resolveTicket(ticket.toTicketDTO().id!!)
        }

        history1.apply {
            state = TicketStatus.RESOLVED
        }
        historyRepository.save(history1)
        assertThrows<OperationNotPermittedException> {
            customerService.resolveTicket(ticket.toTicketDTO().id!!)
        }

        customerRepository.flush()
        productRepository.flush()
        ticketRepository.flush()
        historyRepository.flush()
        employeeRepository.flush()
    }

    @Test
    fun messageTesting() {
        customer.apply {
            first_name = "Daniel"
            last_name = "Panaite"
            email = "dani@polito.it"
            dob = LocalDate.of(1998,9,15)
            address = "Via Po"
            phone_number = "0000000000"
        }
        customerRepository.save(customer)

        product.apply {
            ean = "928173912863"
            name = "AirPods"
            brand = "Pear Electronics"
        }
        productRepository.save(product)

        ticket.apply {
            title = "Non an original product"
            description = "Why is there no apple on the logo?"
            priority = PriorityLevel.HIGH
        }
        ticketRepository.save(ticket)

        expert.apply {
            first_name = "Mario"
            last_name = "Antonio"
            email = "mario.anto@polito.it"
            type = EmployeeRole.EXPERT
        }
        employeeRepository.save(expert)

        history1.apply {
            state = TicketStatus.CLOSED
            date = LocalDateTime.now()
        }
        historyRepository.save(history1)

        product.addTicket(ticket)
        customer.addTicket(ticket)
        product.addTicket(ticket)
        ticket.addHistory(history1)

        customerRepository.save(customer)
        employeeRepository.save(expert)
        productRepository.save(product)
        ticketRepository.save(ticket)
        historyRepository.save(history1)

        message1.body = "Try sending a picture"
        message1.expert = expert
        messageRepository.save(message1)

        expert.addMessage(message1)
        ticket.addMessage(message1)

        attachment.attachment = "Attachment Test".toByteArray()
        attachmentRepository.save(attachment)

        message2.body = "Here it is."
        messageRepository.save(message2)

        expert.addMessage(message2)
        ticket.addMessage(message2)

        message2.addAttachment(attachment)
        expert.addMessage(message2)
        ticket.addMessage(message2)

        message2.body = "Added attachment."
        messageService.editMessage(message2.getId()!!, message2.body)

        assert(messageRepository.findByIdOrNull(message1.getId()) == message1)
        assert(messageRepository.findByIdOrNull(message2.getId()) == message2)
        message2.listOfAttachment.forEach{
            assert(attachmentRepository.findByIdOrNull(it.getId()!!) == it)
        }
        assert(!message2.listOfAttachment.isEmpty())

        assertThrows<MessageNotFoundException> {
            messageService.addAttachment(message2.getId()!!.inc(), arrayOf(MockMultipartFile("attachment", "Meow".toByteArray())))
        }

        customerRepository.flush()
        productRepository.flush()
        ticketRepository.flush()
        historyRepository.flush()
        employeeRepository.flush()
    }

    @Test
    fun ticketAssignedCorrectly() {
        customer.apply {
            first_name = "Luigi"
            last_name = "Crisci"
            email = "xyz@xyz.it"
            dob = LocalDate.of(1998,9,13)
            address = "Via Rivalta"
            phone_number = "0000000000"
        }
        customerRepository.save(customer)

        product.apply {
            ean = "4935531465706"
            name = "JMT X-ring 530x2 Gold 104 Open Chain With Rivet Link for Kawasaki KH 400 a 1976"
            brand = "JMT"
        }
        productRepository.save(product)

        ticket.apply {
            title = "Can't use the product"
            description = "How should i assemble the product?"
            priority = PriorityLevel.HIGH
        }
        ticketRepository.save(ticket)

        expert.apply {
            first_name = "Giorgio"
            last_name = "P"
            email = "giorgio.p@polito.i"
            type = EmployeeRole.EXPERT
        }
        employeeRepository.save(expert)

        history1.apply {
            state = TicketStatus.OPEN
            date = LocalDateTime.now()
        }
        historyRepository.save(history1)

        product.addTicket(ticket)
        customer.addTicket(ticket)
        product.addTicket(ticket)
        ticket.addHistory(history1)

        customerRepository.save(customer)
        employeeRepository.save(expert)
        productRepository.save(product)
        ticketRepository.save(ticket)
        historyRepository.save(history1)

        assertThrows<TicketNotFoundException> {
            managerService.assignTicket(-1, expert.getId()!!, PriorityLevel.HIGH)
        }

        assertThrows<ExpertNotFoundException> {
            managerService.assignTicket(ticket.getId()!!, UUID.randomUUID(), PriorityLevel.HIGH)
        }

        managerService.assignTicket(ticket.getId()!!, expert.getId()!!, PriorityLevel.HIGH)
        assert(historyRepository.findByTicketIdOrderByDateDesc(ticketId = ticket.getId()!!).first().state == TicketStatus.IN_PROGRESS)


        assertThrows<OperationNotPermittedException>() {
            managerService.assignTicket(ticket.getId()!!, expert.getId()!!, PriorityLevel.HIGH)
        }


        customerRepository.deleteAll()
        productRepository.deleteAll()
        ticketRepository.deleteAll()
        historyRepository.deleteAll()
        employeeRepository.deleteAll()

    }

    @Test
    fun messageSendedCorrectly() {
        customer.apply {
            first_name = "Luigi"
            last_name = "Crisci"
            email = "xyz@xyz.it"
            dob = LocalDate.of(1998,9,13)
            address = "Via Rivalta"
            phone_number = "0000000000"
        }
        customerRepository.save(customer)

        product.apply {
            ean = "4935531465706"
            name = "JMT X-ring 530x2 Gold 104 Open Chain With Rivet Link for Kawasaki KH 400 a 1976"
            brand = "JMT"
        }
        productRepository.save(product)

        ticket.apply {
            title = "Can't use the product"
            description = "How should i assemble the product?"
            priority = PriorityLevel.HIGH
        }
        ticketRepository.save(ticket)

        expert.apply {
            first_name = "Giorgio"
            last_name = "P"
            email = "giorgio.p@polito.i"
            type = EmployeeRole.EXPERT
        }
        employeeRepository.save(expert)

        history1.apply {
            state = TicketStatus.OPEN
            date = LocalDateTime.now()
        }
        historyRepository.save(history1)

        product.addTicket(ticket)
        customer.addTicket(ticket)
        product.addTicket(ticket)
        ticket.addHistory(history1)

        customerRepository.save(customer)
        employeeRepository.save(expert)
        productRepository.save(product)
        ticketRepository.save(ticket)
        historyRepository.save(history1)

        assertThrows<TicketNotFoundException> {
            expertService.addMessage(-1, MessageDTO(null, "Test", null, mutableSetOf<AttachmentDTO>(), null), expert.getId()!!)
        }

        assertThrows<TicketNotFoundException> {
            customerService.addMessage(-1, MessageDTO(null, "Test", null, mutableSetOf<AttachmentDTO>(), null))
        }

        assertThrows<OperationNotPermittedException> {
            expertService.addMessage(ticket.getId()!!, MessageDTO(null, "Test", null, mutableSetOf<AttachmentDTO>(), null), expert.getId()!!)
        }

        assertThrows<OperationNotPermittedException> {
            customerService.addMessage(ticket.getId()!!, MessageDTO(null, "Test", null, mutableSetOf<AttachmentDTO>(), null))
        }

        managerService.assignTicket(ticket.getId()!!, expert.getId()!!, PriorityLevel.HIGH)
        assert(historyRepository.findByTicketIdOrderByDateDesc(ticketId = ticket.getId()!!).first().state == TicketStatus.IN_PROGRESS)

        customerService.addMessage(ticket.getId()!!, MessageDTO(null, "Test", null, mutableSetOf<AttachmentDTO>(), null))
        expertService.addMessage(ticket.getId()!!, MessageDTO(null, "Test", null, mutableSetOf<AttachmentDTO>(), null), expert.getId()!!)

        assert(managerService.getTicketMessages(ticket.getId()!!)?.size == 2)


        customerRepository.deleteAll()
        productRepository.deleteAll()
        ticketRepository.deleteAll()
        historyRepository.deleteAll()
        employeeRepository.deleteAll()

    }

    @Test
    fun productAddedCorrectly() {
        customer.apply {
            first_name = "Daniel"
            last_name = "Panaite"
            email = "dani@polito.it"
            dob = LocalDate.of(1998,9,15)
            address = "Via Po"
            phone_number = "0000000000"
        }
        customerRepository.save(customer)

        product.apply {
            ean = "4935531465706"
            name = "JMT X-ring 530x2 Gold 104 Open Chain With Rivet Link for Kawasaki KH 400 a 1976"
            brand = "JMT"
        }
        productRepository.save(product)

        product.apply {
            ean = "89126408921321"
            name = "PearPods"
            brand = "Pear"
        }
        productRepository.save(product)

        expert.apply {
            first_name = "Mario"
            last_name = "Antonio"
            email = "mario.anto@polito.it"
            type = EmployeeRole.MANAGER
        }
        employeeRepository.save(expert)

        assertThrows<ProductNotFoundException> {
            productService.getProduct("123123123")
        }

        assertThrows<ProductNotFoundException> {
            productService.updateProduct("123123123", ProductDTO("123123123", "PearPods", "Pear"))
        }

        assertThrows<ProductNotFoundException> {
            productService.deleteProduct("123123123")
        }

        assertThrows<BlankFieldsException> {
            productService.updateProduct("4935531465706", ProductDTO("","PearPods", "Pear"))
        }

        assert(productRepository.findProductByEan("89126408921321")?.name == "PearPods")

        productService.updateProduct("89126408921321", ProductDTO("89126408921321", "FruitPods", "Pear"))
        assert(productRepository.findProductByEan("89126408921321")?.name == "FruitPods")

        assert(productService.getAllProducts().size == 2 )

        customerRepository.deleteAll()
        productRepository.deleteAll()
        employeeRepository.deleteAll()

    }

    @Test
    fun customerTests() {
        customer.apply {
            first_name = "Daniel"
            last_name = "Panaite"
            email = "dani@polito.it"
            dob = LocalDate.of(1998,9,15)
            address = "Via Po"
            phone_number = "0000000000"
        }
        customerRepository.save(customer)

        product.apply {
            ean = "89126408921321"
            name = "PearPods"
            brand = "Pear"
        }
        productRepository.save(product)

        message1.apply {
            body = "Ciao"
        }
        messageRepository.save(message1)

        ticket.apply {
            title = "Can't use the product"
            description = "How should i assemble the product?"
            priority = PriorityLevel.HIGH
        }
        ticketRepository.save(ticket)

        history1.apply {
            state = TicketStatus.OPEN
            date = LocalDateTime.now()
        }
        historyRepository.save(history1)

        assertThrows<CustomerNotFoundException> {
            customerService.getCustomerByEmail("meow@polito.it")
        }

        assertThrows<DuplicatedEmailException> {
            customerService.insertCustomer(customer.toDTO())
        }

        assertThrows<OperationNotPermittedException> {
            customerService.addMessage(1, message1.toDTO())
        }

        customerRepository.deleteAll()
        productRepository.deleteAll()
        employeeRepository.deleteAll()
        messageRepository.deleteAll()
        historyRepository.deleteAll()

    }



}
