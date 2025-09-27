package net.codejava.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.codejava.constant.Endpoint;
import net.codejava.domain.dto.meta.MetaRequestDTO;
import net.codejava.domain.dto.meta.MetaResponseDTO;
import net.codejava.domain.dto.user.UpdUserRequestDTO;
import net.codejava.domain.dto.user.UserBookingCountDTO;
import net.codejava.domain.dto.user.UserDetailResponseDTO;
import net.codejava.domain.entity.User;
import net.codejava.responses.MetaResponse;
import net.codejava.responses.Response;
import net.codejava.service.UserService;
import net.codejava.utility.AuthUtil;
import net.codejava.utility.JwtTokenUtil;

@Tag(name = "User Controller", description = "APIs related to User operations")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    @Operation(summary = "Get Detail User For User", description = "This API allows users to get detail user.")
    @GetMapping(Endpoint.V1.User.GET_DETAIL)
    public ResponseEntity<Response<UserDetailResponseDTO>> getDetailUser() {
        User user =
                (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.OK).body(userService.getDetailUser(user.getId()));
    }

    @Operation(summary = "Update User For User", description = "This API allows users to update user.")
    @PutMapping(Endpoint.V1.User.UPDATE)
    public ResponseEntity<Response<UserDetailResponseDTO>> updateUser(
            @RequestBody @Valid UpdUserRequestDTO requestDTO) {
        User user =
                (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser(user.getId(), requestDTO));
    }

    //    @Operation(summary = "Update User For User", description = "This API allows users to update user.")
    @GetMapping(Endpoint.V1.User.GET_USER_BOOKING)
    public ResponseEntity<MetaResponse<MetaResponseDTO, List<UserBookingCountDTO>>> getListBookingForUserManager(
            HttpServletRequest servletRequest, @ParameterObject MetaRequestDTO metaRequestDTO) {
        Integer userId =
                Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK).body(userService.getListUserBooking(metaRequestDTO, userId));
    }

    @GetMapping(Endpoint.V1.User.GET_USER_LIST)
    public ResponseEntity<MetaResponse<MetaResponseDTO, List<UserBookingCountDTO>>> getListUserManager(
            HttpServletRequest servletRequest, @ParameterObject MetaRequestDTO metaRequestDTO) {
        Integer userId =
                Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK).body(userService.getListUser(metaRequestDTO));
    }

    @Operation(summary = "Get wallet User For User", description = "This API allows users to get my wallet.")
    @GetMapping(Endpoint.V1.User.GET_MONEY_IN_WALLET)
    public ResponseEntity<Response<Map<String, String>>> getMyWallet() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.getMyWallet(AuthUtil.getRequestedUser().getId()));
    }

    @Operation(
            summary = "Get List of Users with PICK_UP or CONFIRM Orders",
            description = "This API allows the owner to get a list of users with orders in PICK_UP or CONFIRM status.")
    @GetMapping(Endpoint.V1.User.GET_USER_BOOKING_PICKUP_CONFIRM)
    public ResponseEntity<MetaResponse<MetaResponseDTO, List<UserBookingCountDTO>>>
            getListUserWithPickupOrConfirmOrders(
                    HttpServletRequest servletRequest, @ParameterObject MetaRequestDTO metaRequestDTO) {
        Integer userId =
                Integer.valueOf(jwtTokenUtil.getAccountId(servletRequest.getHeader(HttpHeaders.AUTHORIZATION)));
        return ResponseEntity.status(HttpStatus.OK)
                .body(userService.getListUserWithPickupOrConfirmOrders(metaRequestDTO, userId));
    }

    @Operation(
            summary = "Toggle User Status",
            description = "This API allows admin to toggle user status (active/inactive).")
    @PutMapping(Endpoint.V1.User.TOGGLE_STATUS)
    public ResponseEntity<Response<String>> toggleUserStatus(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.toggleUserStatus(id));
    }

    @Operation(summary = "Delete User", description = "This API allows admin to delete a user account.")
    @DeleteMapping(Endpoint.V1.User.DELETE_USER)
    public ResponseEntity<Response<String>> deleteUser(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.deleteUser(id));
    }
}
