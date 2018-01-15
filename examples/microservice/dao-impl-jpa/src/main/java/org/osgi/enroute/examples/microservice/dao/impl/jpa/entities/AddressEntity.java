package org.osgi.enroute.examples.microservice.dao.impl.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.osgi.enroute.examples.microservice.dao.dto.AddressDTO;

@Entity
@Table(name="addresses")
public class AddressEntity {

    @ManyToOne
    @JoinColumn(name="person_id", foreignKey=@ForeignKey(name="person"))
    private PersonEntity person;
    
    @Id
    @Column(name="email_address")
    private String emailAddress;
    private String city;
    private String country;
    
    public static AddressEntity fromDTO(PersonEntity person, AddressDTO dto) {
        AddressEntity entity = new AddressEntity();
        entity.person = person;
        entity.emailAddress = dto.emailAddress;
        entity.city = dto.city;
        entity.country = dto.country;
        
        return entity;
    }
    
    public AddressDTO toDTO() {
        AddressDTO dto = new AddressDTO();
        dto.personId = person.getPersonId();
        dto.emailAddress = emailAddress;
        dto.city = city;
        dto.country = country;
        
        return dto;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
