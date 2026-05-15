package codex.mmxxvi.repository;

import codex.mmxxvi.dto.response.ResponseTicket;
import codex.mmxxvi.entity.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface TicketRepository extends MongoRepository<Ticket, UUID> {
    ResponseTicket findTicketById(UUID id);
}
