package org.osgi.enroute.examples.microservice.dao.impl;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.osgi.enroute.examples.microservice.dao.impl.PersonTable.FIRST_NAME;
import static org.osgi.enroute.examples.microservice.dao.impl.PersonTable.INIT;
import static org.osgi.enroute.examples.microservice.dao.impl.PersonTable.LAST_NAME;
import static org.osgi.enroute.examples.microservice.dao.impl.PersonTable.PERSON_ID;
import static org.osgi.enroute.examples.microservice.dao.impl.PersonTable.SQL_DELETE_PERSON_BY_PK;
import static org.osgi.enroute.examples.microservice.dao.impl.PersonTable.SQL_INSERT_PERSON;
import static org.osgi.enroute.examples.microservice.dao.impl.PersonTable.SQL_SELECT_ALL_PERSONS;
import static org.osgi.enroute.examples.microservice.dao.impl.PersonTable.SQL_SELECT_PERSON_BY_PK;
import static org.osgi.enroute.examples.microservice.dao.impl.PersonTable.SQL_UPDATE_PERSON_BY_PK;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.enroute.examples.microservice.dao.AddressDao;
import org.osgi.enroute.examples.microservice.dao.PersonDao;
import org.osgi.enroute.examples.microservice.dao.dto.PersonDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PersonDaoImpl implements PersonDao {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonDaoImpl.class);

    @Reference
    TransactionControl transactionControl;

    @Reference(name="provider")
    JDBCConnectionProvider jdbcConnectionProvider;

    @Reference
    AddressDao addressDao;

    Connection connection;

    @Activate
    void start(Map<String, Object> props) throws SQLException {
        connection = jdbcConnectionProvider.getResource(transactionControl);
        transactionControl.supports(() -> connection.prepareStatement(INIT).execute());
    }

    @Override
    public List<PersonDTO> select() {

        return transactionControl.notSupported(() -> {

            List<PersonDTO> dbResults = new ArrayList<>();

            ResultSet rs = connection.createStatement().executeQuery(SQL_SELECT_ALL_PERSONS);

            while (rs.next()) {
                PersonDTO personDTO = mapRecordToPerson(rs);
                personDTO.addresses = addressDao.select(personDTO.personId);
                dbResults.add(personDTO);
            }

            return dbResults;
        });
    }

    @Override
    public void delete(Long primaryKey) {

        transactionControl.required(() -> {
            PreparedStatement pst = connection.prepareStatement(SQL_DELETE_PERSON_BY_PK);
            pst.setLong(1, primaryKey);
            pst.executeUpdate();
            addressDao.delete(primaryKey);
            logger.info("Deleted Person with ID : {}", primaryKey);
            return null;
        });
    }

    @Override
    public PersonDTO findByPK(Long pk) {

       return transactionControl.supports(() -> {

            PersonDTO personDTO = null;

            PreparedStatement pst = connection.prepareStatement(SQL_SELECT_PERSON_BY_PK);
            pst.setLong(1, pk);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                personDTO = mapRecordToPerson(rs);
                personDTO.addresses = addressDao.select(pk);
            }

            return personDTO;
        });
    }

    @Override
    public Long save(PersonDTO data) {

        return transactionControl.required(() -> {

            PreparedStatement pst = connection.prepareStatement(SQL_INSERT_PERSON, RETURN_GENERATED_KEYS);

            pst.setString(1, data.firstName);
            pst.setString(2, data.lastName);

            pst.executeUpdate();

            AtomicLong genPersonId = new AtomicLong(data.personId);

            if (genPersonId.get() <= 0) {
                ResultSet genKeys = pst.getGeneratedKeys();

                if (genKeys.next()) {
                    genPersonId.set(genKeys.getLong(1));
                }
            }

            logger.info("Saved Person with ID : {}", genPersonId.get());

            if (genPersonId.get() > 0) {
                data.addresses.stream().forEach(address -> {
                    address.personId = genPersonId.get();
                    addressDao.save(genPersonId.get(), address);
                });
            }

            return genPersonId.get();
        });
    }

    @Override
    public void update(PersonDTO data) {

        transactionControl.required(() -> {

            PreparedStatement pst = connection.prepareStatement(SQL_UPDATE_PERSON_BY_PK);
            pst.setString(1, data.firstName);
            pst.setString(2, data.lastName);
            pst.setLong(3, data.personId);
            pst.executeUpdate();

            logger.info("Updated person : {}", data);

            data.addresses.stream().forEach(address -> addressDao.update(data.personId, address));
            
            return null;
        });
    }

    protected PersonDTO mapRecordToPerson(ResultSet rs) throws SQLException {
        PersonDTO personDTO = new PersonDTO();
        personDTO.personId = rs.getLong(PERSON_ID);
        personDTO.firstName = rs.getString(FIRST_NAME);
        personDTO.lastName = rs.getString(LAST_NAME);
        return personDTO;
    }
}
