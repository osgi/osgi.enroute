package org.osgi.enroute.examples.microservice.dao.impl;
public interface AddressTable {

	String TABLE_NAME = "ADDRESSES";

	String SQL_SELECT_ADDRESS_BY_PERSON = "SELECT * FROM " + TABLE_NAME + " WHERE PERSON_ID = ? ";

	String SQL_DELETE_ADDRESS = "DELETE FROM " + TABLE_NAME + " WHERE EMAIL_ADDRESS = ? AND  PERSON_ID=?";

	String SQL_DELETE_ALL_ADDRESS_BY_PERSON_ID = "DELETE FROM " + TABLE_NAME + " WHERE PERSON_ID=?";

	String SQL_SELECT_ADDRESS_BY_PK = "SELECT * FROM " + TABLE_NAME + " where EMAIL_ADDRESS=?";

	String SQL_ADD_ADDRESS = "INSERT INTO " + TABLE_NAME + "(EMAIL_ADDRESS,PERSON_ID,CITY,COUNTRY) VALUES(?,?,?,?)";

	String SQL_UPDATE_ADDRESS_BY_PK_AND_PERSON_ID = "UPDATE " + TABLE_NAME + " SET CITY=?, COUNTRY=? "
			+ "WHERE EMAIL_ADDRESS = ? AND  PERSON_ID=?";

	String PERSON_ID = "person_id";

	String EMAIL_ADDRESS = "email_address";

	String CITY = "city";

	String COUNTRY = "country";

	String INIT = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +" (" //
			+ "email_address varchar(255) NOT NULL," //
			+ "person_id  bigint NOT NULL," //
			+ "city varchar(100) NOT NULL," //
			+ "country varchar(2) NOT NULL," //
			+ "PRIMARY KEY (email_address)" + ") ;";
}
