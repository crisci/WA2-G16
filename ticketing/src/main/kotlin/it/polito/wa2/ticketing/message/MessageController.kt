package it.polito.wa2.ticketing.message

import it.polito.wa2.ticketing.attachment.Attachment
import it.polito.wa2.ticketing.attachment.AttachmentDTO
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class MessageController(val messageService: MessageService) {

    @GetMapping("/API/messages/{messageId}/attachments")
    @ResponseStatus(HttpStatus.OK)
    fun getMessageAttachments(@PathVariable messageId: Long): Set<ByteArray>{
        return messageService.getMessageAttachments(messageId)
    }

    @PostMapping("/API/messages/{messageId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    fun addAttachments(@PathVariable messageId: Long, @RequestBody attachment: Array<MultipartFile>){
        messageService.addAttachment(messageId, attachment)
    }

    @PutMapping("/API/messages/{messageId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun editMessage(@PathVariable messageId: Long, @RequestBody message: String){
        messageService.editMessage(messageId, message)
    }

}