package org.osgi.enroute.examples.microservice.dao.impl.jpa;

import static java.util.stream.Collectors.toList;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.osgi.enroute.examples.microservice.dao.AddressDao;
import org.osgi.enroute.examples.microservice.dao.dto.AddressDTO;
import org.osgi.enroute.examples.microservice.dao.impl.jpa.entities.AddressEntity;
import org.osgi.enroute.examples.microservice.dao.impl.jpa.entities.PersonEntity;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AddressDaoImpl implements AddressDao {

    private static final Logger logger = LoggerFactory.getLogger(AddressDaoImpl.class);

	@Reference
	TransactionControl transactionControl;

	@Reference(name="provider")
	JPAEntityManagerProvider jpaEntityManagerProvider;

	EntityManager em;

	@Activate
	void activate(Map<String, Object> props) throws SQLException {
		em = jpaEntityManagerProvider.getResource(transactionControl);
	}

	@Override
	public List<AddressDTO> select(Long personId) {

		return transactionControl.notSupported(() -> {

		    CriteriaBuilder builder = em.getCriteriaBuilder();
		    
		    CriteriaQuery<AddressEntity> query = builder.createQuery(AddressEntity.class);
		    
		    Root<AddressEntity> from = query.from(AddressEntity.class);
		    
		    query.where(builder.equal(from.get("person").get("personId"), personId));
		    
		    return em.createQuery(query).getResultList().stream()
		            .map(AddressEntity::toDTO)
		            .collect(toList());
		});
	}

	@Override
	public AddressDTO findByPK(String pk) {

		return transactionControl.supports(() -> {
		    AddressEntity address = em.find(AddressEntity.class, pk);
			return address == null ? null : address.toDTO();
		});
	}

	@Override
	public void save(Long personId, AddressDTO data) {
	    
	    transactionControl.required(() -> {
	        PersonEntity person = em.find(PersonEntity.class, personId);
	        if(person == null) {
	            throw new IllegalArgumentException("There is no person with id " + personId);
	        }
	        em.persist(AddressEntity.fromDTO(person, data));
	        
	        return null;
	    });
	}

	@Override
	public void update(Long personId, AddressDTO data) {
	    
	    transactionControl.required(() -> {
	        
	        AddressEntity address = em.find(AddressEntity.class, data.emailAddress);
	        if(address == null) {
                throw new IllegalArgumentException("There is no address with email " + data.emailAddress);
            }
	        
	        address.setCity(data.city);
	        address.setCountry(data.country);
	        
	        logger.info("Updated Person Address : {}", data);
	        
	        return null;
	    });
	}

	@Override
	public void delete(Long personId) {
	    
		transactionControl.required(() -> {
		    CriteriaBuilder builder = em.getCriteriaBuilder();
            
            CriteriaDelete<AddressEntity> query = builder.createCriteriaDelete(AddressEntity.class);
            
            Root<AddressEntity> from = query.from(AddressEntity.class);
            
            query.where(builder.equal(from.get("person").get("personId"), personId));
            
            em.createQuery(query).executeUpdate();
            
            return null;
		});
	}
}