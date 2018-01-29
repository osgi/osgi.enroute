package org.osgi.enroute.examples.microservice.dao.impl.jpa.entities;

import static java.util.stream.Collectors.toList;
import static javax.persistence.CascadeType.ALL;
import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.osgi.enroute.examples.microservice.dao.dto.PersonDTO;

@Entity
@Table(name="persons")
public class PersonEntity {
    
    @GeneratedValue(strategy = IDENTITY)
    @Id
    @Column(name="person_id")
    private Long personId;
    
    @Column(name="first_name")
    private String firstName;
    
    @Column(name="last_name")
    private String lastName;

    @OneToMany(mappedBy="person", cascade=ALL)
    private List<AddressEntity> addresses = new ArrayList<>();

    public Long getPersonId() {
        return personId;
    }

    public PersonDTO toDTO() {
        PersonDTO dto = new PersonDTO();
        dto.personId = personId;
        dto.firstName = firstName;
        dto.lastName = lastName;
        dto.addresses = addresses.stream()
                .map(AddressEntity::toDTO)
                .collect(toList());
        return dto;
    }
    
    public static PersonEntity fromDTO(PersonDTO dto) {
        PersonEntity entity = new PersonEntity();
        if(dto.personId != 0) {
            entity.personId = Long.valueOf(dto.personId);
        }
        entity.firstName = dto.firstName;
        entity.lastName = dto.lastName;
        entity.addresses = dto.addresses.stream()
                .map(a -> AddressEntity.fromDTO(entity, a))
                .collect(toList());
        
        return entity;
    }
}
