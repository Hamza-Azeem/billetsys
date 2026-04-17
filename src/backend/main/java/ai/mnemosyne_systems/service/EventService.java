/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */
package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.event.Event;
import ai.mnemosyne_systems.model.event.EventConstants;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class EventService {

    public void saveTicketEvent(Ticket ticket, User createdBy) {
        if (ticket.status != null) {
            handleActionEvent(ticket, createdBy);
        }
        if (ticket.category != null && ticket.category.name != null) {
            handleCategoryEvent(ticket, createdBy);
        }
    }

    public List<Event> getAllChangesToEntity(Long entityId) {
        return Event.find("entityId = :entityId", Sort.by("createdAt").ascending(), Map.of("entityId", entityId))
                .list();
    }

    private void handleActionEvent(Ticket ticket, User createdBy) {
        Event lastActionEvent = Event
                .find("entityId = :entityId AND eventType = :type", Sort.by("createdAt").descending(),
                        Map.of("entityId", ticket.id, "type", EventConstants.TICKET_STATUS_CHANGED))
                .firstResult();

        boolean hasChanged = lastActionEvent == null || !ticket.status.toUpperCase().equals(lastActionEvent.eventValue);

        if (hasChanged) {
            saveEvent(ticket.id, EventConstants.TICKET_STATUS_CHANGED, ticket.status.toUpperCase(), ticket.company.id,
                    createdBy.id);
        }
    }

    private void handleCategoryEvent(Ticket ticket, User createdBy) {
        Event lastCategoryEvent = Event
                .find("entityId = :entityId AND eventType = :type", Sort.by("createdAt").descending(),
                        Map.of("entityId", ticket.id, "type", EventConstants.TICKET_CATEGORY_CHANGED))
                .firstResult();

        boolean hasChanged = lastCategoryEvent == null
                || !ticket.category.name.toUpperCase().equals(lastCategoryEvent.eventValue);

        if (hasChanged) {
            saveEvent(ticket.id, EventConstants.TICKET_CATEGORY_CHANGED, ticket.category.name.toUpperCase(),
                    ticket.company.id, createdBy.id);
        }
    }

    private void saveEvent(Long entityId, Long eventType, String eventValue, Long companyId, Long userId) {
        Event event = new Event();
        event.entityId = entityId;
        event.eventType = eventType;
        event.eventValue = eventValue;
        event.companyId = companyId;
        event.userId = userId;
        event.persist();
    }
}
