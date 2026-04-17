\newpage

# Events

## Overview

The event system tracks all changes made to an entity throughout its lifecycle.
Every time an entity is created or updated, the system records an event that captures
what changed, who made the change, and when it happened.

> Currently, the event system supports **Tickets**. It is designed to be extended to other entities in the future.

---

## Event Types

Event types are grouped into constants defined in `EventConstants.java`. For tickets, they are:

### TICKET_STATUS_CHANGED (1L)

An action event is recorded when the **status of a ticket changes**.
The new status value (e.g., `OPEN`, `ASSIGNED`, `IN PROGRESS`, `RESOLVED`, `CLOSED`) is stored in the `event_value` payload field of the event.

### TICKET_CATEGORY_CHANGED (2L)

A category event is recorded when the **category of a ticket changes**.
The new category name (e.g., `BUG`, `FEATURE`, `QUESTION`) is stored in the `event_value` payload field of the event.

---

## How It Works

Events are triggered in two scenarios for a **Ticket**:

**1. Ticket Created**
When a user creates a new ticket, the system records:
- A `TICKET_STATUS_CHANGED` event (value e.g. `OPEN`)
- A `TICKET_CATEGORY_CHANGED` event matching the ticket's initial category (e.g. `BUG`)

**2. Ticket Updated**
When a support user updates a ticket, the system compares the ticket's current state
against the last recorded event for each type. A new event is only saved if something
has actually changed.

```text
Ticket updated
     │
     ├── Has the status changed since the last STATUS event?
     │        └── Yes → Save a new TICKET_STATUS_CHANGED event
     │
     └── Has the category changed since the last CATEGORY event?
              └── Yes → Save a new TICKET_CATEGORY_CHANGED event
```

---

## Event Structure

| Field | Type | Description |
|---|---|---|
| `id` | Long | Unique identifier |
| `eventType` | Long | The classification of the event (e.g. `1` for status change) |
| `entityId` | Long | ID of the entity this event belongs to |
| `companyId` | Long | The ID of the company the ticket belongs to |
| `userId` | Long | The ID of the user who triggered the event |
| `eventValue` | String | The payload/value of the change (e.g. `OPEN`) |
| `createdAt` | LocalDateTime | Timestamp of when the event was recorded |

---
