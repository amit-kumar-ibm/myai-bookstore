package com.myai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myai.dto.ApiDtos.AddressDto;
import com.myai.dto.ApiDtos.AddressRequest;
import com.myai.dto.DtoMapper;
import com.myai.exception.ApiException;
import com.myai.model.Address;
import com.myai.repository.AddressRepository;
import com.myai.repository.UserRepository;

@Service
@Transactional
public class AddressService {

    private final AddressRepository addresses;
    private final UserRepository users;

    public AddressService(AddressRepository addresses, UserRepository users) {
        this.addresses = addresses;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<AddressDto> list(Long userId) {
        return addresses.findByUserIdOrderByIdAsc(userId).stream().map(DtoMapper::toDto).toList();
    }

    public AddressDto create(Long userId, AddressRequest r) {
        Address a = new Address(users.getReferenceById(userId), r.firstName(), r.lastName(), r.line1(),
                r.line2(), r.city(), r.state(), r.pin(), r.country(), r.phone());
        return DtoMapper.toDto(addresses.save(a));
    }

    public void delete(Long userId, Long addressId) {
        Address a = addresses.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> ApiException.notFound("Address"));
        addresses.delete(a);
    }
}
