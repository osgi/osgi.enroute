package org.osgi.enroute.examples.microservice.dao.impl;

import static org.osgi.enroute.examples.microservice.dao.impl.AddressTable.SQL_ADD_ADDRESS;
import static org.osgi.enroute.examples.microservice.dao.impl.AddressTable.SQL_DELETE_ALL_ADDRESS_BY_PERSON_ID;
import static org.osgi.enroute.examples.microservice.dao.impl.AddressTable.SQL_SELECT_ADDRESS_BY_PERSON;
import static org.osgi.enroute.examples.microservice.dao.impl.AddressTable.SQL_SELECT_ADDRESS_BY_PK;
import static org.osgi.enroute.examples.microservice.dao.impl.AddressTable.SQL_UPDATE_ADDRESS_BY_PK_AND_PERSON_ID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.enroute.examples.microservice.dao.AddressDao;
import org.osgi.enroute.examples.microservice.dao.dto.AddressDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.transaction.control.TransactionControl;
import org.osgi.service.transaction.control.jdbc.JDBCConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AddressDaoImpl implements AddressDao {

    private static final Logger logger = LoggerFactory.getLogger(AddressDaoImpl.class);

	@Reference
	TransactionControl transactionControl;

	@Reference(name="provider")
	JDBCConnectionProvider jdbcConnectionProvider;

	Connection connection;

	@Activate
	void activate(Map<String, Object> props) throws SQLException {
		connection = jdbcConnectionProvider.getResource(transactionControl);
		transactionControl.supports( () -> connection.prepareStatement(AddressTable.INIT).execute());
	}

	@Override
	public List<AddressDTO> select(Long personId) {

		return transactionControl.notSupported(() -> {

			List<AddressDTO> dbResults = new ArrayList<>();

			PreparedStatement pst = connection.prepareStatement(SQL_SELECT_ADDRESS_BY_PERSON);
			pst.setLong(1, personId);

			ResultSet rs = pst.executeQuery();

			while (rs.next()) {
				AddressDTO addressDTO = mapRecordToAddress(rs);
				dbResults.add(addressDTO);
			}

			return dbResults;
		});
	}

	@Override
	public AddressDTO findByPK(String pk) {

		return transactionControl.supports(() -> {

			AddressDTO addressDTO = null;

			PreparedStatement pst = connection.prepareStatement(SQL_SELECT_ADDRESS_BY_PK);
			pst.setString(1, pk);

			ResultSet rs = pst.executeQuery();

			if (rs.next()) {
				addressDTO = mapRecordToAddress(rs);
			}

			return addressDTO;
		});
	}

	@Override
	public void save(Long personId, AddressDTO data) {
	    
	    transactionControl.required(() -> {
	        PreparedStatement pst = connection.prepareStatement(SQL_ADD_ADDRESS);
	        pst.setString(1, data.emailAddress);
	        pst.setLong(2, data.personId);
	        pst.setString(3, data.city);
	        pst.setString(4, data.country);
	        logger.info("Saved Person with id {}  and Address : {}", personId, data);
	        pst.executeUpdate();
	        
	        return null;
	    });
	}

	@Override
	public void update(Long personId, AddressDTO data) {
	    
	    transactionControl.required(() -> {
	        PreparedStatement pst = connection.prepareStatement(SQL_UPDATE_ADDRESS_BY_PK_AND_PERSON_ID);
	        pst.setString(1, data.city);
	        pst.setString(2, data.country);
	        pst.setString(3, data.emailAddress);
	        pst.setLong(4, data.personId);
	        logger.info("Updated Person Address : {}", data);
	        pst.executeUpdate();
	        
	        return null;
	    });
	}

	@Override
	public void delete(Long personId) {
	    
		transactionControl.required(() -> {
			PreparedStatement pst = connection.prepareStatement(SQL_DELETE_ALL_ADDRESS_BY_PERSON_ID);
			pst.setLong(1, personId);
			logger.info("Deleted Person {} Addresses", personId);
			pst.executeUpdate();

			return null;
		});
	}

	protected AddressDTO mapRecordToAddress(ResultSet rs) throws SQLException {
		AddressDTO addressDTO = new AddressDTO();
		addressDTO.personId = rs.getLong(AddressTable.PERSON_ID);
		addressDTO.emailAddress = rs.getString(AddressTable.EMAIL_ADDRESS);
		addressDTO.city = rs.getString(AddressTable.CITY);
		addressDTO.country = rs.getString(AddressTable.COUNTRY);
		return addressDTO;
	}
}