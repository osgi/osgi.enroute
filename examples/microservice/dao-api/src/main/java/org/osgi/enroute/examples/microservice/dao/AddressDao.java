package org.osgi.enroute.examples.microservice.dao;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.enroute.examples.microservice.dao.dto.AddressDTO;

@ProviderType
public interface AddressDao {
    
    public List<AddressDTO> select(Long personId);

    public AddressDTO findByPK(String emailAddress);

    public void save(Long personId,AddressDTO data);

    public void update(Long personId,AddressDTO data);

    public void delete(Long personId) ;

}
