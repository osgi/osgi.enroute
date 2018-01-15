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

import org.osgi.enroute.examples.microservice.dao.PersonDao;
import org.osgi.enroute.examples.microservice.dao.dto.PersonDTO;
import org.osgi.enroute.examples.microservice.dao.impl.jpa.entities.PersonEntity;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.jpa.JPAEntityManagerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PersonDaoImpl implements PersonDao {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonDaoImpl.class);

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
    public List<PersonDTO> select() {

        return transactionControl.notSupported(() -> {

            CriteriaBuilder builder = em.getCriteriaBuilder();
            
            CriteriaQuery<PersonEntity> query = builder.createQuery(PersonEntity.class);
            
            query.from(PersonEntity.class);
            
            return em.createQuery(query).getResultList().stream()
                    .map(PersonEntity::toDTO)
                    .collect(toList());
        });
    }

    @Override
    public void delete(Long primaryKey) {

        transactionControl.required(() -> {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            
            CriteriaDelete<PersonEntity> query = builder.createCriteriaDelete(PersonEntity.class);
            
            Root<PersonEntity> from = query.from(PersonEntity.class);
            
            query.where(builder.equal(from.get("personId"), primaryKey));
            
            em.createQuery(query).executeUpdate();
            
            logger.info("Deleted Person with ID : {}", primaryKey);
            return null;
        });
    }

    @Override
    public PersonDTO findByPK(Long pk) {

       return transactionControl.supports(() -> {
           PersonEntity person = em.find(PersonEntity.class, pk);
           return person == null ? null : person.toDTO();
        });
    }

    @Override
    public Long save(PersonDTO data) {

        return transactionControl.required(() -> {

            PersonEntity entity = PersonEntity.fromDTO(data);
            
            if(entity.getPersonId() == null) {
                em.persist(entity);
            } else {
                em.merge(entity);
            }

            logger.info("Saved Person with ID : {}", entity.getPersonId());

            return entity.getPersonId();
        });
    }

    @Override
    public void update(PersonDTO data) {

        transactionControl.required(() -> {

            PersonEntity entity = PersonEntity.fromDTO(data);
            
            if(entity.getPersonId() <= 0) {
                throw new IllegalStateException("No primary key defined for the Entity");
            } else {
                em.merge(entity);
            }

            logger.info("Updated person : {}", data);

            return null;
        });
    }
}
