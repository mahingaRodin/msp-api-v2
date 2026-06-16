package com.msp.mappers;

import com.msp.models.Store;
import com.msp.models.User;
import com.msp.payloads.dtos.StoreDto;

public class StoreMapper {
    public static StoreDto toDto(Store store) {
        if (store == null) {
            return null;
        }
        StoreDto storeDto = new StoreDto();
        storeDto.setId(store.getId());
        storeDto.setBrand(store.getBrand());
        storeDto.setStoreAdmin(UserMapper.toDTO(store.getStoreAdmin()));
        storeDto.setCreatedAt(store.getCreatedAt());
        storeDto.setUpdatedAt(store.getUpdatedAt());
        storeDto.setDescription(store.getDescription());
        storeDto.setStoreType(store.getStoreType());
        storeDto.setStatus(store.getStatus());
        storeDto.setContact(store.getContact());
        storeDto.setTenantId(store.getTenantId());

        return storeDto;
    }

    public static Store toEntity(StoreDto storeDto, User storeAdmin) {
        Store store = new Store();
        store.setId(storeDto.getId());
        store.setBrand(storeDto.getBrand());
        store.setStoreAdmin(storeAdmin);
        store.setCreatedAt(storeDto.getCreatedAt());
        store.setUpdatedAt(storeDto.getUpdatedAt());
        store.setDescription(storeDto.getDescription());
        store.setStoreType(storeDto.getStoreType());
        store.setStatus(storeDto.getStatus());
        store.setContact(storeDto.getContact());
        return store;
    }
}
