package net.codejava.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import net.codejava.domain.dto.auth.LoginResponseDTO;
import net.codejava.domain.dto.meta.MetaRequestDTO;
import net.codejava.domain.dto.meta.MetaResponseDTO;
import net.codejava.domain.dto.user.AddUserRequestDTO;
import net.codejava.domain.dto.user.UpdUserRequestDTO;
import net.codejava.domain.dto.user.UserBookingCountDTO;
import net.codejava.domain.dto.user.UserDetailResponseDTO;
import net.codejava.responses.MetaResponse;
import net.codejava.responses.Response;

public interface UserService {
    Response<UserDetailResponseDTO> getDetailUser(Integer id);

    Response<LoginResponseDTO> addUser(AddUserRequestDTO requestDTO) throws IOException;

    Response<UserDetailResponseDTO> updateUser(Integer id, UpdUserRequestDTO requestDTO);

    MetaResponse<MetaResponseDTO, List<UserBookingCountDTO>> getListUserBooking(
            MetaRequestDTO requestDTO, Integer ownerId);

    MetaResponse<MetaResponseDTO, List<UserBookingCountDTO>> getListUser(
            MetaRequestDTO requestDTO);

    Response<Map<String, String>> getMyWallet(Integer userId);

    MetaResponse<MetaResponseDTO, List<UserBookingCountDTO>> getListUserWithPickupOrConfirmOrders(
            MetaRequestDTO requestDTO, Integer ownerId);

    Response<String> toggleUserStatus(Integer userId);

    Response<String> deleteUser(Integer userId);
}
