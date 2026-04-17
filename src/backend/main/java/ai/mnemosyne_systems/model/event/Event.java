/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */
package ai.mnemosyne_systems.model.event;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "event_type")
    public Long eventType;

    @CreationTimestamp
    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "entity_id")
    public Long entityId;

    @Column(name = "company_id")
    public Long companyId;

    @Column(name = "user_id")
    public Long userId;

    @Column(name = "event_value")
    public String eventValue;
}
